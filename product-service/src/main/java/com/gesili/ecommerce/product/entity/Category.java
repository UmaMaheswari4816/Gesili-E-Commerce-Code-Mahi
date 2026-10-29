package com.gesili.ecommerce.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A product category (write model). Owned by product-service. */
@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
public class Category {

    @Id private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    private String description;

    public static Category create(String name, String slug, String description) {
        Category c = new Category();
        c.id = UUID.randomUUID();
        c.name = name;
        c.slug = slug;
        c.description = description;
        return c;
    }
}
