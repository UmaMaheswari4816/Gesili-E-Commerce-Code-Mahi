package com.gesili.ecommerce.notification.service;

import com.gesili.ecommerce.notification.entity.ProcessedEvent;
import com.gesili.ecommerce.notification.repository.ProcessedEventRepository;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Dedupe so a redelivered event never sends a duplicate notification. */
@Service
public class EventDeduplicator {

    private final ProcessedEventRepository processed;

    public EventDeduplicator(ProcessedEventRepository processed) {
        this.processed = processed;
    }

    @Transactional
    public boolean markIfNew(UUID eventId) {
        if (processed.existsById(eventId)) {
            return false;
        }
        try {
            processed.saveAndFlush(ProcessedEvent.of(eventId));
            return true;
        } catch (DataIntegrityViolationException raceLost) {
            return false;
        }
    }
}
