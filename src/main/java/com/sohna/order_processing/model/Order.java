package com.sohna.order_processing.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Core aggregate root representing a customer order.
 *
 * UUID primary key is used instead of a numeric ID to prevent customers
 * from guessing other customers' order IDs by incrementing a number.
 *
 * Optimistic locking via @Version prevents lost updates when two requests
 * modify the same order at the same time — the second request gets a 409
 * instead of silently overwriting the first change.
 *
 * Orders are never physically deleted. The deleted flag is set to true on
 * cancellation so the full history is preserved for audit and support purposes.
 */
@Entity
@Table(
        name = "orders",
        indexes = {
                // status is the most frequently filtered column — by list queries and the scheduler bulk update
                @Index(name = "idx_order_status", columnList = "status"),
                @Index(name = "idx_order_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private String customerName;

    // @Email ensures only valid email formats are stored at the database level
    @Email
    @Column(nullable = false)
    private String customerEmail;

    // Prevents duplicate orders if the client retries a failed network request.
    // The service returns the original order instead of creating a new one.
    @Column(unique = true)
    private String idempotencyKey;

    // Stored as STRING so adding new statuses later doesn't corrupt existing rows
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    // @PositiveOrZero ensures a negative total can never be saved to the database
    @PositiveOrZero
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    // EAGER fetch because orders are always displayed with their items.
    // Lazy loading here would cause N+1 queries on every order fetch.
    // CascadeType.ALL and orphanRemoval mean the order fully owns its items.
    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    // Soft delete — cancelled orders stay in the database for audit history.
    // All list queries filter out deleted = true so they are invisible to normal API calls.
    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // JPA increments this on every update. If two requests modify the same order
    // simultaneously, the one working on a stale version gets a 409 Conflict.
    @Version
    private Long version;

    /**
     * Sets timestamps before the order is first saved to the database.
     * Fires automatically by JPA before every INSERT.
     */
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Updates the updatedAt timestamp before every save.
     * Fires automatically by JPA before every UPDATE.
     */
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Associates an item with this order and keeps the bidirectional
     * relationship consistent on both sides.
     *
     * Without calling item.setOrder(this), the foreign key would be null
     * and the save would fail with a constraint violation.
     *
     * @param item the line item to add to this order
     */
    public void addItem(OrderItem item) {
        item.setOrder(this);
        items.add(item);
    }
}