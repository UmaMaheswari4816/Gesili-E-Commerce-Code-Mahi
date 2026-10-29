package com.gesili.ecommerce.payment.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gesili.ecommerce.common.event.EventEnvelope;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/** Publishes payment events to Kafka, keyed by orderId. */
@Slf4j
@Component
public class PaymentEventPublisher {

    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper mapper;

    public PaymentEventPublisher(KafkaTemplate<String, String> kafka, ObjectMapper mapper) {
        this.kafka = kafka;
        this.mapper = mapper;
    }

    public <T> void publish(String topic, UUID orderId, EventEnvelope<T> envelope) {
        try {
            kafka.send(topic, orderId.toString(), mapper.writeValueAsString(envelope));
        } catch (Exception e) {
            log.error("Failed to publish {} for order {}: {}", envelope.eventType(), orderId, e.getMessage());
        }
    }
}
