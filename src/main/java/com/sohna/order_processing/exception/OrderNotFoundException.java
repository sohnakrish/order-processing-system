package com.sohna.order_processing.exception;

import java.util.UUID;

/**
 * Thrown when an order cannot be found by the given ID.
 * GlobalExceptionHandler maps this to a 404 response.
 */
public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(UUID id) {
        super("Order not found. Please check your order ID and try again.");
    }
}