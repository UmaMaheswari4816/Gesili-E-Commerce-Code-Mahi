package com.gesili.ecommerce.payment.service;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Tracks orders observed as cancelled/timed-out via {@code OrderCancelled} events, so the
 * webhook path can detect a late payment without a synchronous call.
 *
 * <p>Simplification for learning: an in-memory set. In production this would be a persisted
 * projection (survives restart) or a synchronous lookup. The reconciliation job is the
 * backstop for anything missed.
 */
@Component
public class EventDrivenOrderStatusClient implements OrderStatusClient {

    private final Set<UUID> cancelledOrders = ConcurrentHashMap.newKeySet();

    public void markCancelled(UUID orderId) {
        cancelledOrders.add(orderId);
    }

    @Override
    public boolean isCancelledOrTimedOut(UUID orderId) {
        return cancelledOrders.contains(orderId);
    }
}
