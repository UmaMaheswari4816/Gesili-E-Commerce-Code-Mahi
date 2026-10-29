package com.gecili.ecommerce.order.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gecili.ecommerce.common.event.EventTypes;
import com.gecili.ecommerce.order.service.EventDeduplicator;
import com.gecili.ecommerce.order.service.OrderService;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes inventory + payment events and drives the order saga.
 *
 * <p>Each message is an {@code EventEnvelope} JSON. We dedupe on {@code eventId} (Kafka is
 * at-least-once), then route on {@code eventType}. Consumer group {@code order-service} keeps
 * independent offsets; keying by orderId upstream preserves per-order ordering.
 */
@Slf4j
@Component
public class SagaEventListener {

    private final ObjectMapper mapper;
    private final OrderService orderService;
    private final EventDeduplicator dedup;

    public SagaEventListener(
            ObjectMapper mapper, OrderService orderService, EventDeduplicator dedup) {
        this.mapper = mapper;
        this.orderService = orderService;
        this.dedup = dedup;
    }

    @KafkaListener(
            topics = {EventTypes.TOPIC_INVENTORY_EVENTS, EventTypes.TOPIC_PAYMENT_EVENTS},
            groupId = "order-service")
    public void onEvent(String message) throws Exception {
        JsonNode env = mapper.readTree(message);
        UUID eventId = UUID.fromString(env.path("eventId").asText());
        String type = env.path("eventType").asText();
        JsonNode payload = env.path("payload");

        if (!dedup.markIfNew(eventId)) {
            log.debug("Skipping duplicate event {} ({})", eventId, type);
            return;
        }

        switch (type) {
            case EventTypes.INVENTORY_RESERVED -> {
                UUID orderId = UUID.fromString(payload.path("orderId").asText());
                orderService.onInventoryReserved(orderId);
            }
            case EventTypes.INVENTORY_RESERVATION_FAILED -> {
                UUID orderId = UUID.fromString(payload.path("orderId").asText());
                orderService.cancel(orderId, "OUT_OF_STOCK");
            }
            case EventTypes.PAYMENT_COMPLETED -> {
                UUID orderId = UUID.fromString(payload.path("orderId").asText());
                orderService.onPaymentCompleted(orderId);
            }
            case EventTypes.PAYMENT_FAILED -> {
                UUID orderId = UUID.fromString(payload.path("orderId").asText());
                orderService.cancel(orderId, "PAYMENT_FAILED");
            }
            default -> log.debug("Ignoring event type {}", type);
        }
    }
}
