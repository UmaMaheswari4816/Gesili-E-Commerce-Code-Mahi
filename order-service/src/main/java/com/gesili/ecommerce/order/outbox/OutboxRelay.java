package com.gesili.ecommerce.order.outbox;

import com.gesili.ecommerce.order.entity.OutboxEvent;
import com.gesili.ecommerce.order.repository.OutboxRepository;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transactional outbox relay (Phase 0 Note 1).
 *
 * <p>Polls unpublished rows with {@code WHERE published_at IS NULL ORDER BY id} — a simple
 * oldest-first scan with <strong>no high-water-mark cursor</strong> — publishes each to Kafka
 * <strong>keyed by aggregateId (orderId)</strong> so per-order ordering holds end to end, then
 * stamps {@code published_at}.
 *
 * <p>Wrapped in {@link SchedulerLock} so it is a <strong>single writer</strong>: two instances
 * never publish the same rows or interleave ordering. At-least-once publication is safe
 * because all consumers deduplicate on the envelope's {@code eventId}.
 *
 * <p>FLOW (the bridge from the database to Kafka):
 * <pre>
 *   OrderService.placeOrder() wrote a row into the `outbox` table (same TX as the order)
 *        │
 *        ▼  (this relay, every ~1s)
 *   read unpublished rows  →  kafka.send(topic, key=orderId, payload)  →  mark published_at
 *        │
 *        ▼
 *   Kafka order.events  →  inventory-service / notification-service consume it
 * </pre>
 */
@Slf4j
@Component
public class OutboxRelay {

    private static final int BATCH = 100;

    private final OutboxRepository outbox;
    private final KafkaTemplate<String, String> kafka;

    public OutboxRelay(OutboxRepository outbox, KafkaTemplate<String, String> kafka) {
        this.outbox = outbox;
        this.kafka = kafka;
    }

    @Scheduled(fixedDelayString = "${gesili.order.outbox.poll-ms:1000}")
    @SchedulerLock(name = "outbox-relay", lockAtLeastFor = "PT0.5S", lockAtMostFor = "PT1M")
    @Transactional
    public void publishBatch() {
        List<OutboxEvent> batch = outbox.findUnpublished(PageRequest.of(0, BATCH));
        if (batch.isEmpty()) {
            return;
        }
        for (OutboxEvent e : batch) {
            try {
                // Key by aggregateId (orderId) => all of an order's events share a partition.
                kafka.send(e.getTopic(), e.getAggregateId().toString(), e.getPayload()).get();
                e.setPublishedAt(Instant.now());
                outbox.save(e);
            } catch (Exception ex) {
                // Stop on first failure; unpublished rows are retried next poll (ordering preserved).
                log.error("Outbox publish failed for id {}: {}", e.getId(), ex.getMessage());
                break;
            }
        }
    }
}
