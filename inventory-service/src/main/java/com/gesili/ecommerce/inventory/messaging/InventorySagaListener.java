package com.gesili.ecommerce.inventory.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gesili.ecommerce.common.event.EventEnvelope;
import com.gesili.ecommerce.common.event.EventTypes;
import com.gesili.ecommerce.common.event.payload.InventoryReservationFailedEvent;
import com.gesili.ecommerce.common.event.payload.InventoryReservedEvent;
import com.gesili.ecommerce.common.event.payload.InventoryReservedEvent.ReservationLine;
import com.gesili.ecommerce.common.exception.InsufficientStockException;
import com.gesili.ecommerce.inventory.dto.InventoryDtos.ReservationResponse;
import com.gesili.ecommerce.inventory.dto.InventoryDtos.ReserveRequest;
import com.gesili.ecommerce.inventory.entity.Reservation;
import com.gesili.ecommerce.inventory.repository.ReservationRepository;
import com.gesili.ecommerce.inventory.service.InventoryService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Drives inventory's part of the choreography saga.
 *
 * <ul>
 *   <li>{@code OrderCreated} &rarr; reserve every line. All succeed &rarr; emit
 *       {@code InventoryReserved}; any shortfall &rarr; emit {@code InventoryReservationFailed}
 *       (and release anything already held for this order).</li>
 *   <li>{@code PaymentCompleted} &rarr; commit the order's reservations.</li>
 *   <li>{@code PaymentFailed} / {@code OrderCancelled} &rarr; release the order's reservations
 *       and emit {@code InventoryReleased} (compensation).</li>
 * </ul>
 *
 * Reserve is idempotent on {@code orderId:sku}, so a redelivered {@code OrderCreated} is safe.
 */
@Slf4j
@Component
public class InventorySagaListener {

    private final ObjectMapper mapper;
    private final InventoryService inventory;
    private final ReservationRepository reservations;
    private final InventoryEventPublisher publisher;

    public InventorySagaListener(
            ObjectMapper mapper,
            InventoryService inventory,
            ReservationRepository reservations,
            InventoryEventPublisher publisher) {
        this.mapper = mapper;
        this.inventory = inventory;
        this.reservations = reservations;
        this.publisher = publisher;
    }

    @KafkaListener(
            topics = {EventTypes.TOPIC_ORDER_EVENTS, EventTypes.TOPIC_PAYMENT_EVENTS},
            groupId = "inventory-service")
    public void onEvent(String message) throws Exception {
        JsonNode env = mapper.readTree(message);
        String type = env.path("eventType").asText();
        JsonNode payload = env.path("payload");

        switch (type) {
            case EventTypes.ORDER_CREATED -> handleOrderCreated(payload);
            case EventTypes.PAYMENT_COMPLETED -> handlePaymentCompleted(payload);
            case EventTypes.PAYMENT_FAILED, EventTypes.ORDER_CANCELLED -> handleRelease(payload, type);
            default -> log.debug("Inventory ignoring event type {}", type);
        }
    }

    private void handleOrderCreated(JsonNode payload) {
        UUID orderId = UUID.fromString(payload.path("orderId").asText());
        List<ReservationLine> reserved = new ArrayList<>();
        try {
            for (JsonNode item : payload.path("items")) {
                UUID productId = UUID.fromString(item.path("productId").asText());
                String sku = item.path("sku").asText();
                int qty = item.path("quantity").asInt();
                ReservationResponse r =
                        inventory.reserve(new ReserveRequest(orderId, productId, sku, qty));
                reserved.add(new ReservationLine(r.reservationId(), productId, sku, qty));
            }
            Instant expiresAt =
                    reservations.findByOrderId(orderId).stream()
                            .map(Reservation::getExpiresAt)
                            .findFirst()
                            .orElse(Instant.now());
            var payloadOut = new InventoryReservedEvent(orderId, reserved, expiresAt);
            publisher.publish(
                    EventTypes.TOPIC_INVENTORY_EVENTS,
                    orderId,
                    EventEnvelope.of(EventTypes.INVENTORY_RESERVED, 1, orderId.toString(), "inventory-service", payloadOut));
            log.info("Reserved all lines for order {}", orderId);
        } catch (InsufficientStockException e) {
            // Compensate: release anything we already held for this order, then emit Failed.
            reservations.findByOrderId(orderId).forEach(r -> inventory.release(r.getId()));
            var failed = new InventoryReservationFailedEvent(orderId, "OUT_OF_STOCK", List.of());
            publisher.publish(
                    EventTypes.TOPIC_INVENTORY_EVENTS,
                    orderId,
                    EventEnvelope.of(EventTypes.INVENTORY_RESERVATION_FAILED, 1, orderId.toString(), "inventory-service", failed));
            log.info("Reservation failed for order {}: {}", orderId, e.getMessage());
        }
    }

    private void handlePaymentCompleted(JsonNode payload) {
        UUID orderId = UUID.fromString(payload.path("orderId").asText());
        reservations.findByOrderId(orderId).forEach(r -> inventory.commit(r.getId()));
        log.info("Committed reservations for order {}", orderId);
    }

    private void handleRelease(JsonNode payload, String type) {
        UUID orderId = UUID.fromString(payload.path("orderId").asText());
        List<UUID> releasedIds = new ArrayList<>();
        for (Reservation r : reservations.findByOrderId(orderId)) {
            if (inventory.release(r.getId())) {
                releasedIds.add(r.getId());
            }
        }
        if (!releasedIds.isEmpty()) {
            var released =
                    new com.gesili.ecommerce.common.event.payload.InventoryReleasedEvent(
                            orderId, releasedIds, type.equals(EventTypes.PAYMENT_FAILED) ? "PAYMENT_FAILED" : "ORDER_CANCELLED");
            publisher.publish(
                    EventTypes.TOPIC_INVENTORY_EVENTS,
                    orderId,
                    EventEnvelope.of(EventTypes.INVENTORY_RELEASED, 1, orderId.toString(), "inventory-service", released));
            log.info("Released {} reservation(s) for order {}", releasedIds.size(), orderId);
        }
    }
}
