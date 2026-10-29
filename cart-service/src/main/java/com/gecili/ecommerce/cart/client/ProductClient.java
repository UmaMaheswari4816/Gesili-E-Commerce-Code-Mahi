package com.gecili.ecommerce.cart.client;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * OpenFeign client to product-service, resolved via Eureka ({@code lb://product-service}).
 * Demonstrates discovery-aware synchronous inter-service calls. Wrapped with Resilience4j in
 * {@link com.gecili.ecommerce.cart.service.ProductLookupService}.
 */
@FeignClient(name = "product-service", path = "/api/v1/products")
public interface ProductClient {

    @GetMapping("/{id}")
    ProductView getById(@PathVariable UUID id);

    /** Minimal projection of the product-service response we actually need. */
    record ProductView(UUID id, String sku, String name, BigDecimal price, String currency, boolean active) {}
}
