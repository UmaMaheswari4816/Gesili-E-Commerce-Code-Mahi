package com.gecili.ecommerce.common.event;

/**
 * Canonical {@code eventType} string constants and Kafka topic names.
 *
 * <p>Centralizing these avoids typos drifting between producers and consumers (a silent
 * cause of "events published but never consumed"). Keep them in sync with the event
 * catalogue in {@code docs/}.
 */
public final class EventTypes {

    private EventTypes() {}

    // ---- Topics (keyed by orderId; see docs for partition/consumer-group policy) ----
    public static final String TOPIC_ORDER_EVENTS = "order.events";
    public static final String TOPIC_INVENTORY_EVENTS = "inventory.events";
    public static final String TOPIC_PAYMENT_EVENTS = "payment.events";

    /** Dead-letter topic suffix; e.g. {@code order.events.DLT}. */
    public static final String DLT_SUFFIX = ".DLT";

    // ---- Event types ----
    public static final String ORDER_CREATED = "OrderCreated";
    public static final String ORDER_CONFIRMED = "OrderConfirmed";
    public static final String ORDER_CANCELLED = "OrderCancelled";

    public static final String INVENTORY_RESERVED = "InventoryReserved";
    public static final String INVENTORY_RESERVATION_FAILED = "InventoryReservationFailed";
    public static final String INVENTORY_RELEASED = "InventoryReleased";

    public static final String PAYMENT_COMPLETED = "PaymentCompleted";
    public static final String PAYMENT_FAILED = "PaymentFailed";
    public static final String PAYMENT_REFUNDED = "PaymentRefunded";
}
