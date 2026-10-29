package com.gecili.ecommerce.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Payment settings. Secrets come from env (never committed). {@code defaultProvider} selects
 * the Strategy used when no {@code X-Payment-Provider} header is present.
 */
@ConfigurationProperties(prefix = "gecili.payment")
public record PaymentProperties(String defaultProvider, Razorpay razorpay, Stripe stripe) {

    public PaymentProperties {
        if (defaultProvider == null || defaultProvider.isBlank()) {
            defaultProvider = "razorpay";
        }
        if (razorpay == null) {
            razorpay = new Razorpay(null, null, null);
        }
        if (stripe == null) {
            stripe = new Stripe(null, null);
        }
    }

    public record Razorpay(String keyId, String keySecret, String webhookSecret) {}

    public record Stripe(String apiKey, String webhookSecret) {}
}
