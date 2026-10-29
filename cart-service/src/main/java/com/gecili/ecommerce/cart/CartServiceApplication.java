package com.gecili.ecommerce.cart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Cart service. Carts live in Redis keyed by user id, with a TTL so abandoned carts expire
 * automatically. The cart is a <em>draft</em>: it snapshots the price at add-time, but the
 * order service re-validates price and stock at checkout (prices drift; stock is owned by
 * inventory-service).
 *
 * <p>{@link EnableFeignClients} enables the discovery-aware product-service client used to
 * demonstrate Resilience4j (Phase 9).
 */
@SpringBootApplication
@EnableFeignClients
public class CartServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CartServiceApplication.class, args);
    }
}
