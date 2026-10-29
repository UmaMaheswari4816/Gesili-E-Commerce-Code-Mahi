package com.gesili.ecommerce.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import com.gesili.ecommerce.auth.dto.AuthDtos.LoginRequest;
import com.gesili.ecommerce.auth.dto.AuthDtos.RefreshRequest;
import com.gesili.ecommerce.auth.dto.AuthDtos.RegisterRequest;
import com.gesili.ecommerce.auth.dto.AuthDtos.TokenResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * End-to-end auth flow against a real PostgreSQL (Testcontainers), exercising Flyway
 * migration, registration, login, token rotation on refresh, and the authenticated /me
 * endpoint with a Bearer token verified by the service's own public key.
 */
@Testcontainers
@SpringBootTest(webEnvironment = RANDOM_PORT)
class AuthFlowIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("gesili_auth");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.config.import", () -> "");           // no config server in test
        registry.add("eureka.client.enabled", () -> "false");      // no registry in test
    }

    @Autowired private TestRestTemplate rest;

    @Test
    void register_login_refresh_me_happyPath() {
        // Register
        ResponseEntity<Void> reg =
                rest.postForEntity(
                        "/api/v1/auth/register",
                        new RegisterRequest("jane@example.com", "S3curePass!", "Jane"),
                        Void.class);
        assertThat(reg.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Login
        ResponseEntity<TokenResponse> login =
                rest.postForEntity(
                        "/api/v1/auth/login",
                        new LoginRequest("jane@example.com", "S3curePass!"),
                        TokenResponse.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        TokenResponse tokens = login.getBody();
        assertThat(tokens).isNotNull();
        assertThat(tokens.accessToken()).isNotBlank();
        assertThat(tokens.refreshToken()).isNotBlank();

        // Call /me with the access token
        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(tokens.accessToken());
        ResponseEntity<String> me =
                rest.exchange("/api/v1/auth/me", org.springframework.http.HttpMethod.GET,
                        new HttpEntity<>(authHeaders), String.class);
        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(me.getBody()).contains("jane@example.com");

        // Refresh rotates the refresh token
        ResponseEntity<TokenResponse> refreshed =
                rest.postForEntity(
                        "/api/v1/auth/refresh",
                        new RefreshRequest(tokens.refreshToken()),
                        TokenResponse.class);
        assertThat(refreshed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refreshed.getBody().refreshToken()).isNotEqualTo(tokens.refreshToken());

        // Reusing the OLD refresh token now fails (reuse detection)
        ResponseEntity<String> reuse =
                rest.postForEntity(
                        "/api/v1/auth/refresh",
                        new RefreshRequest(tokens.refreshToken()),
                        String.class);
        assertThat(reuse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void jwks_isPublic_andReturnsKey() {
        ResponseEntity<String> jwks = rest.getForEntity("/.well-known/jwks.json", String.class);
        assertThat(jwks.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(jwks.getBody()).contains("\"kty\":\"RSA\"");
    }

    @Test
    void me_withoutToken_is401() {
        ResponseEntity<String> me = rest.getForEntity("/api/v1/auth/me", String.class);
        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
