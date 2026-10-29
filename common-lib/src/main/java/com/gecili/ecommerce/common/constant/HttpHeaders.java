package com.gecili.ecommerce.common.constant;

/**
 * Custom HTTP header names shared across services and the gateway.
 *
 * <p>Centralized so the gateway (which sets them) and services (which read them) never
 * disagree on spelling.
 */
public final class HttpHeaders {

    private HttpHeaders() {}

    /** Correlation id propagated gateway &rarr; services &rarr; traces/logs. */
    public static final String CORRELATION_ID = "X-Correlation-Id";

    /** Client-supplied idempotency key for non-idempotent operations (e.g. create order). */
    public static final String IDEMPOTENCY_KEY = "Idempotency-Key";

    /** Selects the payment provider strategy (e.g. {@code razorpay}, {@code stripe}). */
    public static final String PAYMENT_PROVIDER = "X-Payment-Provider";

    /** Authenticated user id injected by the gateway after JWT validation. */
    public static final String USER_ID = "X-User-Id";
}
