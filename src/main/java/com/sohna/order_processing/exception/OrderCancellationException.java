package com.sohna.order_processing.exception;

import com.sohna.order_processing.model.OrderStatus;

import java.util.UUID;

/**
 * Thrown when cancellation is attempted on an order
 * that is no longer in PENDING status.
 * GlobalExceptionHandler maps this to a 400 response.
 */
public class OrderCancellationException extends RuntimeException {

    // Order is in a status that does not allow cancellation
    public OrderCancellationException(UUID id, OrderStatus currentStatus) {
        super("This order cannot be cancelled as it is already " + currentStatus + ".");
    }

    // Order has already been cancelled before
    public OrderCancellationException(UUID id) {
        super("This order has already been cancelled.");
    }
}