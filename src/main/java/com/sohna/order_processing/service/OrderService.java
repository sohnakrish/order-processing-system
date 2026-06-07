package com.sohna.order_processing.service;

import com.sohna.order_processing.dto.request.CreateOrderRequest;
import com.sohna.order_processing.dto.response.OrderResponse;
import com.sohna.order_processing.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Defines the contract for all order operations.
 *
 * Using an interface allows the service to be mocked in tests
 * without changing the controller.
 */
public interface OrderService {

    /**
     * Places a new order for a customer.
     *
     * @param request the create order request payload
     * @return the created order response
     */
    OrderResponse createOrder(CreateOrderRequest request);

    /**
     * Retrieves a single order by its ID.
     *
     * @param id the order UUID
     * @return the matching order response
     */
    OrderResponse getOrderById(UUID id);

    /**
     * Retrieves all orders with optional status filter and pagination.
     *
     * @param status   optional status filter — returns all orders if null
     * @param pageable pagination and sorting parameters
     * @return a page of order responses
     */
    Page<OrderResponse> getAllOrders(OrderStatus status, Pageable pageable);

    /**
     * Updates the status of an existing order.
     *
     * @param id        the order UUID
     * @param newStatus the requested new status
     * @return the updated order response
     */
    OrderResponse updateOrderStatus(UUID id, OrderStatus newStatus);

    /**
     * Cancels an order. Only allowed if the order is in PENDING status.
     *
     * @param id the order UUID
     * @return the cancelled order response
     */
    OrderResponse cancelOrder(UUID id);

    /**
     * Bulk updates all PENDING orders to PROCESSING.
     * Called by the scheduler every 5 minutes automatically.
     */
    void processPendingOrders();
}