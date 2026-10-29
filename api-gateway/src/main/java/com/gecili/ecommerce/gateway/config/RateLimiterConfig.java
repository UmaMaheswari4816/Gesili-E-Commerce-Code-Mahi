package com.gecili.ecommerce.gateway.config;

import java.security.Principal;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

/**
 * Key resolvers for the Redis rate limiter.
 *
 * <p>Authenticated requests are limited per user (the JWT subject); anonymous requests fall
 * back to the client IP. Per-user limiting is fairer than global limiting and prevents one
 * noisy user from starving others.
 */
@Configuration
public class RateLimiterConfig {

    /** Limit by authenticated principal, else by remote IP. */
    @Bean
    @Primary
    public KeyResolver userOrIpKeyResolver() {
        return exchange ->
                exchange
                        .getPrincipal()
                        .map(Principal::getName)
                        .switchIfEmpty(
                                Mono.fromSupplier(
                                        () -> {
                                            var addr = exchange.getRequest().getRemoteAddress();
                                            return addr != null ? addr.getAddress().getHostAddress() : "unknown";
                                        }));
    }
}
