package com.sohna.order_processing.exception;

/**
 * Thrown when the request body is null, malformed, or contains
 * invalid data not caught by the DTO validation layer.
 * GlobalExceptionHandler maps this to a 400 response.
 */
public class InvalidOrderRequestException extends RuntimeException {

    public InvalidOrderRequestException(String reason) {
        super("Unable to process your request. " + reason);
    }
}