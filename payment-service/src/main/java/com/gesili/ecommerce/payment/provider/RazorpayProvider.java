package com.gesili.ecommerce.payment.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gesili.ecommerce.payment.config.PaymentProperties;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Razorpay provider (default). Sandbox-oriented: creates a deterministic order reference and
 * verifies webhooks with HMAC-SHA256 over the raw body using the configured webhook secret —
 * exactly how Razorpay signs its {@code X-Razorpay-Signature} header.
 *
 * <p>This implementation does not call the live Razorpay API (no SDK/live keys required to
 * build and test); the signature verification and webhook parsing mirror the real contract so
 * swapping in the SDK later is localized to {@link #createOrder} and {@link #refund}.
 */
@Slf4j
@Component
public class RazorpayProvider implements PaymentProvider {

    private final PaymentProperties props;
    private final ObjectMapper mapper;

    public RazorpayProvider(PaymentProperties props, ObjectMapper mapper) {
        this.props = props;
        this.mapper = mapper;
    }

    @Override
    public String name() {
        return "razorpay";
    }

    @Override
    public CreatedOrder createOrder(UUID orderId, BigDecimal amount, String currency) {
        // Real impl: POST /v1/orders. Sandbox: derive a stable ref from the order id.
        String providerRef = "order_" + orderId.toString().replace("-", "").substring(0, 14);
        return new CreatedOrder(providerRef, props.razorpay().keyId());
    }

    @Override
    public boolean verifySignature(String rawBody, String signatureHeader) {
        String secret = props.razorpay().webhookSecret();
        if (secret == null || secret.isBlank()) {
            log.warn("Razorpay webhook secret not configured; rejecting webhook");
            return false;
        }
        String expected = HmacSignatures.hmacHex(rawBody, secret);
        return HmacSignatures.constantTimeEquals(expected, signatureHeader);
    }

    @Override
    public WebhookOutcome parseWebhook(String rawBody) {
        try {
            JsonNode root = mapper.readTree(rawBody);
            String eventId = root.path("id").asText(root.path("event_id").asText());
            String event = root.path("event").asText();
            JsonNode entity =
                    root.path("payload").path("payment").path("entity");
            String providerRef = entity.path("order_id").asText(null);
            String orderId = root.path("notes").path("orderId").asText(entity.path("notes").path("orderId").asText(null));

            WebhookOutcome.Type type =
                    switch (event) {
                        case "payment.captured" -> WebhookOutcome.Type.CAPTURED;
                        case "payment.failed" -> WebhookOutcome.Type.FAILED;
                        case "refund.processed" -> WebhookOutcome.Type.REFUNDED;
                        default -> WebhookOutcome.Type.IGNORED;
                    };
            return new WebhookOutcome(eventId, type, orderId, providerRef);
        } catch (Exception e) {
            log.error("Failed to parse Razorpay webhook: {}", e.getMessage());
            return new WebhookOutcome(UUID.randomUUID().toString(), WebhookOutcome.Type.IGNORED, null, null);
        }
    }

    @Override
    public RefundResult refund(String providerRef, BigDecimal amount) {
        // Real impl: POST /v1/payments/{id}/refund. Sandbox: synthesize a success.
        return new RefundResult("rfnd_" + providerRef, true);
    }
}
