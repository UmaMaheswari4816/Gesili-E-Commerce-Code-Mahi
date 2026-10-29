package com.gecili.ecommerce.order.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gecili.ecommerce.common.event.EventEnvelope;
import com.gecili.ecommerce.order.entity.OutboxEvent;
import com.gecili.ecommerce.order.repository.OutboxRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Writes domain events into the outbox table. MUST be called within the same transaction as
 * the state change it accompanies (the caller's {@code @Transactional} boundary), so the row
 * and the business change commit atomically.
 */
@Component
public class OutboxWriter {

    private final OutboxRepository outbox;
    private final ObjectMapper mapper;

    public OutboxWriter(OutboxRepository outbox, ObjectMapper mapper) {
        this.outbox = outbox;
        this.mapper = mapper;
    }

    /** Serialize the envelope and stage it for publication to {@code topic}, keyed by orderId. */
    public <T> void write(String topic, UUID aggregateId, EventEnvelope<T> envelope) {
        try {
            String json = mapper.writeValueAsString(envelope);
            outbox.save(OutboxEvent.of(topic, aggregateId, envelope.eventType(), json));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize outbox event", e);
        }
    }
}
