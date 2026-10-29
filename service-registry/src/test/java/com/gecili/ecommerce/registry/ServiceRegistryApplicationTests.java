package com.gecili.ecommerce.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

/**
 * Boots the standalone Eureka server and verifies it is up and serving the registry API.
 *
 * <p>The config import is disabled here so the test does not require a running Config Server.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@TestPropertySource(
        properties = {
            "spring.config.import=",
            "eureka.client.register-with-eureka=false",
            "eureka.client.fetch-registry=false"
        })
class ServiceRegistryApplicationTests {

    @Autowired private TestRestTemplate rest;

    @Test
    void contextLoads() {
        // Fails if @EnableEurekaServer wiring is broken.
    }

    @Test
    void eurekaAppsEndpointResponds() {
        // Eureka exposes the registered-apps list at /eureka/apps (empty at startup).
        ResponseEntity<String> response = rest.getForEntity("/eureka/apps", String.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    void healthIsUp() {
        ResponseEntity<String> response = rest.getForEntity("/actuator/health", String.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).contains("UP");
    }
}
