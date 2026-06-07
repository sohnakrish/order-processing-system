package com.sohna.order_processing.mapper;

import com.sohna.order_processing.dto.request.CreateOrderRequest;
import com.sohna.order_processing.dto.request.OrderItemRequest;
import com.sohna.order_processing.dto.response.OrderItemResponse;
import com.sohna.order_processing.dto.response.OrderResponse;
import com.sohna.order_processing.model.Order;
import com.sohna.order_processing.model.OrderItem;
import com.sohna.order_processing.model.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for OrderMapper.
 *
 * Verifies that all conversions between request DTOs, entities
 * and response DTOs are handled correctly.
 *
 * No mocks needed — mapper has no external dependencies.
 * Tests run against the real mapper instance directly.
 *
 * Pattern: AAA (Arrange, Act, Assert) in every test.
 */
class OrderMapperTest {

    // ============================================================
    // CONSTANTS
    // Defined once and reused across all tests.
    // ============================================================

    private static final String CUSTOMER_NAME = "John Smith";
    private static final String CUSTOMER_EMAIL = "john.smith@gmail.com";
    private static final String PRODUCT_ID = "APPL-IPH15-001";
    private static final String PRODUCT_NAME = "Apple iPhone 15";
    private static final BigDecimal PRODUCT_PRICE = new BigDecimal("999.99");
    private static final String IDEMPOTENCY_KEY = "550e8400-e29b-41d4-a716-446655440000";

    // ============================================================
    // SHARED TEST STATE
    // Mapper instance created once and reused — no Spring context needed.
    // ============================================================

    private OrderMapper orderMapper;

    @BeforeEach
    void setUp() {
        // Real mapper instance — no mocks needed here
        orderMapper = new OrderMapper();
    }

    // ============================================================
    // TEST DATA BUILDERS
    // Shared helpers that build consistent test data.
    // Keeps each test clean — no repeated setup code.
    // ============================================================

    /**
     * Builds a valid create order request with one item.
     * Used as the baseline for toEntity tests.
     */
    private CreateOrderRequest buildValidRequest() {
        return CreateOrderRequest.builder()
                .customerName(CUSTOMER_NAME)
                .customerEmail(CUSTOMER_EMAIL)
                .items(List.of(buildValidItemRequest()))
                .build();
    }

    /**
     * Builds a valid request with an idempotency key.
     * Used to verify the key is correctly mapped to the entity.
     */
    private CreateOrderRequest buildRequestWithKey() {
        return CreateOrderRequest.builder()
                .customerName(CUSTOMER_NAME)
                .customerEmail(CUSTOMER_EMAIL)
                .idempotencyKey(IDEMPOTENCY_KEY)
                .items(List.of(buildValidItemRequest()))
                .build();
    }

    /**
     * Builds a valid request with two items.
     * Used to verify total amount calculation across multiple lines.
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
     * Builds a single valid order item request.
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
     * Builds a saved Order entity in the given status.
     * Simulates an entity returned from the repository after save.
     */
    private Order buildSavedOrder(OrderStatus status) {
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .customerName(CUSTOMER_NAME)
                .customerEmail(CUSTOMER_EMAIL)
                .status(status)
                .totalAmount(PRODUCT_PRICE)
                .deleted(false)
                .build();

        // Build item and link to order using addItem()
        // so the bidirectional relationship is correct
        OrderItem item = OrderItem.builder()
                .id(UUID.randomUUID())
                .productName(PRODUCT_NAME)
                .productId(PRODUCT_ID)
                .quantity(1)
                .productPrice(PRODUCT_PRICE)
                .build();

        order.addItem(item);

        // Fire @PrePersist to populate createdAt and updatedAt
        order.prePersist();

        return order;
    }

    // ============================================================
    // toEntity TESTS
    // Verifies request → Order entity mapping is correct.
    // ============================================================

    @Test
    void toEntity_validRequest_mapsCustomerFieldsCorrectly() {
        // Customer name and email must be copied exactly from request to entity.
        // Arrange
        CreateOrderRequest request = buildValidRequest();

        // Act
        Order result = orderMapper.toEntity(request);

        // Assert
        assertThat(result.getCustomerName()).isEqualTo(CUSTOMER_NAME);
        assertThat(result.getCustomerEmail()).isEqualTo(CUSTOMER_EMAIL);
    }

    @Test
    void toEntity_requestWithIdempotencyKey_keyMappedToEntity() {
        // Idempotency key must be carried from the request to the entity
        // so the repository can check for duplicates on save.
        // Arrange
        CreateOrderRequest request = buildRequestWithKey();

        // Act
        Order result = orderMapper.toEntity(request);

        // Assert
        assertThat(result.getIdempotencyKey()).isEqualTo(IDEMPOTENCY_KEY);
    }

