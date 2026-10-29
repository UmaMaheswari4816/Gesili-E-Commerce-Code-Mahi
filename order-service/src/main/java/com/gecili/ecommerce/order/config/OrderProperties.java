package com.gecili.ecommerce.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Order settings. {@code paymentDeadlineSeconds} defaults to 900 (15 minutes) — shorter than
 * the inventory reservation TTL (20 minutes) so a reservation never expires mid-payment.
 */
@ConfigurationProperties(prefix = "gecili.order")
public record OrderProperties(long paymentDeadlineSeconds) {
    public OrderProperties {
        if (paymentDeadlineSeconds <= 0) {
            paymentDeadlineSeconds = 900;
        }
    }
}
