package com.sohna.order_processing.model;

/**
 * Defines the lifecycle states an order can move through.
 *
 * Allowed transitions:
 *   PENDING → PROCESSING → SHIPPED → DELIVERED
 *   PENDING → CANCELLED
 *
 * Transition rules are enforced in OrderValidationHelper.
 * The PENDING → PROCESSING transition happens automatically via the scheduler every 5 minutes.
 */
public enum OrderStatus {

    // Initial state when a customer places an order. Only status that allows cancellation.
    PENDING,

    // Warehouse has accepted the order. Automatically set by the scheduler every 5 minutes.
    PROCESSING,

    // Order dispatched to courier. No further customer changes allowed.
    SHIPPED,

    // Successfully delivered to the customer. Terminal state.
    DELIVERED,

    // Customer cancelled the order. Only reachable from PENDING. Terminal state.
    CANCELLED
}