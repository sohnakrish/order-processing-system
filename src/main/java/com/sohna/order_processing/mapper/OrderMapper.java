package com.sohna.order_processing.mapper;

import com.sohna.order_processing.dto.request.CreateOrderRequest;
import com.sohna.order_processing.dto.request.OrderItemRequest;
import com.sohna.order_processing.dto.response.OrderItemResponse;
import com.sohna.order_processing.dto.response.OrderResponse;
import com.sohna.order_processing.model.Order;
import com.sohna.order_processing.model.OrderItem;
import com.sohna.order_processing.model.OrderStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Handles all conversions between Order entities and DTOs.
 *
 * Keeping mapping logic here means the service layer stays focused
 * on business rules and the controller never touches raw entities.
 */
@Component
public class OrderMapper {

    /**
     * Converts a CreateOrderRequest into a new Order entity ready to be saved.
     *
     * Status is always set to PENDING on creation — the scheduler and
     * manual status updates handle all transitions after that.
     * totalAmount is calculated here so the service does not have to do it manually.
     *
     * @param request the incoming create order request
     * @return a new Order entity with PENDING status and calculated total
     */
    public Order toEntity(CreateOrderRequest request) {
        Order order = Order.builder()
                .customerName(request.getCustomerName())
                .customerEmail(request.getCustomerEmail())
                .idempotencyKey(request.getIdempotencyKey())
                .status(OrderStatus.PENDING)
                .totalAmount(calculateTotal(request.getItems()))
                .build();

        // addItem() keeps the bidirectional relationship consistent —
        // adding directly to the list would leave the foreign key null
        request.getItems().forEach(itemRequest ->
                order.addItem(toItemEntity(itemRequest)));

        return order;
    }

    /**
     * Converts an Order entity into an OrderResponse DTO.
     *
     * Internal fields like version and deleted are intentionally excluded
     * so implementation details are never exposed to the client.
     *
     * @param order the saved Order entity
     * @return a clean OrderResponse safe to return to the client
     */
    public OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .orderId(order.getId())
                .customerName(order.getCustomerName())
                .customerEmail(order.getCustomerEmail())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .items(toItemResponseList(order.getItems()))
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    /**
     * Converts a list of Order entities to OrderResponse DTOs.
     *
     * @param orders list of Order entities from the repository
     * @return list of OrderResponse DTOs
     */
    public List<OrderResponse> toResponseList(List<Order> orders) {
        return orders.stream()
                .map(this::toResponse)
                .toList();
    }

    private OrderItem toItemEntity(OrderItemRequest request) {
        return OrderItem.builder()
                .productName(request.getProductName())
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .productPrice(request.getProductPrice())
                .build();
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .itemId(item.getId())
                .productName(item.getProductName())
                .productId(item.getProductId())
                .quantity(item.getQuantity())
                .productPrice(item.getProductPrice())
                // lineTotal is computed from the entity so the client does not calculate it
                .lineTotal(item.getLineTotal())
                .build();
    }

    private List<OrderItemResponse> toItemResponseList(List<OrderItem> items) {
        return items.stream()
                .map(this::toItemResponse)
                .toList();
    }

    /**
     * Calculates total order amount by summing all line item totals.
     *
     * BigDecimal.ZERO as the identity value prevents NullPointerException
     * if the items list is somehow empty at this point.
     *
     * @param items list of item requests
     * @return sum of quantity x productPrice across all items
     */
    private BigDecimal calculateTotal(List<OrderItemRequest> items) {
        return items.stream()
                .map(item -> item.getProductPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}