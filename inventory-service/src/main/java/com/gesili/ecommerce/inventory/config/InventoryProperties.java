package com.gesili.ecommerce.inventory.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Inventory settings. {@code reservationTtlSeconds} defaults to 1200 (20 minutes),
 * intentionally longer than the order payment deadline (15 minutes) so a reservation never
 * expires while payment is still legitimately in flight.
 */
@ConfigurationProperties(prefix = "gesili.inventory")
public record InventoryProperties(long reservationTtlSeconds) {

    public InventoryProperties {
        if (reservationTtlSeconds <= 0) {
            reservationTtlSeconds = 1200;
        }
    }
}
