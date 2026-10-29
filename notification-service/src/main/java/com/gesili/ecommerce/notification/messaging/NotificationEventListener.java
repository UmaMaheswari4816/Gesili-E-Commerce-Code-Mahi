package com.gesili.ecommerce.notification.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gesili.ecommerce.common.event.EventTypes;
import com.gesili.ecommerce.notification.service.EventDeduplicator;
import com.gesili.ecommerce.notification.service.NotificationService;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes order/payment/inventory events and sends a customer notification for the ones that
 * matter. Dedupes on the envelope {@code eventId} so redelivery never double-notifies.
 *
 * <p>Recipient resolution is a simplification: in a real system the service would look up the
 * user's email/phone (from auth-service or a cached profile). Here we derive a placeholder
 * from the order id and log it; wiring a real lookup is a documented extension.
 *
 * <p>FLOW (the END of the saga — this service only reacts, never emits):
 * <pre>
 *   order.events / payment.events / inventory.events ─▶ HERE
 *       OrderConfirmed / OrderCancelled / PaymentCompleted / PaymentFailed /
 *       PaymentRefunded / InventoryReservationFailed
 *         → dedupe on eventId → NotificationService.notify()
 *            → NotificationTemplates.render() → EmailChannel/SmsChannel.send() → log row
 * </pre>
 */
@Slf4j
@Component
public class NotificationEventListener {

    private static final Set<String> NOTIFY_ON =
            Set.of(
                    EventTypes.ORDER_CONFIRMED,
                    EventTypes.ORDER_CANCELLED,
                    EventTypes.PAYMENT_COMPLETED,
                    EventTypes.PAYMENT_FAILED,
                    EventTypes.PAYMENT_REFUNDED,
                    EventTypes.INVENTORY_RESERVATION_FAILED);

    private final ObjectMapper mapper;
    private final NotificationService notifications;
    private final EventDeduplicator dedup;

    public NotificationEventListener(
            ObjectMapper mapper, NotificationService notifications, EventDeduplicator dedup) {
        this.mapper = mapper;
        this.notifications = notifications;
        this.dedup = dedup;
    }

    @KafkaListener(
            topics = {
                EventTypes.TOPIC_ORDER_EVENTS,
                EventTypes.TOPIC_PAYMENT_EVENTS,
                EventTypes.TOPIC_INVENTORY_EVENTS
            },
            groupId = "notification-service")
    public void onEvent(String message) throws Exception {
        JsonNode env = mapper.readTree(message);
        String type = env.path("eventType").asText();
        if (!NOTIFY_ON.contains(type)) {
            return;
        }
        UUID eventId = UUID.fromString(env.path("eventId").asText());
        if (!dedup.markIfNew(eventId)) {
            log.debug("Skipping duplicate event {} ({})", eventId, type);
            return;
        }

        JsonNode payload = env.path("payload");
        UUID orderId = UUID.fromString(payload.path("orderId").asText());
        String reason = payload.path("reason").asText(null);

        // Placeholder recipient; a real impl resolves the user's contact details.
        String recipient = "user-" + orderId.toString().substring(0, 8) + "@example.com";
        notifications.notify(type, orderId, recipient, "email", reason);
        log.info("Notified for {} on order {}", type, orderId);
    }
}
