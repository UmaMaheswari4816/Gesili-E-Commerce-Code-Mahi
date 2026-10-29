package com.gesili.ecommerce.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Edge security. The gateway validates JWTs here (defence in depth — each resource server
 * validates again). Public routes: auth endpoints, the payment webhook, JWKS proxy, Swagger
 * and health. Everything else requires a valid token verified against the auth-service JWKS
 * (configured via {@code spring.security.oauth2.resourceserver.jwt.jwk-set-uri}).
 */
@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    private static final String[] PUBLIC = {
        "/api/v1/auth/register",
        "/api/v1/auth/login",
        "/api/v1/auth/refresh",
        // Payment provider webhooks: public, signature-verified downstream over the RAW body.
        "/api/v1/payments/webhook/**",
        // Aggregated docs + each service's proxied docs.
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/v3/api-docs/**",
        "/*/v3/api-docs/**",
        "/actuator/health/**"
    };

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(
                        ex ->
                                ex.pathMatchers(HttpMethod.OPTIONS)
                                        .permitAll()
                                        .pathMatchers(PUBLIC)
                                        .permitAll()
                                        .anyExchange()
                                        .authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> {}));
        return http.build();
    }
}
