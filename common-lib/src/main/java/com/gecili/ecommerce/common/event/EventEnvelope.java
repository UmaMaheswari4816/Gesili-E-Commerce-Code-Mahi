package com.gecili.ecommerce.common.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

/**
 * Versioned envelope wrapping every domain event published to Kafka.
 *
 * <p>Separating a stable envelope from the versioned {@code payload} is what lets consumers
 * evolve safely: routing, deduplication and tracing read only the envelope, while the typed
 * body can change shape across {@link #version() versions} without breaking the transport.
 * This is, in effect, the discipline a schema registry (Avro/Protobuf) would enforce for
 * you — here we do it by convention in a shared library.
 *
 * <p>Fields:
 * <ul>
 *   <li>{@code eventId} — unique id; the <strong>idempotency/dedup key</strong> consumers
 *       use to ignore duplicates (Kafka delivers at-least-once).</li>
 *   <li>{@code eventType} — logical type, e.g. {@code "OrderCreated"}.</li>
 *   <li>{@code version} — schema version of {@code payload}.</li>
 *   <li>{@code occurredAt} — when the event happened (producer clock, UTC).</li>
 *   <li>{@code correlationId} — flows gateway &rarr; all services &rarr; traces, tying a
 *       request's events together across the saga.</li>
 *   <li>{@code source} — producing service name.</li>
 *   <li>{@code payload} — the typed domain event body.</li>
 * </ul>
 *
 * <p>Immutable record. Use {@link #of(String, int, String, String, Object)} to stamp
 * {@code eventId}/{@code occurredAt} automatically.
 *
 * @param <T> the payload type
 */
public record EventEnvelope<T>(
        UUID eventId,
        String eventType,
        int version,
        Instant occurredAt,
        String correlationId,
        String source,
        T payload) {

    /** Canonical constructor with null-guards on the required envelope fields. */
    @JsonCreator
    public EventEnvelope(
            @JsonProperty("eventId") UUID eventId,
            @JsonProperty("eventType") String eventType,
            @JsonProperty("version") int version,
            @JsonProperty("occurredAt") Instant occurredAt,
            @JsonProperty("correlationId") String correlationId,
            @JsonProperty("source") String source,
            @JsonProperty("payload") T payload) {
        this.eventId = eventId == null ? UUID.randomUUID() : eventId;
        this.eventType = requireNonBlank(eventType, "eventType");
        this.version = version <= 0 ? 1 : version;
        this.occurredAt = occurredAt == null ? Instant.now() : occurredAt;
        this.correlationId = correlationId;
        this.source = requireNonBlank(source, "source");
        this.payload = payload;
    }

    /**
     * Factory that stamps a fresh {@code eventId} and {@code occurredAt=now}.
     *
     * @param eventType     logical event type
     * @param version       payload schema version
     * @param correlationId request correlation id (may be null if unknown)
     * @param source        producing service name
     * @param payload       typed event body
     */
    public static <T> EventEnvelope<T> of(
            String eventType, int version, String correlationId, String source, T payload) {
        return new EventEnvelope<>(
                UUID.randomUUID(), eventType, version, Instant.now(), correlationId, source, payload);
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("EventEnvelope." + field + " must not be blank");
        }
        return value;
    }
}
