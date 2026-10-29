package com.gecili.ecommerce.common.enums;

/**
 * Lifecycle states of a {@code Payment}, owned by the payment-service bounded context.
 *
 * <p>Happy path: {@link #INITIATED} &rarr; {@link #AUTHORIZED} &rarr; {@link #CAPTURED}.
 *
 * <p>If a webhook confirms payment for an order that has already been cancelled or timed
 * out, the payment moves {@link #CAPTURED} &rarr; {@link #REFUND_PENDING} &rarr;
 * {@link #REFUNDED} (with {@link #REFUND_FAILED} retried by the reconciliation job).
 *
 * <p>Webhook-driven transitions are <strong>forward-only</strong>: a stale or out-of-order
 * webhook can never move a payment backward (e.g. CAPTURED cannot revert to INITIATED).
 * That rule is enforced in the payment-service, not here.
 */
public enum PaymentStatus {

    /** Provider order/intent created; awaiting customer checkout. */
    INITIATED,

    /** Funds authorized by the provider but not yet captured. */
    AUTHORIZED,

    /** Funds captured — payment is complete. */
    CAPTURED,

    /** Provider reported a failed/declined payment. */
    FAILED,

    /** A refund has been requested (e.g. late payment on a cancelled order). */
    REFUND_PENDING,

    /** Refund completed successfully. */
    REFUNDED,

    /** Refund attempt failed; retried by the reconciliation job. */
    REFUND_FAILED
}
