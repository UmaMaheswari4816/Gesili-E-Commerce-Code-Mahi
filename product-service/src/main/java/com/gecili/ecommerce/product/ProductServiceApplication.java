package com.gecili.ecommerce.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Product catalog service.
 *
 * <p>Applies CQRS: PostgreSQL (via JPA) is the authoritative <strong>write model</strong>
 * for products and categories; Elasticsearch is the <strong>read model</strong> optimized
 * for full-text search and faceted filtering. Writes project asynchronously into ES.
 *
 * <p>JPA and Elasticsearch repositories live in separate packages so their
 * {@code @EnableXxxRepositories} scanning never overlaps.
 */
@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.gecili.ecommerce.product.repository.jpa")
@EnableElasticsearchRepositories(basePackages = "com.gecili.ecommerce.product.repository.search")
public class ProductServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }
}
