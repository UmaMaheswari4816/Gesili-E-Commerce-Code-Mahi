package com.gesili.ecommerce.payment.provider;

import java.math.BigDecimal;

/**
 * Strategy for a payment provider (Razorpay, Stripe, ...). Each provider knows how to create
 * an order/intent, verify a webhook signature over the RAW request body, parse a webhook into
 * a normalized outcome, and issue a refund.
 *
 * <p>Adding a provider = adding one implementation; nothing else changes (Open/Closed). The
 * active provider is chosen by config or the {@code X-Payment-Provider} header via
 * {@code PaymentProviderFactory}.
 */
public interface PaymentProvider {

    /** Provider key, e.g. {@code "razorpay"} / {@code "stripe"}. */
    String name();

    /** Create a provider order/intent; returns the reference + checkout params for the client. */
    CreatedOrder createOrder(java.util.UUID orderId, BigDecimal amount, String currency);

    /**
     * Verify the webhook signature over the EXACT raw request body. Returns true if valid.
     * Parsing-then-reserializing would change the bytes and break this — callers pass the raw
     * bytes straight from the request.
     */
    boolean verifySignature(String rawBody, String signatureHeader);

    /** Parse a verified webhook body into a normalized outcome. */
    WebhookOutcome parseWebhook(String rawBody);

    /** Issue a refund for a captured payment (late-payment compensation). */
    RefundResult refund(String providerRef, BigDecimal amount);

    /** Checkout params handed back to the client (keyId is provider-specific; may be null). */
    record CreatedOrder(String providerRef, String keyId) {}

    /** Normalized webhook result. */
    record WebhookOutcome(String providerEventId, Type type, String orderId, String providerRef) {
        public enum Type { CAPTURED, FAILED, REFUNDED, IGNORED }
    }

    record RefundResult(String refundRef, boolean success) {}
}
