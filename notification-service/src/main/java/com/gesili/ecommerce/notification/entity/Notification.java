package com.gesili.ecommerce.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A log of a notification that was sent (audit trail; not authoritative business data). */
@Entity
@Table(name = "notifications", indexes = @Index(name = "idx_notif_order", columnList = "order_id"))
@Getter
@Setter
@NoArgsConstructor
public class Notification {

    @Id private UUID id;

    @Column(name = "order_id")
    private UUID orderId;

    @Column(nullable = false)
    private String channel;

    @Column(nullable = false)
    private String recipient;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(nullable = false)
    private boolean delivered;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static Notification of(
            UUID orderId, String channel, String recipient, String eventType,
            String subject, String body, boolean delivered) {
        Notification n = new Notification();
        n.id = UUID.randomUUID();
        n.orderId = orderId;
        n.channel = channel;
        n.recipient = recipient;
        n.eventType = eventType;
        n.subject = subject;
        n.body = body;
        n.delivered = delivered;
        n.createdAt = Instant.now();
        return n;
    }
}
