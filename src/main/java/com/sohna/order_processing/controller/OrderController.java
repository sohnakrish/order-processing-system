package com.sohna.order_processing.controller;

import com.sohna.order_processing.dto.request.CreateOrderRequest;
import com.sohna.order_processing.dto.response.ApiResponse;
import com.sohna.order_processing.dto.response.OrderResponse;
import com.sohna.order_processing.dto.response.PagedOrderResponse;
import com.sohna.order_processing.helper.OrderMessageHelper;
import com.sohna.order_processing.model.OrderStatus;
import com.sohna.order_processing.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for all order operations.
 *
 * All endpoints return a consistent ApiResponse wrapper so the
 * client always knows what to expect — success or failure,
 * the response shape is always the same.
 */
@Slf4j//It's for logging
@Validated
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderMessageHelper messageHelper;

    /**
     * Creates a new order for a customer.
     * Returns 201 Created on success.
     *
     * @param request the create order request payload
     * @return the created order wrapped in ApiResponse
     */
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        log.info("POST /api/orders - creating order for: {}", request.getCustomerEmail());

        OrderResponse order = orderService.createOrder(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(order, "Order placed successfully."));
    }

    /**
     * Retrieves a single order by its ID.
     * Returns 404 if the order does not exist.
     *
     * @param id the order UUID from the path
     * @return the matching order wrapped in ApiResponse
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @PathVariable UUID id) {
        log.info("GET /api/orders/{} - fetching order", id);

        OrderResponse order = orderService.getOrderById(id);

        return ResponseEntity.ok(ApiResponse.success(order,
                "Order retrieved successfully."));
    }

    /**
     * Retrieves all orders with optional status filter and pagination.
     *
     * Returns a clean pagination wrapper instead of Spring's verbose
     * Page object so the client only gets what it needs.
     *
     * @param status optional status filter
     * @param page   page number starting from 0
     * @param size   number of records per page — max 100
     * @return a paged order response wrapped in ApiResponse
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PagedOrderResponse>> getAllOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Page number cannot be negative.")
            int page,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "Page size must be at least 1.")
            @Max(value = 100, message = "Page size cannot exceed 100.")
            int size) {
        log.info("GET /api/orders - status filter: {}, page: {}, size: {}",
                status, page, size);

        Pageable pageable = PageRequest.of(page, size,
                Sort.by("createdAt").descending());
        Page<OrderResponse> orders = orderService.getAllOrders(status, pageable);

        String message = messageHelper.resolveListMessage(status, orders.isEmpty());

        // Build clean pagination response
        PagedOrderResponse pagedResponse = PagedOrderResponse.builder()
                .orders(orders.getContent())
                .pagination(PagedOrderResponse.PaginationMeta.builder()
                        .currentPage(orders.getNumber())
                        .pageSize(orders.getSize())
                        .totalOrders(orders.getTotalElements())
                        .totalPages(orders.getTotalPages())
                        .isFirstPage(orders.isFirst())
                        .isLastPage(orders.isLast())
                        .build())
                .build();

        return ResponseEntity.ok(ApiResponse.success(pagedResponse, message));
    }

    /**
     * Updates the status of an existing order.
     * Returns 409 if the transition is not allowed.
     *
     * @param id        the order UUID from the path
     * @param newStatus the requested new status
     * @return the updated order wrapped in ApiResponse
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable UUID id,
            @RequestParam OrderStatus newStatus) {
        log.info("PATCH /api/orders/{}/status - updating to {}", id, newStatus);

        OrderResponse order = orderService.updateOrderStatus(id, newStatus);

        return ResponseEntity.ok(ApiResponse.success(order,
                messageHelper.resolveUpdateMessage(newStatus)));
    }

    /**
     * Cancels an order.
     * Only allowed if the order is in PENDING status.
     * Returns 409 if the order state does not allow cancellation.
     *
     * @param id the order UUID from the path
     * @return the cancelled order wrapped in ApiResponse
     */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable UUID id) {
        log.info("PATCH /api/orders/{}/cancel - cancelling order", id);

        OrderResponse order = orderService.cancelOrder(id);

        return ResponseEntity.ok(ApiResponse.success(order,
                "Your order has been cancelled successfully."));
    }
}