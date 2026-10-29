package com.gesili.ecommerce.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A line item within an order. Price is snapshotted at order time. */
@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
public class OrderItem {

    @Id private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    public static OrderItem of(UUID orderId, UUID productId, String sku, int qty, BigDecimal unitPrice) {
        OrderItem i = new OrderItem();
        i.id = UUID.randomUUID();
        i.orderId = orderId;
        i.productId = productId;
        i.sku = sku;
        i.quantity = qty;
        i.unitPrice = unitPrice;
        return i;
    }
}
