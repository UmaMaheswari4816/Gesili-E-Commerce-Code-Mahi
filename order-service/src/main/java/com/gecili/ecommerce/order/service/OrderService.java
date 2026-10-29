package com.gecili.ecommerce.order.service;

import com.gecili.ecommerce.common.enums.OrderStatus;
import com.gecili.ecommerce.common.event.EventEnvelope;
import com.gecili.ecommerce.common.event.EventTypes;
import com.gecili.ecommerce.common.event.payload.OrderCreatedEvent;
import com.gecili.ecommerce.common.exception.ResourceNotFoundException;
import com.gecili.ecommerce.order.config.OrderProperties;
import com.gecili.ecommerce.order.dto.OrderDtos.OrderResponse;
import com.gecili.ecommerce.order.dto.OrderDtos.PlaceOrderRequest;
import com.gecili.ecommerce.order.entity.OrderEntity;
import com.gecili.ecommerce.order.entity.OrderItem;
import com.gecili.ecommerce.order.mapper.OrderMapper;
import com.gecili.ecommerce.order.outbox.OutboxWriter;
import com.gecili.ecommerce.order.repository.OrderRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Order lifecycle + saga transitions.
 *
 * <p>{@link #placeOrder} persists the order (status {@code PENDING_INVENTORY}) and an
 * {@code OrderCreated} outbox row in ONE transaction (transactional outbox). The relay
 * publishes it; inventory reserves; events flow back and drive the transitions below.
 */
@Slf4j
@Service
public class OrderService {

    private final OrderRepository orders;
    private final OutboxWriter outbox;
    private final OrderMapper mapper;
    private final OrderProperties props;

    public OrderService(
            OrderRepository orders, OutboxWriter outbox, OrderMapper mapper, OrderProperties props) {
        this.orders = orders;
        this.outbox = outbox;
        this.mapper = mapper;
        this.props = props;
    }

    @Transactional
    public OrderResponse placeOrder(UUID userId, PlaceOrderRequest req) {
        BigDecimal total =
                req.items().stream()
                        .map(l -> l.unitPrice().multiply(BigDecimal.valueOf(l.quantity())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        OrderEntity order = OrderEntity.create(userId, total, req.currency());
        req.items()
                .forEach(
                        l ->
                                order
                                        .getItems()
                                        .add(OrderItem.of(order.getId(), l.productId(), l.sku(), l.quantity(), l.unitPrice())));
        order.transitionTo(OrderStatus.PENDING_INVENTORY);
        orders.save(order);

        // Same transaction: stage OrderCreated in the outbox.
        List<OrderCreatedEvent.OrderLine> lines =
                order.getItems().stream()
                        .map(i -> new OrderCreatedEvent.OrderLine(i.getProductId(), i.getSku(), i.getQuantity(), i.getUnitPrice()))
                        .toList();
        var payload = new OrderCreatedEvent(order.getId(), userId, lines, total, req.currency());
        var envelope =
                EventEnvelope.of(EventTypes.ORDER_CREATED, 1, order.getId().toString(), "order-service", payload);
        outbox.write(EventTypes.TOPIC_ORDER_EVENTS, order.getId(), envelope);

        log.info("Placed order {} for user {} ({} items)", order.getId(), userId, lines.size());
        return mapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse get(UUID orderId) {
        return orders
                .findById(orderId)
                .map(mapper::toResponse)
                .orElseThrow(() -> ResourceNotFoundException.of("Order", orderId));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listForUser(UUID userId) {
        return orders.findByUserId(userId).stream().map(mapper::toResponse).toList();
    }

    // ---- Saga transitions (called by event consumers; each is idempotent at the status level) ----

    /** InventoryReserved -> move to PAYMENT_PENDING and set the payment deadline. */
    @Transactional
    public void onInventoryReserved(UUID orderId) {
        OrderEntity order = require(orderId);
        if (order.getStatus() != OrderStatus.PENDING_INVENTORY) {
            return; // already advanced; ignore duplicate
        }
        order.setPaymentDeadline(Instant.now().plusSeconds(props.paymentDeadlineSeconds()));
        order.transitionTo(OrderStatus.PAYMENT_PENDING);
        orders.save(order);
    }

    /** PaymentCompleted -> CONFIRMED + emit OrderConfirmed. */
    @Transactional
    public void onPaymentCompleted(UUID orderId) {
        OrderEntity order = require(orderId);
        if (order.getStatus() == OrderStatus.CONFIRMED) {
            return;
        }
        order.transitionTo(OrderStatus.CONFIRMED);
        orders.save(order);
        emitTerminal(order, EventTypes.ORDER_CONFIRMED, null);
    }

    /** Inventory failure / payment failure / timeout -> CANCELLED + emit OrderCancelled. */
    @Transactional
    public void cancel(UUID orderId, String reason) {
        OrderEntity order = require(orderId);
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.CONFIRMED) {
            return;
        }
        order.setCancelReason(reason);
        order.transitionTo(OrderStatus.CANCELLED);
        orders.save(order);
        emitTerminal(order, EventTypes.ORDER_CANCELLED, reason);
    }

    private void emitTerminal(OrderEntity order, String eventType, String reason) {
        Object payload =
                EventTypes.ORDER_CONFIRMED.equals(eventType)
                        ? new com.gecili.ecommerce.common.event.payload.OrderConfirmedEvent(order.getId())
                        : new com.gecili.ecommerce.common.event.payload.OrderCancelledEvent(order.getId(), reason);
        var envelope = EventEnvelope.of(eventType, 1, order.getId().toString(), "order-service", payload);
        outbox.write(EventTypes.TOPIC_ORDER_EVENTS, order.getId(), envelope);
    }

    private OrderEntity require(UUID orderId) {
        return orders.findById(orderId).orElseThrow(() -> ResourceNotFoundException.of("Order", orderId));
    }
}
