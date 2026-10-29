package com.gesili.ecommerce.inventory.entity;

import com.gesili.ecommerce.common.enums.ReservationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A held reservation line for an order.
 *
 * <p>{@code expiresAt} drives TTL expiry (default 20 minutes). {@code idempotencyKey} is the
 * order id + sku so re-delivery of the same reserve request does not double-hold. Releases
 * are guarded by {@code status}: only {@link ReservationStatus#HELD} &rarr;
 * {@link ReservationStatus#RELEASED} does work.
 */
@Entity
@Table(
        name = "reservations",
        indexes = {
            @Index(name = "idx_resv_order", columnList = "order_id"),
            @Index(name = "idx_resv_status_expiry", columnList = "status, expires_at"),
            @Index(name = "idx_resv_idem", columnList = "idempotency_key", unique = true)
        })
@Getter
@Setter
@NoArgsConstructor
public class Reservation {

    @Id private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ReservationStatus status;

    /** orderId + ":" + sku — prevents double-holding on duplicate reserve messages. */
    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static Reservation hold(
            UUID orderId, UUID productId, String sku, int quantity, Instant expiresAt) {
        Reservation r = new Reservation();
        r.id = UUID.randomUUID();
        r.orderId = orderId;
        r.productId = productId;
        r.sku = sku;
        r.quantity = quantity;
        r.status = ReservationStatus.HELD;
        r.idempotencyKey = orderId + ":" + sku;
        r.expiresAt = expiresAt;
        r.createdAt = Instant.now();
        return r;
    }
}
