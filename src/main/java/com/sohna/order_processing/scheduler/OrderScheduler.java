package com.sohna.order_processing.scheduler;

import com.sohna.order_processing.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Background job that automatically moves PENDING orders to PROCESSING.
 *
 * Runs every 5 minutes so the warehouse team always has an up to date
 * view of which orders need to be picked and packed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderScheduler {

    private final OrderService orderService;

    /**
     * Triggers the bulk PENDING to PROCESSING update every 5 minutes.
     *
     * fixedDelay means the next run starts 5 minutes after the previous
     * run completes — not 5 minutes after it starts. This prevents
     * overlapping runs if the job takes longer than expected.
     *
     * If the job fails, the exception is caught and logged so the
     * scheduler continues running on the next cycle without crashing.
     */
    @Scheduled(fixedDelay = 300000) // 300000ms = 5 minutes
    public void processPendingOrders() {
        log.info("Scheduler triggered - checking for PENDING orders");

        try {
            orderService.processPendingOrders();
        } catch (Exception ex) {
            // Log the error but do not rethrow — a failed run should
            // never stop the scheduler from running on the next cycle
            log.error("Scheduler failed - will retry in 5 minutes. Reason: {}", ex.getMessage());
        }
    }
}