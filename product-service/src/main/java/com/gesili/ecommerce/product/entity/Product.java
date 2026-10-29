package com.gesili.ecommerce.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * A catalog product (write model / source of truth in PostgreSQL).
 *
 * <p>Note: product-service owns descriptive catalog data only. It does NOT own stock levels
 * — inventory-service is authoritative for availability. Price is stored in minor-unit
 * decimal with a currency code.
 */
@Entity
@Table(
        name = "products",
        indexes = {
            @Index(name = "idx_product_sku", columnList = "sku", unique = true),
            @Index(name = "idx_product_category", columnList = "category_id")
        })
@Getter
@Setter
@NoArgsConstructor
public class Product {

    @Id private UUID id;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version private long version;

    public static Product create(
            String sku, String name, String description, BigDecimal price, String currency, UUID categoryId) {
        Product p = new Product();
        p.id = UUID.randomUUID();
        p.sku = sku;
        p.name = name;
        p.description = description;
        p.price = price;
        p.currency = currency;
        p.categoryId = categoryId;
        p.active = true;
        p.createdAt = Instant.now();
        p.updatedAt = p.createdAt;
        return p;
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }
}
