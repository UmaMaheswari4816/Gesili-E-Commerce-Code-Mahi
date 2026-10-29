package com.gecili.ecommerce.payment.entity;

import com.gecili.ecommerce.common.enums.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A payment for an order.
 *
 * <p>State is updated <strong>forward-only</strong>: a stale/out-of-order webhook can never
 * move the payment backward (enforced in the service via {@link PaymentStatus} ordering).
 * {@code providerRef} ties the payment to the provider record for reconciliation.
 */
@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payment_order", columnList = "order_id", unique = true),
    @Index(name = "idx_payment_provider_ref", columnList = "provider_ref")
})
@Getter
@Setter
@NoArgsConstructor
public class Payment {

    @Id private UUID id;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Column(nullable = false)
    private String provider;

    @Column(name = "provider_ref")
    private String providerRef;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PaymentStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version private long version;

    public static Payment initiate(UUID orderId, String provider, BigDecimal amount, String currency) {
        Payment p = new Payment();
        p.id = UUID.randomUUID();
        p.orderId = orderId;
        p.provider = provider;
        p.amount = amount;
        p.currency = currency;
        p.status = PaymentStatus.INITIATED;
        p.createdAt = Instant.now();
        p.updatedAt = p.createdAt;
        return p;
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }
}
