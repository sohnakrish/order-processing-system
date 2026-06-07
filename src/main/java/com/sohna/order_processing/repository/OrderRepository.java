package com.sohna.order_processing.repository;

import com.sohna.order_processing.model.Order;
import com.sohna.order_processing.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Data access layer for Order entities.
 * Soft-deleted orders are filtered out by default in all queries.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByIdAndDeletedFalse(UUID id);

    Page<Order> findByStatusAndDeletedFalse(OrderStatus status, Pageable pageable);

    Page<Order> findByDeletedFalse(Pageable pageable);

    boolean existsByIdempotencyKey(String idempotencyKey);

    /**
     * Bulk updates all PENDING orders to PROCESSING in a single query.
     * Called by the scheduler every 5 minutes.
     *
     * @return number of orders updated
     */
    @Modifying
    @Query("UPDATE Order o SET o.status = 'PROCESSING' WHERE o.status = 'PENDING' AND o.deleted = false")
    int bulkUpdatePendingToProcessing();

    List<Order> findByStatus(OrderStatus status);
}