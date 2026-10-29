package com.gecili.ecommerce.cart.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Cart settings. {@code ttlSeconds} expires abandoned carts (default 7 days). */
@ConfigurationProperties(prefix = "gecili.cart")
public record CartProperties(long ttlSeconds) {
    public CartProperties {
        if (ttlSeconds <= 0) {
            ttlSeconds = 604800;
        }
    }
}
