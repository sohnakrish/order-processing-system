package com.sohna.order_processing.exception;

/**
 * Thrown when an update is attempted on an order that has already been cancelled.
 * A cancelled order is a terminal state — no further changes are allowed.
 * GlobalExceptionHandler maps this to a 409 response.
 */
public class OrderAlreadyCancelledException extends RuntimeException {

    public OrderAlreadyCancelledException() {
        super("This order has already been cancelled and cannot be modified.");
    }
}