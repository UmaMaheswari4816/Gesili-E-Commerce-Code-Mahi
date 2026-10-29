package com.gesili.ecommerce.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Stock for one SKU.
 *
 * <p>{@code available} = sellable now; {@code reserved} = held by open reservations. The
 * invariant is {@code available >= 0} always — that is how we prevent overselling.
 *
 * <p>The {@code @Version} column gives <strong>optimistic locking</strong>: if two
 * transactions read the same row and both try to commit, the second fails with an
 * {@code OptimisticLockException} and retries. Combined with the pessimistic
 * {@code SELECT ... FOR UPDATE} in the repository (used on the hot reserve path), concurrent
 * buyers cannot drive availability below zero.
 */
@Entity
@Table(name = "stock_items")
@Getter
@Setter
@NoArgsConstructor
public class StockItem {

    @Id private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(nullable = false)
    private int available;

    @Column(nullable = false)
    private int reserved;

    @Version private long version;

    public static StockItem create(UUID productId, String sku, int available) {
        StockItem s = new StockItem();
        s.id = UUID.randomUUID();
        s.productId = productId;
        s.sku = sku;
        s.available = available;
        s.reserved = 0;
        return s;
    }

    /** Hold {@code qty}: available--, reserved++. Caller must ensure enough is available. */
    public void hold(int qty) {
        if (qty > available) {
            throw new IllegalStateException("Not enough stock to hold");
        }
        available -= qty;
        reserved += qty;
    }

    /** Release {@code qty} back to available: reserved--, available++. */
    public void release(int qty) {
        int toRelease = Math.min(qty, reserved);
        reserved -= toRelease;
        available += toRelease;
    }

    /** Commit {@code qty}: reserved-- (stock permanently gone; available already reduced). */
    public void commit(int qty) {
        reserved -= Math.min(qty, reserved);
    }
}
