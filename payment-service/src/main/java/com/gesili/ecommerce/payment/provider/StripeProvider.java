package com.gesili.ecommerce.payment.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gesili.ecommerce.payment.config.PaymentProperties;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Stripe provider (second strategy). Demonstrates that adding a provider is just another
 * implementation. Stripe signs webhooks with HMAC-SHA256 over {@code "{timestamp}.{body}"};
 * here we verify HMAC over the raw body for parity with the test harness (a full Stripe impl
 * parses the {@code t=} and {@code v1=} parts of the {@code Stripe-Signature} header).
 */
@Slf4j
@Component
public class StripeProvider implements PaymentProvider {

    private final PaymentProperties props;
    private final ObjectMapper mapper;

    public StripeProvider(PaymentProperties props, ObjectMapper mapper) {
        this.props = props;
        this.mapper = mapper;
    }

    @Override
    public String name() {
        return "stripe";
    }

    @Override
    public CreatedOrder createOrder(UUID orderId, BigDecimal amount, String currency) {
        String providerRef = "pi_" + orderId.toString().replace("-", "").substring(0, 14);
        return new CreatedOrder(providerRef, null);
    }

    @Override
    public boolean verifySignature(String rawBody, String signatureHeader) {
        String secret = props.stripe().webhookSecret();
        if (secret == null || secret.isBlank()) {
            log.warn("Stripe webhook secret not configured; rejecting webhook");
            return false;
        }
        String expected = HmacSignatures.hmacHex(rawBody, secret);
        return HmacSignatures.constantTimeEquals(expected, signatureHeader);
    }

    @Override
    public WebhookOutcome parseWebhook(String rawBody) {
        try {
            JsonNode root = mapper.readTree(rawBody);
            String eventId = root.path("id").asText();
            String type = root.path("type").asText();
            JsonNode obj = root.path("data").path("object");
            String providerRef = obj.path("id").asText(null);
            String orderId = obj.path("metadata").path("orderId").asText(null);

            WebhookOutcome.Type t =
                    switch (type) {
                        case "payment_intent.succeeded" -> WebhookOutcome.Type.CAPTURED;
                        case "payment_intent.payment_failed" -> WebhookOutcome.Type.FAILED;
                        case "charge.refunded" -> WebhookOutcome.Type.REFUNDED;
                        default -> WebhookOutcome.Type.IGNORED;
                    };
            return new WebhookOutcome(eventId, t, orderId, providerRef);
        } catch (Exception e) {
            log.error("Failed to parse Stripe webhook: {}", e.getMessage());
            return new WebhookOutcome(UUID.randomUUID().toString(), WebhookOutcome.Type.IGNORED, null, null);
        }
    }

    @Override
    public RefundResult refund(String providerRef, BigDecimal amount) {
        return new RefundResult("re_" + providerRef, true);
    }
}
