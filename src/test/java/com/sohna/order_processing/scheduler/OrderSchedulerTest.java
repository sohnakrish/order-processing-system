package com.sohna.order_processing.scheduler;

import com.sohna.order_processing.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderScheduler.
 *
 * Verifies that the scheduler correctly delegates to the service
 * and handles failures gracefully without crashing.
 *
 * The actual scheduling timing is not tested here — that is a
 * Spring framework concern. We only test the behavior of the
 * method the scheduler triggers.
 *
 * Pattern: AAA (Arrange, Act, Assert) in every test.
 */
@ExtendWith(MockitoExtension.class)
class OrderSchedulerTest {

    // ============================================================
    // MOCKS
    // Service is mocked — we test only the scheduler behavior,
    // not the business logic inside the service.
    // ============================================================

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderScheduler orderScheduler;

    // ============================================================
    // SCHEDULER BEHAVIOR TESTS
    // ============================================================

    @Test
    void processPendingOrders_whenTriggered_delegatesToService() {
        // Scheduler must call the service when it fires.
        // The actual business logic lives in the service — not here.
        // Arrange
        doNothing().when(orderService).processPendingOrders();

        // Act
        orderScheduler.processPendingOrders();

        // Assert
        verify(orderService, times(1)).processPendingOrders();
    }

    @Test
    void processPendingOrders_calledOnce_serviceCalledExactlyOnce() {
        // Each scheduler trigger must call the service exactly once —
        // not zero times and not multiple times.
        // Arrange
        doNothing().when(orderService).processPendingOrders();

        // Act
        orderScheduler.processPendingOrders();

        // Assert
        verify(orderService, times(1)).processPendingOrders();
        verifyNoMoreInteractions(orderService);
    }

    @Test
    void processPendingOrders_serviceThrowsException_doesNotCrash() {
        // If the service throws an exception the scheduler must catch it
        // and continue running — a failed cycle must never stop the scheduler.
        // Arrange
        doThrow(new RuntimeException("Database connection lost"))
                .when(orderService).processPendingOrders();

        // Act & Assert — no exception must propagate out of the scheduler
        assertThatNoException()
                .isThrownBy(() -> orderScheduler.processPendingOrders());
    }

    @Test
    void processPendingOrders_serviceThrowsException_exceptionNeverPropagates() {
        // Even with a critical error the scheduler must swallow the exception
        // so the next scheduled run can still happen 5 minutes later.
        // Arrange
        doThrow(new RuntimeException("Unexpected error"))
                .when(orderService).processPendingOrders();

        // Act
        orderScheduler.processPendingOrders();

        // Assert — service was still called even though it failed
        verify(orderService, times(1)).processPendingOrders();
    }
}