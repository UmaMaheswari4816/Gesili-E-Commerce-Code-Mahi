package com.gecili.ecommerce.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Transactional outbox row.
 *
 * <p>Written in the SAME DB transaction as the order change. A relay later publishes
 * unpublished rows to Kafka and stamps {@code publishedAt}. This closes the dual-write gap
 * (DB committed but event lost, or vice-versa): either both the order row and the outbox row
 * commit, or neither does.
 *
 * <p>Ordering: the relay selects {@code WHERE published_at IS NULL ORDER BY id} (monotonic
 * BIGSERIAL), with NO high-water-mark cursor, and publishes keyed by {@code aggregateId}
 * (orderId) so per-order ordering holds end to end.
 */
@Entity
@Table(name = "outbox", indexes = @Index(name = "idx_outbox_unpublished", columnList = "published_at, id"))
@Getter
@Setter
@NoArgsConstructor
public class OutboxEvent {

    /** Monotonic id; the relay orders by this. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Kafka topic to publish to. */
    @Column(nullable = false)
    private String topic;

    /** Partition key = aggregate id (orderId) for per-order ordering. */
    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    /** Serialized EventEnvelope JSON. */
    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Null until published; set when the relay confirms the send. */
    @Column(name = "published_at")
    private Instant publishedAt;

    public static OutboxEvent of(String topic, UUID aggregateId, String eventType, String payload) {
        OutboxEvent e = new OutboxEvent();
        e.topic = topic;
        e.aggregateId = aggregateId;
        e.eventType = eventType;
        e.payload = payload;
        e.createdAt = Instant.now();
        return e;
    }
}
