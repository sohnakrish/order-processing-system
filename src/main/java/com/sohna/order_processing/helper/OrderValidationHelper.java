package com.sohna.order_processing.helper;

import com.sohna.order_processing.exception.*;
import com.sohna.order_processing.model.Order;
import com.sohna.order_processing.model.OrderStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Central place for order business rule validations.
 *
 * Only rules that go beyond DTO validation live here —
 * things the service layer needs to enforce at runtime.
 */
@Component
public class OrderValidationHelper {

    // Defines which status transitions are allowed.
    // Any transition not listed here is rejected with a 409 response.
    private static final Map<OrderStatus, List<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
            OrderStatus.PENDING,    List.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED),
            OrderStatus.PROCESSING, List.of(OrderStatus.SHIPPED),
            OrderStatus.SHIPPED,    List.of(OrderStatus.DELIVERED),
            OrderStatus.DELIVERED,  List.of(),
            OrderStatus.CANCELLED,  List.of()
    );

    // Status specific messages for same status update attempts.
    // Using a Map keeps this consistent with ALLOWED_TRANSITIONS
    // and avoids verbose switch/if-else blocks.
    private static final Map<OrderStatus, String> SAME_STATUS_MESSAGES = Map.of(
            OrderStatus.PENDING,    "This order is already waiting to be processed.",
            OrderStatus.PROCESSING, "This order is already being processed by our warehouse.",
            OrderStatus.SHIPPED,    "This order is already shipped and on its way to you.",
            OrderStatus.DELIVERED,  "This order has already been delivered.",
            OrderStatus.CANCELLED,  "This order has already been cancelled."
    );

    /**
     * Validates that the requested status transition is allowed.
     *
     * DELIVERED and CANCELLED are terminal states — no further
     * updates are allowed once an order reaches either of them.
     *
     * @param order     the current order
     * @param newStatus the requested new status
     * @throws OrderAlreadyDeliveredException   if the order is already delivered
     * @throws OrderAlreadyCancelledException   if the order is already cancelled
     * @throws InvalidStatusTransitionException if the transition is not allowed
     */
    public void validateStatusTransition(Order order, OrderStatus newStatus) {
        OrderStatus currentStatus = order.getStatus();

        if (currentStatus == OrderStatus.DELIVERED) {
            throw new OrderAlreadyDeliveredException();
        }

        if (currentStatus == OrderStatus.CANCELLED) {
            throw new OrderAlreadyCancelledException();
        }

        List<OrderStatus> allowedNext = ALLOWED_TRANSITIONS.getOrDefault(
                currentStatus, List.of());

        if (!allowedNext.contains(newStatus)) {
            throw new InvalidStatusTransitionException(currentStatus, newStatus);
        }
    }

    /**
     * Validates that an order can be cancelled.
     *
     * Only PENDING orders can be cancelled — once the warehouse
     * starts processing, cancellation is no longer allowed.
     *
     * @param order the order to cancel
     * @throws OrderAlreadyCancelledException if the order is already cancelled
     * @throws OrderCancellationException     if the order is not in PENDING status
     */
    public void validateCancellation(Order order) {
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new OrderAlreadyCancelledException();
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new OrderCancellationException(order.getId(), order.getStatus());
        }
    }

    /**
     * Validates that an order exists and has not been soft deleted.
     *
     * @param order the order returned from the repository
     * @param id    the requested order ID
     * @throws OrderNotFoundException if the order is null or soft deleted
     */
    public void validateOrderExists(Order order, UUID id) {
        if (order == null || order.isDeleted()) {
            throw new OrderNotFoundException(id);
        }
    }

    /**
     * Validates that the new status is different from the current status.
     *
     * Returns a status specific message so the customer knows
     * exactly what state their order is currently in.
     *
     * @param currentStatus the current order status
     * @param newStatus     the requested new status
     * @throws InvalidOrderRequestException if both statuses are the same
     */
    public void validateStatusNotSame(OrderStatus currentStatus, OrderStatus newStatus) {
        if (currentStatus == newStatus) {
            String message = SAME_STATUS_MESSAGES.getOrDefault(
                    currentStatus, "Order status is already up to date.");
            throw new InvalidOrderRequestException(message);
        }
    }

    /**
     * Checks if the given status is a terminal state.
     *
     * Terminal states are DELIVERED and CANCELLED — once an order
     * reaches either of these, it cannot be modified in any way.
     *
     * @param status the status to check
     * @return true if the status is terminal
     */
    public boolean isTerminalState(OrderStatus status) {
        return status == OrderStatus.DELIVERED || status == OrderStatus.CANCELLED;
    }
}