package com.gecili.ecommerce.payment.messaging;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Small in-memory cache of order totals learned from {@code OrderCreated}, used when
 * {@code InventoryReserved} (which does not carry the amount) arrives.
 *
 * <p>Simplification for learning: in production payment-service would persist this or read
 * the amount from the order via a lookup. Kept in-memory here; the reconciliation job and the
 * order total on OrderCreated bound any gaps across a restart.
 */
public final class OrderAmounts {

    private record Amount(BigDecimal value, String currency) {}

    private static final ConcurrentHashMap<UUID, Amount> CACHE = new ConcurrentHashMap<>();

    private OrderAmounts() {}

    public static void remember(UUID orderId, BigDecimal amount, String currency) {
        CACHE.put(orderId, new Amount(amount, currency));
    }

    public static BigDecimal amount(UUID orderId) {
        Amount a = CACHE.get(orderId);
        return a != null ? a.value() : BigDecimal.ZERO;
    }

    public static String currency(UUID orderId) {
        Amount a = CACHE.get(orderId);
        return a != null ? a.currency() : "INR";
    }
}
