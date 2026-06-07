package com.sohna.order_processing.helper;

import com.sohna.order_processing.model.OrderStatus;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Resolves user friendly response messages for order operations.
 * Keeps message logic out of the controller layer.
 */
@Component
public class OrderMessageHelper {

    private static final Map<OrderStatus, String> EMPTY_STATUS_MESSAGES = Map.of(
            OrderStatus.PENDING,    "No new orders have been placed yet.",
            OrderStatus.PROCESSING, "No orders are currently being processed.",
            OrderStatus.SHIPPED,    "No orders are currently out for delivery.",
            OrderStatus.DELIVERED,  "No orders have been delivered yet.",
            OrderStatus.CANCELLED,  "No orders have been cancelled."
    );

    private static final Map<OrderStatus, String> FOUND_STATUS_MESSAGES = Map.of(
            OrderStatus.PENDING,    "Pending orders retrieved successfully.",
            OrderStatus.PROCESSING, "Processing orders retrieved successfully.",
            OrderStatus.SHIPPED,    "Shipped orders retrieved successfully.",
            OrderStatus.DELIVERED,  "Delivered orders retrieved successfully.",
            OrderStatus.CANCELLED,  "Cancelled orders retrieved successfully."
    );

    private static final Map<OrderStatus, String> UPDATE_MESSAGES = Map.of(
            OrderStatus.PROCESSING, "Your order is now being processed.",
            OrderStatus.SHIPPED,    "Your order has been shipped and is on its way.",
            OrderStatus.DELIVERED,  "Your order has been delivered successfully.",
            OrderStatus.CANCELLED,  "Your order has been cancelled successfully."
    );

    /**
     * Resolves the response message for list order operations.
     *
     * @param status the status filter applied
     * @param empty  whether the result set is empty
     * @return a user friendly message
     */
    public String resolveListMessage(OrderStatus status, boolean empty) {
        if (empty) {
            return status != null
                    ? EMPTY_STATUS_MESSAGES.getOrDefault(status, "No orders found.")
                    : "No orders found.";
        }

        return status != null
                ? FOUND_STATUS_MESSAGES.getOrDefault(status, "Orders retrieved successfully.")
                : "Orders retrieved successfully.";
    }

    /**
     * Resolves the response message after a status update.
     *
     * @param newStatus the new status the order was updated to
     * @return a user friendly message
     */
    public String resolveUpdateMessage(OrderStatus newStatus) {
        return UPDATE_MESSAGES.getOrDefault(newStatus, "Order status updated successfully.");
    }
}