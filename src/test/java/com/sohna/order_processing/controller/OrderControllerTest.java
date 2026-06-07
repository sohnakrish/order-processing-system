package com.sohna.order_processing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sohna.order_processing.dto.request.CreateOrderRequest;
import com.sohna.order_processing.dto.request.OrderItemRequest;
import com.sohna.order_processing.dto.response.OrderResponse;
import com.sohna.order_processing.exception.InvalidStatusTransitionException;
import com.sohna.order_processing.exception.OrderCancellationException;
import com.sohna.order_processing.exception.OrderNotFoundException;
import com.sohna.order_processing.helper.OrderMessageHelper;
import com.sohna.order_processing.model.OrderStatus;
import com.sohna.order_processing.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller tests for OrderController.

 * Tests the HTTP layer only — service is mocked so we verify
 * that endpoints return correct status codes, response shapes,
 * and that validations reject bad input before reaching the service.

 * Uses WebMvcTest which loads only the web layer — faster than
 * loading the full Spring context.

 * Pattern: AAA (Arrange, Act, Assert) in every test.
 */
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    // ============================================================
    // DEPENDENCIES
    // MockMvc simulates HTTP calls without starting a real server.
    // Service and helper are mocked — only HTTP layer is tested.
    // ============================================================

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private OrderMessageHelper messageHelper;

    // ============================================================
    // CONSTANTS
    // Defined once and reused across all tests.
    // ============================================================

    private static final String BASE_URL = "/api/orders";
    private static final String CUSTOMER_NAME = "John Smith";
    private static final String CUSTOMER_EMAIL = "john.smith@gmail.com";
    private static final String PRODUCT_ID = "APPL-IPH15-001";
    private static final String PRODUCT_NAME = "Apple iPhone 15";
    private static final BigDecimal PRODUCT_PRICE = new BigDecimal("999.99");
    private static final BigDecimal TOTAL_AMOUNT = new BigDecimal("999.99");

    // ============================================================
    // SHARED TEST STATE
    // ============================================================

    private UUID orderId;
    private OrderResponse mockResponse;

    /**
     * Resets shared state and sets up default mock behavior
     * before every test.
     */
    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        mockResponse = buildMockResponse(OrderStatus.PENDING);

        // Default message helper responses — avoids repeated stubbing
        when(messageHelper.resolveListMessage(any(), anyBoolean()))
                .thenReturn("Orders retrieved successfully.");
        when(messageHelper.resolveUpdateMessage(any()))
                .thenReturn("Order status updated successfully.");
    }

    // ============================================================
    // TEST DATA BUILDERS
    // Shared helpers that build consistent test data.
    // ============================================================

    /**
     * Builds a fully valid create order request.
     * Used as the baseline for all POST tests.
     */
    private CreateOrderRequest buildValidRequest() {
        return CreateOrderRequest.builder()
                .customerName(CUSTOMER_NAME)
                .customerEmail(CUSTOMER_EMAIL)
                .items(List.of(buildValidItem()))
                .build();
    }

    /**
     * Builds a valid order item.
     */
    private OrderItemRequest buildValidItem() {
        return OrderItemRequest.builder()
                .productId(PRODUCT_ID)
                .productName(PRODUCT_NAME)
                .quantity(1)
                .productPrice(PRODUCT_PRICE)
                .build();
    }

    /**
     * Builds a mock OrderResponse in the given status.
     * Simulates what the service returns after processing.
     */
    private OrderResponse buildMockResponse(OrderStatus status) {
        return OrderResponse.builder()
                .orderId(orderId)
                .customerName(CUSTOMER_NAME)
                .customerEmail(CUSTOMER_EMAIL)
                .status(status)
                .totalAmount(TOTAL_AMOUNT)
                .items(List.of())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Converts an object to JSON string for request bodies.
     */
    private String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    // ============================================================
    // POST — CREATE ORDER TESTS
    // ============================================================

    @Test
    void createOrder_validRequest_returns201Created() throws Exception {
        // A valid order request must return 201 Created with order details.
        // Arrange
        CreateOrderRequest request = buildValidRequest();

        when(orderService.createOrder(any())).thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Order placed successfully."))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void createOrder_blankCustomerName_returns400WithValidationError() throws Exception {
        // Blank customer name must be caught by @Valid before reaching service.
        // Arrange
        CreateOrderRequest request = buildValidRequest();
        request.setCustomerName(null);

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.customerName")
                        .value("Customer name is required."));

        // Service must never be called when validation fails
        verify(orderService, never()).createOrder(any());
    }

    @Test
    void createOrder_invalidEmail_returns400WithValidationError() throws Exception {
        // Invalid email format must be rejected before reaching the service.
        // Arrange
        CreateOrderRequest request = buildValidRequest();
        request.setCustomerEmail("ab@gmail.com");

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.customerEmail")
                        .value("Please enter a valid email address with at least 3 characters before @."));

        verify(orderService, never()).createOrder(any());
    }

    @Test
    void createOrder_emptyItemsList_returns400WithValidationError() throws Exception {
        // An order with no items must be rejected before reaching the service.
        // Arrange
        CreateOrderRequest request = buildValidRequest();
        request.setItems(List.of());

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.items")
                        .value("Order must contain at least one item."));

        verify(orderService, never()).createOrder(any());
    }

    @Test
    void createOrder_invalidIdempotencyKeyFormat_returns400() throws Exception {
        // Idempotency key that is not a valid UUID must be rejected.
        // Arrange
        CreateOrderRequest request = buildValidRequest();
        request.setIdempotencyKey("invalid-key-123");

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.idempotencyKey")
                        .value("Idempotency key must be a valid UUID format (e.g. 550e8400-e29b-41d4-a716-446655440000)"));

        verify(orderService, never()).createOrder(any());
    }

    // ============================================================
    // GET — ORDER BY ID TESTS
    // ============================================================

    @Test
    void getOrderById_existingOrder_returns200WithOrderDetails() throws Exception {
        // A valid order ID must return 200 with the full order details.
        // Arrange
        when(orderService.getOrderById(orderId)).thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void getOrderById_orderNotFound_returns404() throws Exception {
        // A non-existent order ID must return 404 with a clear error message.
        // Arrange
        when(orderService.getOrderById(orderId))
                .thenThrow(new OrderNotFoundException(orderId));

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/" + orderId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("Order not found. Please check your order ID and try again."));
    }

    @Test
    void getOrderById_invalidUUID_returns400() throws Exception {
        // A malformed UUID in the path must return 400 — not a server error.
        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/invalid-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ============================================================
    // GET — LIST ORDERS TESTS
    // ============================================================

    @Test
    void getAllOrders_withStatusFilter_returns200WithFilteredOrders() throws Exception {
        // Status filter must be passed to the service and results returned.
        // Arrange
        when(orderService.getAllOrders(eq(OrderStatus.PENDING), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mockResponse)));

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "?status=PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orders[0].status").value("PENDING"));
    }

    // ============================================================
    // PATCH — UPDATE ORDER STATUS TESTS
    // ============================================================

    @Test
    void updateOrderStatus_validTransition_returns200WithUpdatedOrder() throws Exception {
        // A valid status transition must return 200 with the updated order.
        // Arrange
        OrderResponse shippedResponse = buildMockResponse(OrderStatus.SHIPPED);

        when(orderService.updateOrderStatus(orderId, OrderStatus.SHIPPED))
                .thenReturn(shippedResponse);

        // Act & Assert
        mockMvc.perform(patch(BASE_URL + "/" + orderId + "/status")
                        .param("newStatus", "SHIPPED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("SHIPPED"));
    }

    @Test
    void updateOrderStatus_invalidTransition_returns409Conflict() throws Exception {
        // An invalid status transition must return 409 with a clear error.
        // Arrange
        when(orderService.updateOrderStatus(orderId, OrderStatus.PENDING))
                .thenThrow(new InvalidStatusTransitionException(
                        OrderStatus.DELIVERED, OrderStatus.PENDING));

        // Act & Assert
        mockMvc.perform(patch(BASE_URL + "/" + orderId + "/status")
                        .param("newStatus", "PENDING"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ============================================================
    // PATCH — CANCEL ORDER TESTS
    // ============================================================

    @Test
    void cancelOrder_pendingOrder_returns200WithCancelledOrder() throws Exception {
        // Cancelling a PENDING order must return 200 with CANCELLED status.
        // Arrange
        OrderResponse cancelledResponse = buildMockResponse(OrderStatus.CANCELLED);

        when(orderService.cancelOrder(orderId)).thenReturn(cancelledResponse);

        // Act & Assert
        mockMvc.perform(patch(BASE_URL + "/" + orderId + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    void cancelOrder_nonPendingOrder_returns409Conflict() throws Exception {
        // Cancelling a non-PENDING order must return 409 Conflict.
        // Arrange
        when(orderService.cancelOrder(orderId))
                .thenThrow(new OrderCancellationException(orderId, OrderStatus.PROCESSING));

        // Act & Assert
        mockMvc.perform(patch(BASE_URL + "/" + orderId + "/cancel"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }
}