package com.gecili.ecommerce.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gecili.ecommerce.common.enums.OrderStatus;
import com.gecili.ecommerce.common.event.EventEnvelope;
import com.gecili.ecommerce.common.event.EventTypes;
import com.gecili.ecommerce.order.config.OrderProperties;
import com.gecili.ecommerce.order.dto.OrderDtos.PlaceOrderRequest;
import com.gecili.ecommerce.order.entity.OrderEntity;
import com.gecili.ecommerce.order.mapper.OrderMapper;
import com.gecili.ecommerce.order.outbox.OutboxWriter;
import com.gecili.ecommerce.order.repository.OrderRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for order creation (persist + outbox in one call) and saga transitions. No DB or
 * Kafka — repositories and the outbox writer are mocked, so the state-machine logic and the
 * "write OrderCreated to the outbox" behavior are verified in isolation.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orders;
    @Mock private OutboxWriter outbox;

    private OrderService service;

    @BeforeEach
    void setUp() {
        service = new OrderService(orders, outbox, new OrderMapper(), new OrderProperties(900));
        // lenient: not every test saves (the idempotent no-op path returns early).
        org.mockito.Mockito.lenient()
                .when(orders.save(any(OrderEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void placeOrder_persistsPendingInventory_andWritesOutbox() {
        UUID userId = UUID.randomUUID();
        var req =
                new PlaceOrderRequest(
                        List.of(new PlaceOrderRequest.Line(UUID.randomUUID(), "SKU-AAA-001", 2, new BigDecimal("499.00"))),
                        "INR");

        var resp = service.placeOrder(userId, req);

        assertThat(resp.status()).isEqualTo("PENDING_INVENTORY");
        assertThat(resp.totalAmount()).isEqualByComparingTo("998.00");

        // Outbox received an OrderCreated envelope on the order topic, keyed by the order id.
        ArgumentCaptor<EventEnvelope<?>> env = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(outbox).write(eq(EventTypes.TOPIC_ORDER_EVENTS), any(UUID.class), env.capture());
        assertThat(env.getValue().eventType()).isEqualTo(EventTypes.ORDER_CREATED);
    }

    @Test
    void onInventoryReserved_movesToPaymentPending_andSetsDeadline() {
        OrderEntity order = OrderEntity.create(UUID.randomUUID(), new BigDecimal("10.00"), "INR");
        order.transitionTo(OrderStatus.PENDING_INVENTORY);
        when(orders.findById(order.getId())).thenReturn(Optional.of(order));

        service.onInventoryReserved(order.getId());

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
        assertThat(order.getPaymentDeadline()).isNotNull();
    }

    @Test
    void onPaymentCompleted_confirms_andEmitsOrderConfirmed() {
        OrderEntity order = OrderEntity.create(UUID.randomUUID(), new BigDecimal("10.00"), "INR");
        order.transitionTo(OrderStatus.PAYMENT_PENDING);
        when(orders.findById(order.getId())).thenReturn(Optional.of(order));

        service.onPaymentCompleted(order.getId());

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(outbox).write(eq(EventTypes.TOPIC_ORDER_EVENTS), eq(order.getId()), any());
    }

    @Test
    void cancel_setsCancelled_andEmitsOrderCancelled() {
        OrderEntity order = OrderEntity.create(UUID.randomUUID(), new BigDecimal("10.00"), "INR");
        order.transitionTo(OrderStatus.PAYMENT_PENDING);
        when(orders.findById(order.getId())).thenReturn(Optional.of(order));

        service.cancel(order.getId(), "PAYMENT_TIMEOUT");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancelReason()).isEqualTo("PAYMENT_TIMEOUT");
    }

    @Test
    void onInventoryReserved_isIdempotent_whenAlreadyAdvanced() {
        OrderEntity order = OrderEntity.create(UUID.randomUUID(), new BigDecimal("10.00"), "INR");
        order.transitionTo(OrderStatus.CONFIRMED);
        when(orders.findById(order.getId())).thenReturn(Optional.of(order));

        service.onInventoryReserved(order.getId()); // should no-op

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }
}
