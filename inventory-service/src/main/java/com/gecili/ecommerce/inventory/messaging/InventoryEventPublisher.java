package com.gecili.ecommerce.inventory.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gecili.ecommerce.common.event.EventEnvelope;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes inventory events to Kafka, keyed by orderId.
 *
 * <p>Note: for simplicity this service publishes directly (not via an outbox). That is a
 * deliberate learning simplification — order-service demonstrates the full outbox pattern.
 * The trade-off: a crash between committing the reservation and publishing the event could
 * drop the event; the reservation-expiry job and consumer idempotency bound the blast radius.
 * Adding an outbox here mirrors order-service exactly.
 */
@Slf4j
@Component
public class InventoryEventPublisher {

    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper mapper;

    public InventoryEventPublisher(KafkaTemplate<String, String> kafka, ObjectMapper mapper) {
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
