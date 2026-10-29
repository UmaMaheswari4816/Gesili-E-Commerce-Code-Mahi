package com.gesili.ecommerce.order.entity;

import com.gesili.ecommerce.common.enums.OrderStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The Order aggregate root (named {@code OrderEntity} to avoid clashing with SQL's reserved
 * {@code order}). Owns the saga's state machine.
 *
 * <p>Lifecycle: {@code CREATED → PENDING_INVENTORY → PAYMENT_PENDING → CONFIRMED}, with
 * {@code CANCELLED} (optionally via {@code PAYMENT_TIMEOUT}) on failure. {@code paymentDeadline}
 * is set when entering {@code PAYMENT_PENDING}; the payment-timeout job cancels overdue orders.
 */
@Entity
@Table(
        name = "orders",
        indexes = {
            @Index(name = "idx_orders_user", columnList = "user_id"),
            @Index(name = "idx_orders_status_deadline", columnList = "status, payment_deadline")
        })
@Getter
@Setter
@NoArgsConstructor
public class OrderEntity {

    @Id private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    /** Deadline to complete payment once in PAYMENT_PENDING; null otherwise. */
    @Column(name = "payment_deadline")
    private Instant paymentDeadline;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version private long version;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id")
    private List<OrderItem> items = new ArrayList<>();

    public static OrderEntity create(UUID userId, BigDecimal total, String currency) {
        OrderEntity o = new OrderEntity();
        o.id = UUID.randomUUID();
        o.userId = userId;
        o.status = OrderStatus.CREATED;
        o.totalAmount = total;
        o.currency = currency;
        o.createdAt = Instant.now();
        o.updatedAt = o.createdAt;
        return o;
    }

    public void transitionTo(OrderStatus next) {
        this.status = next;
        this.updatedAt = Instant.now();
    }
}