    @Test
    void toEntity_multipleItems_calculatesTotalAmountCorrectly() {
        // Total must equal sum of all line totals.
        // Here: (1 x 999.99) + (2 x 49.99) = 1099.97
        // Arrange
        CreateOrderRequest request = buildRequestWithMultipleItems();

        // Act
        Order result = orderMapper.toEntity(request);

        // Assert
        assertThat(result.getTotalAmount())
                .isEqualByComparingTo(new BigDecimal("1099.97"));
    }

    @Test
    void toEntity_items_bidirectionalRelationshipSetCorrectly() {
        // addItem() must set the order reference on each item.
        // Without this the foreign key would be null on save.
        // Arrange
        CreateOrderRequest request = buildValidRequest();

        // Act
        Order result = orderMapper.toEntity(request);

        // Assert
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getOrder()).isEqualTo(result);
    }

    // ============================================================
    // toResponse TESTS
    // Verifies Order entity → OrderResponse DTO mapping is correct.
    // ============================================================

    @Test
    void toResponse_savedOrder_mapsOrderIdCorrectly() {
        // The orderId in the response must match the entity UUID.
        // This is the primary identifier the client uses for further calls.
        // Arrange
        Order order = buildSavedOrder(OrderStatus.PENDING);

        // Act
        OrderResponse result = orderMapper.toResponse(order);

        // Assert
        assertThat(result.getOrderId()).isEqualTo(order.getId());
    }

    @Test
    void toResponse_savedOrder_mapsAllCustomerAndStatusFields() {
        // All customer fields and status must be present in the response.
        // Arrange
        Order order = buildSavedOrder(OrderStatus.PROCESSING);

        // Act
        OrderResponse result = orderMapper.toResponse(order);

        // Assert
        assertThat(result.getCustomerName()).isEqualTo(CUSTOMER_NAME);
        assertThat(result.getCustomerEmail()).isEqualTo(CUSTOMER_EMAIL);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PROCESSING);
        assertThat(result.getTotalAmount()).isEqualByComparingTo(PRODUCT_PRICE);
    }

    @Test
    void toResponse_savedOrder_timestampsPopulated() {
        // createdAt and updatedAt must be present after @PrePersist fires.
        // Arrange
        Order order = buildSavedOrder(OrderStatus.PENDING);

        // Act
        OrderResponse result = orderMapper.toResponse(order);

        // Assert
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();
    }

    @Test
    void toResponse_savedOrder_excludesInternalFields() {
        // version and deleted are internal fields — must never appear
        // in the response to avoid leaking implementation details.
        // Arrange
        Order order = buildSavedOrder(OrderStatus.PENDING);

        // Act
        OrderResponse result = orderMapper.toResponse(order);

        // Assert — verify these internal fields do not exist on response
        assertThat(result.getClass().getDeclaredFields())
                .noneMatch(f -> f.getName().equals("version"))
                .noneMatch(f -> f.getName().equals("deleted"));
    }

    @Test
    void toResponse_savedOrder_itemsMappedWithItemId() {
        // Items must be mapped using itemId — not the raw entity id field.
        // Arrange
        Order order = buildSavedOrder(OrderStatus.PENDING);

        // Act
        OrderResponse result = orderMapper.toResponse(order);

        // Assert
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getItemId())
                .isEqualTo(order.getItems().get(0).getId());
    }

    @Test
    void toResponse_savedOrder_lineTotalCalculatedCorrectly() {
        // lineTotal must equal quantity x productPrice.
        // Here: 1 x 999.99 = 999.99
        // Arrange
        Order order = buildSavedOrder(OrderStatus.PENDING);

        // Act
        OrderResponse result = orderMapper.toResponse(order);

        // Assert
        OrderItemResponse item = result.getItems().get(0);
        assertThat(item.getLineTotal())
                .isEqualByComparingTo(
                        item.getProductPrice()
                                .multiply(BigDecimal.valueOf(item.getQuantity())));
    }

    // ============================================================
    // toResponseList TESTS
    // Verifies list mapping works correctly for multiple orders.
    // ============================================================

    @Test
    void toResponseList_multipleOrders_mapsAllCorrectly() {
        // All orders in the list must be mapped — none dropped or duplicated.
        // Arrange
        List<Order> orders = List.of(
                buildSavedOrder(OrderStatus.PENDING),
                buildSavedOrder(OrderStatus.PROCESSING),
                buildSavedOrder(OrderStatus.SHIPPED)
        );

        // Act
        List<OrderResponse> result = orderMapper.toResponseList(orders);

        // Assert
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(result.get(1).getStatus()).isEqualTo(OrderStatus.PROCESSING);
        assertThat(result.get(2).getStatus()).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    void toResponseList_emptyList_returnsEmptyListWithoutError() {
        // An empty input list must return an empty list — not null or an error.
        // Arrange
        List<Order> orders = List.of();

        // Act
        List<OrderResponse> result = orderMapper.toResponseList(orders);

        // Assert
        assertThat(result).isEmpty();
    }
}