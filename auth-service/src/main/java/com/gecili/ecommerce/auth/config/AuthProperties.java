package com.gecili.ecommerce.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized auth settings (issuer, token lifetimes, signing key location).
 *
 * <p>Bound from config (served by the Config Server). The private key is NEVER committed:
 * {@code jwt.private-key-location} points at a file provided via env/secret, and if it is
 * blank the service generates an ephemeral dev keypair at startup (logged clearly).
 */
@ConfigurationProperties(prefix = "gecili.auth")
public record AuthProperties(
        String issuer,
        long accessTtlSeconds,
        long refreshTtlSeconds,
        Jwt jwt) {

    public AuthProperties {
        if (issuer == null || issuer.isBlank()) {
            issuer = "http://localhost:9001";
        }
        if (accessTtlSeconds <= 0) {
            accessTtlSeconds = 900; // 15m
        }
        if (refreshTtlSeconds <= 0) {
            refreshTtlSeconds = 604800; // 7d
        }
        if (jwt == null) {
            jwt = new Jwt(null, null, null);
        }
    }

    /**
     * @param privateKeyLocation PEM (PKCS#8) private key file location (e.g.
     *     {@code file:./keys/jwt-private.pem}); blank = generate an ephemeral dev key.
     * @param publicKeyLocation  PEM public key file location; blank = derive from private or
     *     the ephemeral key.
     * @param activeKeyId        the {@code kid} stamped on newly-issued tokens.
     */
    public record Jwt(String privateKeyLocation, String publicKeyLocation, String activeKeyId) {}
}
