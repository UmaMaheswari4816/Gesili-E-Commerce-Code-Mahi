package com.gecili.ecommerce.order.service;

import com.gecili.ecommerce.order.entity.ProcessedEvent;
import com.gecili.ecommerce.order.repository.ProcessedEventRepository;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumer-side idempotency. Records each handled {@code eventId}; a duplicate (Kafka is
 * at-least-once) is detected and skipped. Runs in its own transaction so the marker commits
 * with the handling work.
 */
@Service
public class EventDeduplicator {

    private final ProcessedEventRepository processed;

    public EventDeduplicator(ProcessedEventRepository processed) {
        this.processed = processed;
    }

    /**
     * @return true if this eventId is new (proceed), false if already processed (skip)
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public boolean markIfNew(UUID eventId) {
        if (processed.existsById(eventId)) {
            return false;
        }
        try {
            processed.saveAndFlush(ProcessedEvent.of(eventId));
            return true;
        } catch (DataIntegrityViolationException raceLost) {
            // Another consumer inserted concurrently — treat as already processed.
            return false;
        }
    }
}
