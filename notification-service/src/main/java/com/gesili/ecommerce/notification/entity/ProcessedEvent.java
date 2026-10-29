package com.gesili.ecommerce.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Dedup record so a redelivered event never sends a duplicate notification. */
@Entity
@Table(name = "processed_events")
@Getter
@Setter
@NoArgsConstructor
public class ProcessedEvent {

    @Id private UUID eventId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    public static ProcessedEvent of(UUID eventId) {
        ProcessedEvent p = new ProcessedEvent();
        p.eventId = eventId;
        p.processedAt = Instant.now();
        return p;
    }
}
