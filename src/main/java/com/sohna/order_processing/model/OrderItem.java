package com.sohna.order_processing.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Represents a single product line within a customer order.
 *
 * OrderItem has no independent lifecycle — it is always created and
 * deleted as part of its parent Order. This is why it has no repository
 * of its own and is never queried in isolation.
 */
@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    // LAZY fetch because we never need the full Order object when working with an item
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false)
    private String productName;

    // Storing the external catalog ID here so the order stays accurate
    // even if the product is renamed or removed from the catalog later
    @Column(nullable = false)
    private String productId;

    @Column(nullable = false)
    private Integer quantity;

    // Price is captured at order time and never updated — if the product price
    // changes after the order is placed, the customer's original price is honored
    // precision = 9 matches @Digits(integer=7) validation — max price 9999999.99
    @Column(nullable = false, precision = 9, scale = 2)
    private BigDecimal productPrice;

    /**
     * Calculates the total cost for this line item.
     *
     * Marked @Transient so JPA does not map it to a column. Storing a derived
     * value in the database risks it going stale if quantity or productPrice changes.
     *
     * @return quantity multiplied by productPrice
     */
    @Transient
    public BigDecimal getLineTotal() {
        return productPrice.multiply(BigDecimal.valueOf(quantity));
    }
}