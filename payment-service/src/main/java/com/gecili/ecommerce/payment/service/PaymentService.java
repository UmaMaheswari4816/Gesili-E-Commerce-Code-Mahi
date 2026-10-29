package com.gecili.ecommerce.payment.service;

import com.gecili.ecommerce.common.enums.PaymentStatus;
import com.gecili.ecommerce.common.event.EventEnvelope;
import com.gecili.ecommerce.common.event.EventTypes;
import com.gecili.ecommerce.common.event.payload.PaymentCompletedEvent;
import com.gecili.ecommerce.common.event.payload.PaymentFailedEvent;
import com.gecili.ecommerce.common.event.payload.PaymentRefundedEvent;
import com.gecili.ecommerce.payment.entity.Payment;
import com.gecili.ecommerce.payment.entity.WebhookEvent;
import com.gecili.ecommerce.payment.messaging.PaymentEventPublisher;
import com.gecili.ecommerce.payment.provider.PaymentProvider;
import com.gecili.ecommerce.payment.provider.PaymentProvider.CreatedOrder;
import com.gecili.ecommerce.payment.provider.PaymentProvider.WebhookOutcome;
import com.gecili.ecommerce.payment.provider.PaymentProviderFactory;
import com.gecili.ecommerce.payment.repository.PaymentRepository;
import com.gecili.ecommerce.payment.repository.WebhookEventRepository;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Payment orchestration: create provider orders on reservation, process verified webhooks
 * forward-only and idempotently, and refund late payments on cancelled/timed-out orders.
 */
@Slf4j
@Service
public class PaymentService {

    private final PaymentRepository payments;
    private final WebhookEventRepository webhookEvents;
    private final PaymentProviderFactory providers;
    private final PaymentEventPublisher publisher;
    private final OrderStatusClient orderStatus;

    public PaymentService(
            PaymentRepository payments,
            WebhookEventRepository webhookEvents,
            PaymentProviderFactory providers,
            PaymentEventPublisher publisher,
            OrderStatusClient orderStatus) {
        this.payments = payments;
        this.webhookEvents = webhookEvents;
        this.providers = providers;
        this.publisher = publisher;
        this.orderStatus = orderStatus;
    }

    /** On InventoryReserved: create the provider order and record an INITIATED payment. */
    @Transactional
    public CreatedOrder initiate(UUID orderId, BigDecimal amount, String currency, String providerName) {
        if (payments.findByOrderId(orderId).isPresent()) {
            Payment existing = payments.findByOrderId(orderId).get();
            PaymentProvider p = providers.byName(existing.getProvider());
            return new CreatedOrder(existing.getProviderRef(), null); // idempotent
        }
        PaymentProvider provider = providers.resolve(providerName);
        CreatedOrder created = provider.createOrder(orderId, amount, currency);
        Payment payment = Payment.initiate(orderId, provider.name(), amount, currency);
        payment.setProviderRef(created.providerRef());
        payments.save(payment);
        log.info("Initiated {} payment for order {} ({})", provider.name(), orderId, created.providerRef());
        return created;
    }

    /**
     * Process a verified webhook. Idempotent (dedupe on provider event id) and forward-only
     * (never moves a payment backward). A capture for an already-cancelled/timed-out order is
     * auto-refunded.
     */
    @Transactional
    public void handleWebhook(PaymentProvider provider, WebhookOutcome outcome) {
        // Idempotency: a replayed provider event is a no-op.
        if (webhookEvents.existsById(outcome.providerEventId())) {
            log.debug("Duplicate webhook {} ignored", outcome.providerEventId());
            return;
        }
        webhookEvents.save(WebhookEvent.of(outcome.providerEventId(), provider.name()));

        if (outcome.type() == WebhookOutcome.Type.IGNORED || outcome.orderId() == null) {
            return;
        }
        UUID orderId = UUID.fromString(outcome.orderId());
        Payment payment = payments.findByOrderId(orderId).orElse(null);
        if (payment == null) {
            log.warn("Webhook for unknown order {}", orderId);
            return;
        }

        switch (outcome.type()) {
            case CAPTURED -> onCaptured(provider, payment, orderId);
            case FAILED -> onFailed(payment, orderId);
            case REFUNDED -> forward(payment, PaymentStatus.REFUNDED);
            default -> { /* ignored */ }
        }
    }

    private void onCaptured(PaymentProvider provider, Payment payment, UUID orderId) {
        // Late-payment race: order already cancelled/timed out -> refund instead of confirming.
        if (orderStatus.isCancelledOrTimedOut(orderId)) {
            log.info("Late payment for cancelled order {} -> refunding", orderId);
            forward(payment, PaymentStatus.REFUND_PENDING);
            var result = provider.refund(payment.getProviderRef(), payment.getAmount());
            forward(payment, result.success() ? PaymentStatus.REFUNDED : PaymentStatus.REFUND_FAILED);
            if (result.success()) {
                var refunded =
                        new PaymentRefundedEvent(
                                orderId, payment.getId(), result.refundRef(), payment.getAmount(),
                                "LATE_PAYMENT_ON_CANCELLED");
                publisher.publish(
                        EventTypes.TOPIC_PAYMENT_EVENTS,
                        orderId,
                        EventEnvelope.of(EventTypes.PAYMENT_REFUNDED, 1, orderId.toString(), "payment-service", refunded));
            }
            return;
        }
        forward(payment, PaymentStatus.CAPTURED);
        var completed =
                new PaymentCompletedEvent(
                        orderId, payment.getId(), payment.getProviderRef(), payment.getAmount(), payment.getCurrency());
        publisher.publish(
                EventTypes.TOPIC_PAYMENT_EVENTS,
                orderId,
                EventEnvelope.of(EventTypes.PAYMENT_COMPLETED, 1, orderId.toString(), "payment-service", completed));
    }

    private void onFailed(Payment payment, UUID orderId) {
        forward(payment, PaymentStatus.FAILED);
        var failed = new PaymentFailedEvent(orderId, payment.getId(), "DECLINED");
        publisher.publish(
                EventTypes.TOPIC_PAYMENT_EVENTS,
                orderId,
                EventEnvelope.of(EventTypes.PAYMENT_FAILED, 1, orderId.toString(), "payment-service", failed));
    }

    /**
     * Forward-only transition: apply {@code next} only if it is "ahead of" the current status.
     * A stale/out-of-order webhook that would move the payment backward is ignored.
     */
    private void forward(Payment payment, PaymentStatus next) {
        if (next.ordinal() <= payment.getStatus().ordinal() && payment.getStatus() != PaymentStatus.INITIATED) {
            log.debug("Ignoring backward transition {} -> {} for order {}",
                    payment.getStatus(), next, payment.getOrderId());
            return;
        }
        payment.setStatus(next);
        payment.touch();
        payments.save(payment);
    }

    @Transactional(readOnly = true)
    public Payment getByOrderId(UUID orderId) {
        return payments.findByOrderId(orderId).orElse(null);
    }
}
