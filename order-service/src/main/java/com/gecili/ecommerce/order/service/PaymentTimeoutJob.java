package com.gecili.ecommerce.order.service;

import com.gecili.ecommerce.common.enums.OrderStatus;
import com.gecili.ecommerce.order.entity.OrderEntity;
import com.gecili.ecommerce.order.repository.OrderRepository;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cancels orders stuck in {@code PAYMENT_PENDING} past their payment deadline, emitting
 * {@code OrderCancelled(PAYMENT_TIMEOUT)} (via {@link OrderService#cancel}) which drives the
 * inventory compensation. ShedLock ensures a single sweeper across instances.
 */
@Slf4j
@Component
public class PaymentTimeoutJob {

    private final OrderRepository orders;
    private final OrderService orderService;

    public PaymentTimeoutJob(OrderRepository orders, OrderService orderService) {
        this.orders = orders;
        this.orderService = orderService;
    }

    @Scheduled(fixedDelayString = "${gecili.order.payment-timeout-scan-ms:60000}")
    @SchedulerLock(name = "payment-timeout", lockAtLeastFor = "PT10S", lockAtMostFor = "PT2M")
    public void cancelOverdue() {
        List<OrderEntity> overdue =
                orders.findByStatusAndPaymentDeadlineBefore(OrderStatus.PAYMENT_PENDING, Instant.now());
        if (overdue.isEmpty()) {
            return;
        }
        log.info("Cancelling {} overdue (payment timeout) order(s)", overdue.size());
        for (OrderEntity o : overdue) {
            try {
                orderService.cancel(o.getId(), "PAYMENT_TIMEOUT");
            } catch (Exception e) {
                log.error("Failed to time out order {}: {}", o.getId(), e.getMessage());
            }
        }
    }
}
