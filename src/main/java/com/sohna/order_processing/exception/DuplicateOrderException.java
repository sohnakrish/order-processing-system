package com.sohna.order_processing.exception;

/**
 * Thrown when the same order is submitted more than once.
 * GlobalExceptionHandler maps this to a 409 response.
 */
public class DuplicateOrderException extends RuntimeException {

    // Same request submitted twice with matching idempotency key
    public DuplicateOrderException() {
        super("This order has already been placed. Please check your orders.");
    }

    // Duplicate detected based on a specific field
    public DuplicateOrderException(String reason) {
        super("Duplicate order detected. " + reason);
    }
}