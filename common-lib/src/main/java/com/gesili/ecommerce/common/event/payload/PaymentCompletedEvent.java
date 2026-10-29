package com.gesili.ecommerce.common.event.payload;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Payload of {@code PaymentCompleted} (topic {@code payment.events}, key = orderId).
 *
 * <p>Emitted by payment-service after a verified, idempotent provider webhook confirms
 * capture. Consumed by order-service (confirm), inventory-service (commit reservation) and
 * notification-service.
 *
 * @param orderId     the paid order
 * @param paymentId   the payment aggregate id
 * @param providerRef provider reference (Razorpay/Stripe) for reconciliation
 * @param amount      captured amount
 * @param currency    ISO-4217 currency code
 */
public record PaymentCompletedEvent(
        UUID orderId, UUID paymentId, String providerRef, BigDecimal amount, String currency) {}
