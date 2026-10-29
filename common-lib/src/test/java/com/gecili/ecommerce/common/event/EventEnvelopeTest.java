package com.gecili.ecommerce.common.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gecili.ecommerce.common.event.payload.PaymentCompletedEvent;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EventEnvelopeTest {

    private final ObjectMapper mapper =
            new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void of_stampsEventIdAndTimestamp() {
        var payload = new PaymentCompletedEvent(UUID.randomUUID(), UUID.randomUUID(), "pay_123", new BigDecimal("499.00"), "INR");

        var envelope = EventEnvelope.of(EventTypes.PAYMENT_COMPLETED, 1, "corr-1", "payment-service", payload);

        assertThat(envelope.eventId()).isNotNull();
        assertThat(envelope.occurredAt()).isNotNull();
        assertThat(envelope.eventType()).isEqualTo("PaymentCompleted");
        assertThat(envelope.version()).isEqualTo(1);
        assertThat(envelope.correlationId()).isEqualTo("corr-1");
        assertThat(envelope.source()).isEqualTo("payment-service");
        assertThat(envelope.payload()).isEqualTo(payload);
    }

    @Test
    void defaultsAreApplied_whenNullOrInvalid() {
        var envelope = new EventEnvelope<>(null, "OrderCreated", 0, null, null, "order-service", "body");

        assertThat(envelope.eventId()).isNotNull();   // generated
        assertThat(envelope.occurredAt()).isNotNull(); // now
        assertThat(envelope.version()).isEqualTo(1);   // coerced from 0
    }

    @Test
    void blankRequiredFields_areRejected() {
        assertThatThrownBy(() -> EventEnvelope.of("  ", 1, null, "svc", "x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("eventType");

        assertThatThrownBy(() -> EventEnvelope.of("Type", 1, null, "", "x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("source");
    }

    @Test
    void roundTripsThroughJackson() throws Exception {
        var payload = new PaymentCompletedEvent(UUID.randomUUID(), UUID.randomUUID(), "pay_123", new BigDecimal("499.00"), "INR");
        var original = EventEnvelope.of(EventTypes.PAYMENT_COMPLETED, 2, "corr-9", "payment-service", payload);

        String json = mapper.writeValueAsString(original);
        var type = mapper.getTypeFactory()
                .constructParametricType(EventEnvelope.class, PaymentCompletedEvent.class);
        EventEnvelope<PaymentCompletedEvent> back = mapper.readValue(json, type);

        assertThat(back.eventId()).isEqualTo(original.eventId());
        assertThat(back.version()).isEqualTo(2);
        assertThat(back.payload()).isEqualTo(payload);
    }
}
