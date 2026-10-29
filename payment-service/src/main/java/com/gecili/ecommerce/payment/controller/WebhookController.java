package com.gecili.ecommerce.payment.controller;

import com.gecili.ecommerce.payment.provider.PaymentProvider;
import com.gecili.ecommerce.payment.provider.PaymentProvider.WebhookOutcome;
import com.gecili.ecommerce.payment.provider.PaymentProviderFactory;
import com.gecili.ecommerce.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Provider webhook endpoint (Phase 0 Note 2).
 *
 * <p>Public route (the gateway allow-lists {@code /api/v1/payments/webhook/**} and applies NO
 * body-reading filters). The controller takes the <strong>raw request body as a String</strong>
 * via {@code @RequestBody String} — Spring hands over the unparsed bytes — so the provider
 * signature is verified over the EXACT bytes the provider signed. We do NOT bind to a DTO here,
 * because deserializing + reserializing would change the bytes and break verification.
 *
 * <p>Flow: verify signature over raw body &rarr; parse &rarr; idempotent, forward-only
 * processing. Always returns 200 on a verified webhook (including replays) so the provider
 * stops retrying; returns 400 only when the signature is invalid.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/payments/webhook")
@Tag(name = "Payment Webhooks", description = "Provider callbacks (public, signature-verified)")
public class WebhookController {

    private final PaymentProviderFactory providers;
    private final PaymentService payments;

    public WebhookController(PaymentProviderFactory providers, PaymentService payments) {
        this.providers = providers;
        this.payments = payments;
    }

    @Operation(summary = "Receive a provider webhook (raw body, signature-verified, idempotent)")
    @PostMapping("/{provider}")
    public ResponseEntity<String> receive(
            @PathVariable String provider,
            @RequestHeader(name = "X-Razorpay-Signature", required = false) String razorpaySig,
            @RequestHeader(name = "Stripe-Signature", required = false) String stripeSig,
            @RequestBody String rawBody) {

        PaymentProvider strategy = providers.byName(provider);
        String signature = "razorpay".equalsIgnoreCase(provider) ? razorpaySig : stripeSig;

        if (!strategy.verifySignature(rawBody, signature)) {
            log.warn("Rejected {} webhook: bad signature", provider);
            return ResponseEntity.badRequest().body("invalid signature");
        }

        WebhookOutcome outcome = strategy.parseWebhook(rawBody);
        payments.handleWebhook(strategy, outcome);
        return ResponseEntity.ok("ok");
    }
}
