package com.gecili.ecommerce.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.gecili.ecommerce.gateway.filter.CorrelationIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Boots the gateway context. Redis, Eureka and the config server are not required here:
 * discovery is disabled, the config import is cleared, and the rate limiter's Redis template
 * is only exercised at request time (not at startup). This verifies the route table,
 * security, filters and fallback beans all wire up.
 */
@SpringBootTest
@TestPropertySource(
        properties = {
            "spring.config.import=",
            "eureka.client.enabled=false",
            "spring.cloud.gateway.discovery.locator.enabled=false",
            // Use a dummy local JWKS so the resource server bean initializes without network.
            "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:9001/.well-known/jwks.json"
        })
class ApiGatewayApplicationTests {

    @Autowired private CorrelationIdFilter correlationIdFilter;

    @Test
    void contextLoads() {
        // Fails if routes/security/filters are misconfigured at startup.
    }

    @Test
    void correlationIdFilterIsRegistered() {
        assertThat(correlationIdFilter).isNotNull();
    }
}
