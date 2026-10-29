package com.gecili.ecommerce.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gecili.ecommerce.common.enums.PaymentStatus;
import com.gecili.ecommerce.payment.entity.Payment;
import com.gecili.ecommerce.payment.messaging.PaymentEventPublisher;
import com.gecili.ecommerce.payment.provider.PaymentProvider;
import com.gecili.ecommerce.payment.provider.PaymentProvider.RefundResult;
import com.gecili.ecommerce.payment.provider.PaymentProvider.WebhookOutcome;
import com.gecili.ecommerce.payment.provider.PaymentProviderFactory;
import com.gecili.ecommerce.payment.repository.PaymentRepository;
import com.gecili.ecommerce.payment.repository.WebhookEventRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for webhook idempotency, capture->PaymentCompleted, and late-payment refund. */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentRepository payments;
    @Mock private WebhookEventRepository webhookEvents;
    @Mock private PaymentProviderFactory providers;
    @Mock private PaymentEventPublisher publisher;
    @Mock private OrderStatusClient orderStatus;
    @Mock private PaymentProvider provider;

    private PaymentService service;

    @BeforeEach
    void setUp() {
        service = new PaymentService(payments, webhookEvents, providers, publisher, orderStatus);
    }

    private Payment captured(UUID orderId) {
        Payment p = Payment.initiate(orderId, "razorpay", new BigDecimal("499.00"), "INR");
        p.setProviderRef("order_abc");
        return p;
    }

    @Test
    void duplicateWebhook_isIgnored() {
        var outcome = new WebhookOutcome("evt_1", WebhookOutcome.Type.CAPTURED, UUID.randomUUID().toString(), "order_abc");
        when(webhookEvents.existsById("evt_1")).thenReturn(true);

        service.handleWebhook(provider, outcome);

        verify(payments, never()).findByOrderId(any());
        verify(publisher, never()).publish(anyString(), any(), any());
    }

    @Test
    void capturedOnActiveOrder_capturesAndEmits() {
        UUID orderId = UUID.randomUUID();
        Payment payment = captured(orderId);
        var outcome = new WebhookOutcome("evt_2", WebhookOutcome.Type.CAPTURED, orderId.toString(), "order_abc");
        when(webhookEvents.existsById("evt_2")).thenReturn(false);
        when(payments.findByOrderId(orderId)).thenReturn(Optional.of(payment));
        when(orderStatus.isCancelledOrTimedOut(orderId)).thenReturn(false);

        service.handleWebhook(provider, outcome);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
        verify(publisher).publish(any(), any(), any()); // PaymentCompleted emitted
    }

    @Test
    void capturedOnCancelledOrder_refunds() {
        UUID orderId = UUID.randomUUID();
        Payment payment = captured(orderId);
        var outcome = new WebhookOutcome("evt_3", WebhookOutcome.Type.CAPTURED, orderId.toString(), "order_abc");
        when(webhookEvents.existsById("evt_3")).thenReturn(false);
        when(payments.findByOrderId(orderId)).thenReturn(Optional.of(payment));
        when(orderStatus.isCancelledOrTimedOut(orderId)).thenReturn(true);
        when(provider.refund(anyString(), any())).thenReturn(new RefundResult("rfnd_1", true));

        service.handleWebhook(provider, outcome);

        verify(provider).refund("order_abc", new BigDecimal("499.00"));
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }
}
