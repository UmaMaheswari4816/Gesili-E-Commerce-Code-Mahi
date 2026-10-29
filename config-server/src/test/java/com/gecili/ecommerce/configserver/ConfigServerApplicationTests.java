package com.gecili.ecommerce.configserver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

/**
 * Boots the Config Server with the native backend pointed at a tiny test config-repo and
 * verifies it serves a service's merged configuration.
 *
 * <p>This exercises the real wiring: native backend, profile resolution, and the
 * {@code /{application}/{profile}} endpoint — without needing Kafka or Eureka.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@TestPropertySource(
        properties = {
            "spring.profiles.active=native",
            "spring.cloud.config.server.native.search-locations=classpath:/test-config-repo",
            "eureka.client.enabled=false",
            "spring.cloud.bus.enabled=false"
        })
class ConfigServerApplicationTests {

    @Autowired private TestRestTemplate rest;

    @Test
    void contextLoads() {
        // If the context fails to start (bad backend wiring), this test fails.
    }

    @Test
    void servesMergedConfigForAService() {
        // {application}/{profile} — demo-service with the 'dev' profile.
        ResponseEntity<String> response = rest.getForEntity("/demo-service/dev", String.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).contains("demo-service");
        // value from application.yml (shared default)
        assertThat(response.getBody()).contains("shared-default-value");
        // value from demo-service-dev.yml (per-service, per-profile override)
        assertThat(response.getBody()).contains("dev-only-value");
    }
}
