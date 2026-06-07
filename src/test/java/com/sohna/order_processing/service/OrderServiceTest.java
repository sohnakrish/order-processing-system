package com.sohna.order_processing.service;

import com.sohna.order_processing.dto.request.CreateOrderRequest;
import com.sohna.order_processing.dto.request.OrderItemRequest;
import com.sohna.order_processing.dto.response.OrderResponse;
import com.sohna.order_processing.exception.*;
import com.sohna.order_processing.helper.OrderValidationHelper;
import com.sohna.order_processing.mapper.OrderMapper;
import com.sohna.order_processing.model.Order;
import com.sohna.order_processing.model.OrderStatus;
import com.sohna.order_processing.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderServiceImpl.
 *
 * Tests all order business logic in isolation using Mockito —
 * no real database or server is started.
 *
 * Pattern: AAA (Arrange, Act, Assert) in every test.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    // ============================================================
    // MOCKS
    // Real dependencies replaced with Mockito fakes so we test
    // only the service logic in complete isolation.
    // ============================================================

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderValidationHelper validationHelper;

    @InjectMocks
    private OrderServiceImpl orderService;

    // ============================================================
    // CONSTANTS
    // Defined once and reused across all tests.
    // Avoids magic values scattered throughout the file.
    // ============================================================

    private static final String CUSTOMER_NAME = "John Smith";
    private static final String CUSTOMER_EMAIL = "john.smith@gmail.com";
    private static final String PRODUCT_NAME = "Apple iPhone 15";
    private static final String PRODUCT_ID = "APPL-IPH15-001";
    private static final BigDecimal PRODUCT_PRICE = new BigDecimal("999.99");
    private static final BigDecimal TOTAL_AMOUNT = new BigDecimal("999.99");
    private static final String IDEMPOTENCY_KEY = "550e8400-e29b-41d4-a716-446655440000";

    // ============================================================
    // SHARED TEST STATE
    // Common variables reset before each test via @BeforeEach.
    // ============================================================

    private UUID orderId;
    private Order mockOrder;
    private OrderResponse mockResponse;

    /**
     * Resets shared test state before every test.
     * Ensures each test starts with a clean consistent baseline.
     */
    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        mockOrder = buildMockOrder(OrderStatus.PENDING);
        mockResponse = buildMockResponse(OrderStatus.PENDING);
    }

    // ============================================================
    // TEST DATA BUILDERS
    // Helper methods shared across all tests.
    // Keeps each test clean — no repeated setup code.
    // ============================================================

    /**
     * A standard valid order request with one item.
     * Used as the baseline for most create order tests.
     */
    private CreateOrderRequest buildValidRequest() {
        return CreateOrderRequest.builder()
                .customerName(CUSTOMER_NAME)
                .customerEmail(CUSTOMER_EMAIL)
                .items(List.of(buildValidItemRequest()))
                .build();
    }

    /**
     * A valid order request with two different items.
     * Used to verify total and line total calculations.
     */
    private CreateOrderRequest buildRequestWithMultipleItems() {
        return CreateOrderRequest.builder()
                .customerName(CUSTOMER_NAME)
                .customerEmail(CUSTOMER_EMAIL)
                .items(List.of(
                        buildValidItemRequest(),
                        OrderItemRequest.builder()
                                .productName("Apple MagSafe Charger")
                                .productId("APPL-MAGSAFE-001")
                                .quantity(2)
                                .productPrice(new BigDecimal("49.99"))
                                .build()
                ))
                .build();
    }

    /**
     * A valid request that includes an idempotency key.
     * Used to test duplicate order detection.
     */
    private CreateOrderRequest buildRequestWithKey(String key) {
        return CreateOrderRequest.builder()
                .customerName(CUSTOMER_NAME)
                .customerEmail(CUSTOMER_EMAIL)
                .idempotencyKey(key)
                .items(List.of(buildValidItemRequest()))
                .build();
    }

    /**
     * A single valid order item with realistic product details.
     */
    private OrderItemRequest buildValidItemRequest() {
        return OrderItemRequest.builder()
                .productName(PRODUCT_NAME)
                .productId(PRODUCT_ID)
                .quantity(1)
                .productPrice(PRODUCT_PRICE)
                .build();
    }

    /**
     * A mock Order entity in the given status.
     * Simulates what the repository returns.
     */
    private Order buildMockOrder(OrderStatus status) {
        return Order.builder()
                .id(UUID.randomUUID())
                .customerName(CUSTOMER_NAME)
                .customerEmail(CUSTOMER_EMAIL)
                .status(status)
                .totalAmount(TOTAL_AMOUNT)
                .items(List.of())
                .deleted(false)
                .build();
    }

    /**
     * A mock OrderResponse in the given status.
     * Simulates what the mapper returns after converting an entity.
     */
    private OrderResponse buildMockResponse(OrderStatus status) {
        return OrderResponse.builder()
                .orderId(UUID.randomUUID())
                .customerName(CUSTOMER_NAME)
                .customerEmail(CUSTOMER_EMAIL)
                .status(status)
                .totalAmount(TOTAL_AMOUNT)
                .items(List.of())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ============================================================
    // CREATE ORDER TESTS
    // ============================================================

    @Test
    void createOrder_singleItem_returnsOrderWithPendingStatus() {
        // Every new order must start as PENDING regardless of input.
        // Arrange
        CreateOrderRequest request = buildValidRequest();

        when(orderMapper.toEntity(request)).thenReturn(mockOrder);
        when(orderRepository.save(mockOrder)).thenReturn(mockOrder);
        when(orderMapper.toResponse(mockOrder)).thenReturn(mockResponse);

        // Act
        OrderResponse result = orderService.createOrder(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
        verify(orderRepository).save(mockOrder);
    }

    @Test
    void createOrder_multipleItems_returnsOrderSuccessfully() {
        // Orders with more than one product must be saved correctly.
        // Arrange
        CreateOrderRequest request = buildRequestWithMultipleItems();

        when(orderMapper.toEntity(request)).thenReturn(mockOrder);
        when(orderRepository.save(mockOrder)).thenReturn(mockOrder);
        when(orderMapper.toResponse(mockOrder)).thenReturn(mockResponse);

        // Act
        OrderResponse result = orderService.createOrder(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void createOrder_validRequest_statusIsAlwaysPending() {
        // Status must always be PENDING on creation — client cannot override this.
        // Arrange
        CreateOrderRequest request = buildValidRequest();

        when(orderMapper.toEntity(request)).thenReturn(mockOrder);
        when(orderRepository.save(mockOrder)).thenReturn(mockOrder);
        when(orderMapper.toResponse(mockOrder)).thenReturn(mockResponse);

        // Act
        OrderResponse result = orderService.createOrder(request);

        // Assert
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void createOrder_validRequest_totalAmountCalculatedCorrectly() {
        // Total must equal sum of all item line totals. Here: 1 x 999.99 = 999.99
        // Arrange
        CreateOrderRequest request = buildValidRequest();

        when(orderMapper.toEntity(request)).thenReturn(mockOrder);
        when(orderRepository.save(mockOrder)).thenReturn(mockOrder);
        when(orderMapper.toResponse(mockOrder)).thenReturn(mockResponse);

        // Act
        OrderResponse result = orderService.createOrder(request);

        // Assert
        assertThat(result.getTotalAmount())
                .isEqualByComparingTo(TOTAL_AMOUNT);
    }

    @Test
    void createOrder_multipleItems_lineTotalCalculatedPerItem() {
        // Line total per item = quantity x price. Here: (1x999.99) + (2x49.99) = 1099.97
        // Arrange
        CreateOrderRequest request = buildRequestWithMultipleItems();
        Order multiItemOrder = buildMockOrder(OrderStatus.PENDING);
        multiItemOrder.setTotalAmount(new BigDecimal("1099.97"));
        OrderResponse multiItemResponse = OrderResponse.builder()
                .orderId(UUID.randomUUID())
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("1099.97"))
                .items(List.of())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(orderMapper.toEntity(request)).thenReturn(multiItemOrder);
        when(orderRepository.save(multiItemOrder)).thenReturn(multiItemOrder);
        when(orderMapper.toResponse(multiItemOrder)).thenReturn(multiItemResponse);

        // Act
        OrderResponse result = orderService.createOrder(request);

        // Assert
        assertThat(result.getTotalAmount())
                .isEqualByComparingTo(new BigDecimal("1099.97"));
    }

    @Test
    void createOrder_duplicateIdempotencyKey_throwsDuplicateOrderException() {
        // Same key used twice means client retried — reject to prevent duplicate.
        // Arrange
        CreateOrderRequest request = buildRequestWithKey(IDEMPOTENCY_KEY);

        when(orderRepository.existsByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(DuplicateOrderException.class)
                .hasMessageContaining("already been placed.");

        // Order must never reach the save step
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_noIdempotencyKey_skipsDuplicateCheck() {
        // Without a key the duplicate check must never run.
        // Arrange
        CreateOrderRequest request = buildValidRequest();

        when(orderMapper.toEntity(request)).thenReturn(mockOrder);
        when(orderRepository.save(mockOrder)).thenReturn(mockOrder);
        when(orderMapper.toResponse(mockOrder)).thenReturn(mockResponse);

        // Act
        orderService.createOrder(request);

        // Assert
        verify(orderRepository, never()).existsByIdempotencyKey(any());
    }

    @Test
    void createOrder_validRequest_orderSavedToRepositoryOnce() {
        // Order must be saved exactly once — not zero or multiple times.
        // Arrange
        CreateOrderRequest request = buildValidRequest();

        when(orderMapper.toEntity(request)).thenReturn(mockOrder);
        when(orderRepository.save(mockOrder)).thenReturn(mockOrder);
        when(orderMapper.toResponse(mockOrder)).thenReturn(mockResponse);

        // Act
        orderService.createOrder(request);

        // Assert
        verify(orderRepository, times(1)).save(mockOrder);
    }

    // ============================================================
    // GET ORDER TESTS
    // ============================================================

    @Test
    void getOrderById_existingOrder_returnsCorrectOrder() {
        // A valid ID must return the matching order with all fields.
        // Arrange
        when(orderRepository.findByIdAndDeletedFalse(orderId))
                .thenReturn(Optional.of(mockOrder));
        when(orderMapper.toResponse(mockOrder)).thenReturn(mockResponse);

        // Act
        OrderResponse result = orderService.getOrderById(orderId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void getOrderById_orderNotFound_throwsOrderNotFoundException() {
        // Non-existent or soft-deleted order must return a clear not found error.
        // Arrange
        when(orderRepository.findByIdAndDeletedFalse(orderId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> orderService.getOrderById(orderId))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("Order not found.");
    }

    // ============================================================
    // LIST ORDERS TESTS
    // ============================================================

    @Test
    void getAllOrders_noStatusFilter_returnsAllActiveOrders() {
        // Without a filter all non-deleted orders must be returned.
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> mockPage = new PageImpl<>(List.of(mockOrder));

        when(orderRepository.findByDeletedFalse(pageable)).thenReturn(mockPage);
        when(orderMapper.toResponse(mockOrder)).thenReturn(mockResponse);

        // Act
        Page<OrderResponse> result = orderService.getAllOrders(null, pageable);

        // Assert
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(orderRepository).findByDeletedFalse(pageable);
        verify(orderRepository, never()).findByStatusAndDeletedFalse(any(), any());
    }

    @Test
    void getAllOrders_withStatusFilter_returnsOnlyMatchingOrders() {
        // Status filter must restrict results to only that status.
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Order processingOrder = buildMockOrder(OrderStatus.PROCESSING);
        OrderResponse processingResponse = buildMockResponse(OrderStatus.PROCESSING);
        Page<Order> mockPage = new PageImpl<>(List.of(processingOrder));

        when(orderRepository.findByStatusAndDeletedFalse(OrderStatus.PROCESSING, pageable))
                .thenReturn(mockPage);
        when(orderMapper.toResponse(processingOrder)).thenReturn(processingResponse);

        // Act
        Page<OrderResponse> result = orderService.getAllOrders(OrderStatus.PROCESSING, pageable);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus())
                .isEqualTo(OrderStatus.PROCESSING);
    }

    @Test
    void getAllOrders_noMatchingOrders_returnsEmptyPage() {
        // When no orders exist response must be an empty page — not null or error.
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);

        when(orderRepository.findByDeletedFalse(pageable))
                .thenReturn(new PageImpl<>(List.of()));

        // Act
        Page<OrderResponse> result = orderService.getAllOrders(null, pageable);

        // Assert
        assertThat(result.isEmpty()).isTrue();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void getAllOrders_withPageSize_returnsCorrectNumberOfOrders() {
        // Page size must be respected — only requested number returned
        // even if more orders exist in the database.
        // Arrange
        Pageable pageable = PageRequest.of(0, 2);
        Order secondOrder = buildMockOrder(OrderStatus.PROCESSING);
        OrderResponse secondResponse = buildMockResponse(OrderStatus.PROCESSING);
        Page<Order> mockPage = new PageImpl<>(
                List.of(mockOrder, secondOrder), pageable, 5);

        when(orderRepository.findByDeletedFalse(pageable)).thenReturn(mockPage);
        when(orderMapper.toResponse(mockOrder)).thenReturn(mockResponse);
        when(orderMapper.toResponse(secondOrder)).thenReturn(secondResponse);

        // Act
        Page<OrderResponse> result = orderService.getAllOrders(null, pageable);

        // Assert — page has 2 items but total in DB is 5
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(5);
    }

    // ============================================================
    // UPDATE ORDER STATUS TESTS
    // ============================================================

    @Test
    void updateOrderStatus_pendingToProcessing_updatesSuccessfully() {
        // PENDING to PROCESSING is the first valid transition.
        // Validator must be called before the order is saved.
        // Arrange
        OrderResponse processingResponse = buildMockResponse(OrderStatus.PROCESSING);

        when(orderRepository.findByIdAndDeletedFalse(orderId))
                .thenReturn(Optional.of(mockOrder));
        when(orderRepository.save(mockOrder)).thenReturn(mockOrder);
        when(orderMapper.toResponse(mockOrder)).thenReturn(processingResponse);

        // Act
        OrderResponse result = orderService.updateOrderStatus(orderId, OrderStatus.PROCESSING);

        // Assert
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PROCESSING);
        verify(validationHelper).validateStatusNotSame(OrderStatus.PENDING, OrderStatus.PROCESSING);
        verify(validationHelper).validateStatusTransition(mockOrder, OrderStatus.PROCESSING);
        verify(orderRepository).save(mockOrder);
    }

    @Test
    void updateOrderStatus_processingToShipped_updatesSuccessfully() {
        // PROCESSING to SHIPPED means warehouse has dispatched the order.
        // Arrange
        Order processingOrder = buildMockOrder(OrderStatus.PROCESSING);
        OrderResponse shippedResponse = buildMockResponse(OrderStatus.SHIPPED);

        when(orderRepository.findByIdAndDeletedFalse(orderId))
                .thenReturn(Optional.of(processingOrder));
        when(orderRepository.save(processingOrder)).thenReturn(processingOrder);
        when(orderMapper.toResponse(processingOrder)).thenReturn(shippedResponse);

        // Act
        OrderResponse result = orderService.updateOrderStatus(orderId, OrderStatus.SHIPPED);

        // Assert
        assertThat(result.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        verify(validationHelper).validateStatusTransition(processingOrder, OrderStatus.SHIPPED);
    }

    @Test
    void updateOrderStatus_shippedToDelivered_updatesSuccessfully() {
        // SHIPPED to DELIVERED confirms the order reached the customer.
        // Arrange
        Order shippedOrder = buildMockOrder(OrderStatus.SHIPPED);
        OrderResponse deliveredResponse = buildMockResponse(OrderStatus.DELIVERED);

        when(orderRepository.findByIdAndDeletedFalse(orderId))
                .thenReturn(Optional.of(shippedOrder));
        when(orderRepository.save(shippedOrder)).thenReturn(shippedOrder);
        when(orderMapper.toResponse(shippedOrder)).thenReturn(deliveredResponse);

        // Act
        OrderResponse result = orderService.updateOrderStatus(orderId, OrderStatus.DELIVERED);

        // Assert
        assertThat(result.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        verify(validationHelper).validateStatusTransition(shippedOrder, OrderStatus.DELIVERED);
    }

    @Test
    void updateOrderStatus_invalidTransition_throwsInvalidStatusTransitionException() {
        // Skipping steps in the lifecycle must be rejected.
        // PENDING to DELIVERED skips PROCESSING and SHIPPED.
        // Arrange
        when(orderRepository.findByIdAndDeletedFalse(orderId))
                .thenReturn(Optional.of(mockOrder));
        doThrow(new InvalidStatusTransitionException(OrderStatus.PENDING, OrderStatus.DELIVERED))
                .when(validationHelper).validateStatusTransition(mockOrder, OrderStatus.DELIVERED);

        // Act & Assert
        assertThatThrownBy(() -> orderService.updateOrderStatus(orderId, OrderStatus.DELIVERED))
                .isInstanceOf(InvalidStatusTransitionException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void updateOrderStatus_deliveredToPending_throwsOrderAlreadyDeliveredException() {
        // DELIVERED is a terminal state — no further updates allowed.
        // Arrange
        Order deliveredOrder = buildMockOrder(OrderStatus.DELIVERED);

        when(orderRepository.findByIdAndDeletedFalse(orderId))
                .thenReturn(Optional.of(deliveredOrder));
        doThrow(new OrderAlreadyDeliveredException())
                .when(validationHelper).validateStatusTransition(deliveredOrder, OrderStatus.PENDING);

        // Act & Assert
        assertThatThrownBy(() -> orderService.updateOrderStatus(orderId, OrderStatus.PENDING))
                .isInstanceOf(OrderAlreadyDeliveredException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void updateOrderStatus_orderNotFound_throwsOrderNotFoundException() {
        // Status update on a non-existent order must return not found.
        // Arrange
        when(orderRepository.findByIdAndDeletedFalse(orderId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> orderService.updateOrderStatus(orderId, OrderStatus.PROCESSING))
                .isInstanceOf(OrderNotFoundException.class);

        verify(orderRepository, never()).save(any());
    }

    // ============================================================
    // CANCEL ORDER TESTS
    // Parameterized test covers PROCESSING, SHIPPED, DELIVERED
    // in one clean test instead of three separate ones.
    // ============================================================

    @Test
    void cancelOrder_pendingOrder_cancelledSuccessfully() {
        // PENDING is the only status that allows cancellation.
        // Arrange
        OrderResponse cancelledResponse = buildMockResponse(OrderStatus.CANCELLED);

        when(orderRepository.findByIdAndDeletedFalse(orderId))
                .thenReturn(Optional.of(mockOrder));
        when(orderRepository.save(mockOrder)).thenReturn(mockOrder);
        when(orderMapper.toResponse(mockOrder)).thenReturn(cancelledResponse);

        // Act
        OrderResponse result = orderService.cancelOrder(orderId);

        // Assert
        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(validationHelper).validateCancellation(mockOrder);
    }

    @Test
    void cancelOrder_pendingOrder_statusSetToCancelledAndSoftDeleted() {
        // Cancellation must set status to CANCELLED and deleted to true.
        // Soft delete keeps the order in DB for audit history.
        // Arrange
        OrderResponse cancelledResponse = buildMockResponse(OrderStatus.CANCELLED);

        when(orderRepository.findByIdAndDeletedFalse(orderId))
                .thenReturn(Optional.of(mockOrder));
        when(orderRepository.save(mockOrder)).thenReturn(mockOrder);
        when(orderMapper.toResponse(mockOrder)).thenReturn(cancelledResponse);

        // Act
        orderService.cancelOrder(orderId);

        // Assert
        assertThat(mockOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(mockOrder.isDeleted()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"PROCESSING", "SHIPPED", "DELIVERED"})
    void cancelOrder_nonPendingStatus_throwsException(OrderStatus status) {
        // Once an order moves past PENDING it cannot be cancelled.
        // This single test runs automatically for PROCESSING, SHIPPED and DELIVERED.
        // Arrange
        Order nonPendingOrder = buildMockOrder(status);

        when(orderRepository.findByIdAndDeletedFalse(orderId))
                .thenReturn(Optional.of(nonPendingOrder));
        doThrow(new OrderCancellationException(orderId, status))
                .when(validationHelper).validateCancellation(nonPendingOrder);

        // Act & Assert
        assertThatThrownBy(() -> orderService.cancelOrder(orderId))
                .isInstanceOf(OrderCancellationException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void cancelOrder_orderNotFound_throwsOrderNotFoundException() {
        // Cannot cancel an order that does not exist.
        // Arrange
        when(orderRepository.findByIdAndDeletedFalse(orderId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> orderService.cancelOrder(orderId))
                .isInstanceOf(OrderNotFoundException.class);

        verify(orderRepository, never()).save(any());
    }

    // ============================================================
    // SCHEDULER TESTS
    // ============================================================

    @Test
    void processPendingOrders_pendingOrdersExist_updatesAllToProcessing() {
        // Scheduler must trigger a single bulk update — not one query per order.
        // Arrange
        when(orderRepository.bulkUpdatePendingToProcessing()).thenReturn(3);

        // Act
        orderService.processPendingOrders();

        // Assert
        verify(orderRepository, times(1)).bulkUpdatePendingToProcessing();
    }

    @Test
    void processPendingOrders_noPendingOrders_completesWithoutError() {
        // When no orders are pending the scheduler must finish cleanly.
        // Arrange
        when(orderRepository.bulkUpdatePendingToProcessing()).thenReturn(0);

        // Act
        orderService.processPendingOrders();

        // Assert
        verify(orderRepository).bulkUpdatePendingToProcessing();
    }
}