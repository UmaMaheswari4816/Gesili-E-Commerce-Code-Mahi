package com.gecili.ecommerce.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Idempotency record for provider webhooks. The provider's event id is the primary key; a
 * replayed webhook (providers retry) hits the unique PK and is skipped without
 * re-processing. Returning 200 on a replay (without side effects) is the correct behavior.
 */
@Entity
@Table(name = "webhook_events")
@Getter
@Setter
@NoArgsConstructor
public class WebhookEvent {

    /** Provider-assigned event id (e.g. Razorpay/Stripe event id). */
    @Id
    @Column(name = "provider_event_id")
    private String providerEventId;

    @Column(nullable = false)
    private String provider;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    public static WebhookEvent of(String providerEventId, String provider) {
        WebhookEvent w = new WebhookEvent();
        w.providerEventId = providerEventId;
        w.provider = provider;
        w.receivedAt = Instant.now();
        return w;
    }
}
