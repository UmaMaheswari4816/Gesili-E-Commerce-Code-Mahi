package com.gesili.ecommerce.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Auth / User service.
 *
 * <p>Owns users, roles and refresh tokens. Issues RS256-signed JWT access tokens (short
 * lived) and refresh tokens (long lived, rotating, revocable), and publishes its public
 * keys at {@code /.well-known/jwks.json} so the gateway and every other resource server can
 * verify tokens without a shared secret.
 */
@SpringBootApplication
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
