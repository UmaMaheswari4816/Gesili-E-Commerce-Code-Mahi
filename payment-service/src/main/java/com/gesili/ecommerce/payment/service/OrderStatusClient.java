package com.gesili.ecommerce.payment.service;

import java.util.UUID;

/**
 * Tells whether an order is already cancelled/timed out — used to detect the late-payment
 * race. Implemented against order-service.
 *
 * <p>In this phase the implementation is event-driven: payment-service tracks terminal order
 * states it observes on {@code order.events} (OrderCancelled), avoiding a synchronous call on
 * the webhook hot path. A synchronous OpenFeign lookup to {@code GET /orders/{id}} is the
 * documented alternative.
 */
public interface OrderStatusClient {
    boolean isCancelledOrTimedOut(UUID orderId);
}
