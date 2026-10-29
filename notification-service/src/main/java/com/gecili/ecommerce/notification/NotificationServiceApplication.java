package com.gecili.ecommerce.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Notification service. Consumes saga events (order/payment/inventory) and sends email/SMS
 * via a channel {@code Strategy}. It owns no business data — only a log of what was sent.
 * Consumption is idempotent (dedupe on the event id) so a redelivered event never
 * double-notifies.
 */
@SpringBootApplication
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
