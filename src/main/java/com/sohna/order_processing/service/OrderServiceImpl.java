package com.sohna.order_processing.service;

import com.sohna.order_processing.dto.request.CreateOrderRequest;
import com.sohna.order_processing.dto.response.OrderResponse;
import com.sohna.order_processing.exception.DuplicateOrderException;
import com.sohna.order_processing.exception.InvalidStatusTransitionException;
import com.sohna.order_processing.exception.OrderCancellationException;
import com.sohna.order_processing.exception.OrderNotFoundException;
import com.sohna.order_processing.helper.OrderValidationHelper;
import com.sohna.order_processing.mapper.OrderMapper;
import com.sohna.order_processing.model.Order;
import com.sohna.order_processing.model.OrderStatus;
import com.sohna.order_processing.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implements all order business logic.
 *
 * Each method validates first, then performs the operation,
 * then saves and returns the response.
 *
 * @Transactional ensures any failure mid-operation rolls back
 * all database changes cleanly.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final OrderValidationHelper validationHelper;

    /**
     * Creates a new order.
     *
     * If an idempotency key is provided and already exists,
     * the request is rejected to prevent duplicate orders
     * when a client retries a failed network request.
     *
     * Empty string keys are treated as no key — the DTO getter
     * handles this so the service stays clean.
     *
     * @param request the create order request payload
     * @return the created order response
     * @throws DuplicateOrderException if the same order was already placed
     */
    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating order for customer: {}", request.getCustomerEmail());
//        log.info("Idempotency key received: '{}'", request.getIdempotencyKey());
//        log.info("Idempotency key is null: {}", request.getIdempotencyKey() == null);

        // getIdempotencyKey() returns null if blank — handled in the DTO getter
        if (request.getIdempotencyKey() != null &&
                orderRepository.existsByIdempotencyKey(request.getIdempotencyKey())) {
            throw new DuplicateOrderException();
        }

        Order order = orderMapper.toEntity(request);
        Order savedOrder = orderRepository.save(order);

        log.info("Order created successfully - ID: {}", savedOrder.getId());
        return orderMapper.toResponse(savedOrder);
    }

    /**
     * Retrieves a single order by ID.
     * Soft deleted orders are treated as not found.
     *
     * @param id the order UUID
     * @return the matching order response
     * @throws OrderNotFoundException if the order does not exist
     */
    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID id) {
        log.info("Fetching order - ID: {}", id);

        Order order = orderRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        return orderMapper.toResponse(order);
    }

    /**
     * Retrieves all orders with optional status filter.
     *
     * readOnly = true improves performance by telling JPA not to
     * track entity changes for this query.
     *
     * @param status   optional status filter
     * @param pageable pagination parameters
     * @return a page of order responses
     */
    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(OrderStatus status, Pageable pageable) {
        log.info("Fetching orders - status filter: {}, page: {}",
                status, pageable.getPageNumber());

        if (status != null) {
            return orderRepository.findByStatusAndDeletedFalse(status, pageable)
                    .map(orderMapper::toResponse);
        }

        return orderRepository.findByDeletedFalse(pageable)
                .map(orderMapper::toResponse);
    }

    /**
     * Updates an order status after validating the transition is allowed.
     *
     * @param id        the order UUID
     * @param newStatus the requested new status
     * @return the updated order response
     * @throws OrderNotFoundException           if the order does not exist
     * @throws InvalidStatusTransitionException if the transition is not allowed
     */
    @Override
    @Transactional
    public OrderResponse updateOrderStatus(UUID id, OrderStatus newStatus) {
        Order order = orderRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        // Log after fetching so we have the current status available
        log.info("Updating order status - ID: {}, from: {}, to: {}",
                id, order.getStatus(), newStatus);

        validationHelper.validateStatusTransition(order, newStatus);
        validationHelper.validateStatusNotSame(order.getStatus(), newStatus);


        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);

        log.info("Order status updated - ID: {}, new status: {}", id, newStatus);
        return orderMapper.toResponse(updatedOrder);
    }

    /**
     * Cancels an order by marking it as CANCELLED and soft deleting it.
     *
     * Soft delete preserves the order history for audit purposes.
     * Only orders in PENDING status can be cancelled.
     *
     * @param id the order UUID
     * @return the cancelled order response
     * @throws OrderNotFoundException     if the order does not exist
     * @throws OrderCancellationException if the order is not in PENDING status
     */
    @Override
    @Transactional
    public OrderResponse cancelOrder(UUID id) {
        log.info("Cancelling order - ID: {}", id);

        Order order = orderRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        validationHelper.validateCancellation(order);

        order.setStatus(OrderStatus.CANCELLED);
        // Soft delete preserves the order in the database for audit history
        order.setDeleted(true);
        Order cancelledOrder = orderRepository.save(order);

        log.info("Order cancelled successfully - ID: {}", id);
        return orderMapper.toResponse(cancelledOrder);
    }

    /**
     * Bulk updates all PENDING orders to PROCESSING.
     *
     * Called by the scheduler every 5 minutes. Uses a single bulk
     * UPDATE query instead of fetching each order individually
     * to keep database calls minimal.
     */
    @Override
    @Transactional
    public void processPendingOrders() {
        log.info("Scheduler running - processing PENDING orders");

        int updatedCount = orderRepository.bulkUpdatePendingToProcessing();

        log.info("Scheduler completed - {} orders moved to PROCESSING", updatedCount);
    }
}