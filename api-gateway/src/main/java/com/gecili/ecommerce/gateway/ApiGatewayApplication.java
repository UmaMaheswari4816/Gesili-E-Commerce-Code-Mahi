package com.gecili.ecommerce.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway — the single public entry point.
 *
 * <p>Responsibilities: route to services by discovery ({@code lb://}), validate JWTs at the
 * edge against the auth-service JWKS, enforce Redis-backed rate limits, apply CORS, propagate
 * a correlation id, provide circuit-breaker fallbacks, and serve an aggregated Swagger UI.
 *
 * <p>The payment webhook route is deliberately public and has NO body-reading filters (see
 * {@code RouteConfig}) so provider signature verification over the raw body still works.
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
