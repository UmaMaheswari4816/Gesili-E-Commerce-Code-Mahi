package com.gesili.ecommerce.common.event.payload;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Payload of {@code PaymentRefunded} (topic {@code payment.events}, key = orderId).
 *
 * <p>Emitted when a payment is refunded — most notably the "late payment" race, where a
 * webhook confirms payment for an order that has already been cancelled or timed out, so the
 * captured funds are returned. Consumed by notification-service (inform the customer); the
 * order remains {@code CANCELLED}.
 *
 * @param orderId     the order the refund relates to
 * @param paymentId   the payment aggregate id
 * @param providerRef provider refund reference for reconciliation
 * @param amount      refunded amount
 * @param reason      why the refund happened (e.g. {@code LATE_PAYMENT_ON_CANCELLED})
 */
public record PaymentRefundedEvent(
        UUID orderId, UUID paymentId, String providerRef, BigDecimal amount, String reason) {}
