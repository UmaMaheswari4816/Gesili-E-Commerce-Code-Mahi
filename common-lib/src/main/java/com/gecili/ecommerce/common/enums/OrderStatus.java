package com.gecili.ecommerce.common.enums;

/**
 * Lifecycle states of an {@code Order}, owned by the order-service bounded context.
 *
 * <p>The happy path is:
 * {@link #CREATED} &rarr; {@link #PENDING_INVENTORY} &rarr; {@link #PAYMENT_PENDING}
 * &rarr; {@link #CONFIRMED}.
 *
 * <p>Failure/compensation paths lead to {@link #CANCELLED} (optionally via
 * {@link #PAYMENT_TIMEOUT}). A late payment on an already-cancelled order triggers a
 * refund in the payment context but leaves the order {@link #CANCELLED}.
 *
 * <p>This enum lives in common-lib because it is part of the shared contract (events and
 * DTOs reference it). The <em>transition rules</em>, however, belong to the order-service
 * and are NOT encoded here — common-lib must stay free of business logic.
 */
public enum OrderStatus {

    /** Order persisted, saga not yet started. */
    CREATED,

    /** OrderCreated emitted; waiting for inventory to reserve stock. */
    PENDING_INVENTORY,

    /** Stock reserved; waiting for the customer to complete payment before the deadline. */
    PAYMENT_PENDING,

    /** Payment captured; order is confirmed and ready for fulfilment. */
    CONFIRMED,

    /** Payment deadline elapsed while in {@link #PAYMENT_PENDING}; drives cancellation. */
    PAYMENT_TIMEOUT,

    /** Terminal failure/compensation state (out of stock, payment failed, or timeout). */
    CANCELLED
}
