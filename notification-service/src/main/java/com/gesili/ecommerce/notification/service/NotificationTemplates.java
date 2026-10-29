package com.gesili.ecommerce.notification.service;

import com.gesili.ecommerce.common.event.EventTypes;
import java.util.UUID;

/**
 * Maps a saga event type to a human-readable subject + body. Kept as a simple switch for
 * clarity; a real system would use a template engine (Thymeleaf/FreeMarker) and localized
 * message bundles.
 */
public final class NotificationTemplates {

    private NotificationTemplates() {}

    public record Message(String subject, String body) {}

    public static Message render(String eventType, UUID orderId, String reason) {
        return switch (eventType) {
            case EventTypes.ORDER_CONFIRMED ->
                    new Message(
                            "Your order is confirmed",
                            "Good news! Your order " + orderId + " has been confirmed and is being prepared.");
            case EventTypes.ORDER_CANCELLED ->
                    new Message(
                            "Your order was cancelled",
                            "Your order " + orderId + " was cancelled"
                                    + (reason != null ? " (" + reason + ")" : "") + ".");
            case EventTypes.PAYMENT_COMPLETED ->
                    new Message(
                            "Payment received",
                            "We received your payment for order " + orderId + ". Thank you!");
            case EventTypes.PAYMENT_FAILED ->
                    new Message(
                            "Payment failed",
                            "Your payment for order " + orderId + " could not be processed.");
            case EventTypes.PAYMENT_REFUNDED ->
                    new Message(
                            "Refund issued",
                            "A refund for order " + orderId + " has been issued"
                                    + (reason != null ? " (" + reason + ")" : "") + ".");
            case EventTypes.INVENTORY_RESERVATION_FAILED ->
                    new Message(
                            "Item out of stock",
                            "Sorry — an item in your order " + orderId + " is out of stock, so the order was cancelled.");
            default -> new Message("Order update", "There is an update on your order " + orderId + ".");
        };
    }
}
