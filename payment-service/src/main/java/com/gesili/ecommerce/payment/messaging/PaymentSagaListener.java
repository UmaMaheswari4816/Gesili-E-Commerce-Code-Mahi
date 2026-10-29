package com.gesili.ecommerce.payment.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gesili.ecommerce.common.event.EventTypes;
import com.gesili.ecommerce.payment.service.EventDrivenOrderStatusClient;
import com.gesili.ecommerce.payment.service.PaymentService;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Payment's saga participation:
 * <ul>
 *   <li>{@code InventoryReserved} &rarr; create the provider order (payment INITIATED).</li>
 *   <li>{@code OrderCancelled} &rarr; remember the order is terminal so a late capture webhook
 *       triggers a refund rather than a (wrong) confirmation.</li>
 * </ul>
 * The InventoryReserved payload carries the reservation lines but not the amount, so we rely
 * on the order total via the OrderCreated event the payment service also observes; for
 * simplicity here we read the amount from the reserved event if present, else default to the
 * order lookup (documented simplification).
 *
 * <p>FLOW (payment's entry into the saga — note it does NOT emit the money events here):
 * <pre>
 *   order-service     → Kafka order.events (OrderCreated)      ─▶ HERE: cache the order amount
 *   inventory-service → Kafka inventory.events (InventoryReserved) ─▶ HERE
 *       → PaymentService.initiate() creates the provider order (payment INITIATED)
 *   order-service     → Kafka order.events (OrderCancelled)    ─▶ HERE: mark terminal
 *       (so a LATE capture webhook later triggers a refund, not a confirm)
 * </pre>
 * PaymentCompleted/Failed/Refunded are emitted later, by the WEBHOOK path — see
 * {@code WebhookController} → {@code PaymentService.handleWebhook()}.
 */
@Slf4j
@Component
public class PaymentSagaListener {

    private final ObjectMapper mapper;
    private final PaymentService payments;
    private final EventDrivenOrderStatusClient orderStatus;

    public PaymentSagaListener(
            ObjectMapper mapper,
            PaymentService payments,
            EventDrivenOrderStatusClient orderStatus) {
        this.mapper = mapper;
        this.payments = payments;
        this.orderStatus = orderStatus;
    }

    @KafkaListener(
            topics = {EventTypes.TOPIC_INVENTORY_EVENTS, EventTypes.TOPIC_ORDER_EVENTS},
            groupId = "payment-service")
    public void onEvent(String message) throws Exception {
        JsonNode env = mapper.readTree(message);
        String type = env.path("eventType").asText();
        JsonNode payload = env.path("payload");

        switch (type) {
            case EventTypes.INVENTORY_RESERVED -> {
                UUID orderId = UUID.fromString(payload.path("orderId").asText());
                // Amount/currency were cached from OrderCreated (InventoryReserved omits them).
                payments.initiate(
                        orderId, OrderAmounts.amount(orderId), OrderAmounts.currency(orderId), null);
            }
            case EventTypes.ORDER_CREATED -> {
                // Capture amount/currency so initiate() can use the real total.
                UUID orderId = UUID.fromString(payload.path("orderId").asText());
                BigDecimal amount = payload.path("totalAmount").decimalValue();
                String currency = payload.path("currency").asText("INR");
                OrderAmounts.remember(orderId, amount, currency);
            }
            case EventTypes.ORDER_CANCELLED -> {
                UUID orderId = UUID.fromString(payload.path("orderId").asText());
                orderStatus.markCancelled(orderId);
            }
            default -> log.debug("Payment ignoring event type {}", type);
        }
    }
}
