package com.sohna.order_processing.exception;

/**
 * Thrown when any modification is attempted on a delivered order.
 * Delivered is a terminal state — no further changes are allowed.
 * GlobalExceptionHandler maps this to a 409 response.
 */
public class OrderAlreadyDeliveredException extends RuntimeException {

    public OrderAlreadyDeliveredException() {
        super("This order has already been delivered and cannot be modified.");
    }
}