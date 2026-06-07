package com.sohna.order_processing.exception;

import com.sohna.order_processing.model.OrderStatus;

/**
 * Thrown when a status update violates the allowed transition rules.
 * Allowed transitions: PENDING → PROCESSING → SHIPPED → DELIVERED.
 * GlobalExceptionHandler maps this to a 409 response.
 */
public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(OrderStatus from, OrderStatus to) {
        super("This order cannot be updated from " + from + " to " + to + ".");
    }

    // Used when the order is already in a terminal state
    public InvalidStatusTransitionException(OrderStatus terminalStatus) {
        super("This order is already " + terminalStatus + " and cannot be updated.");
    }
}