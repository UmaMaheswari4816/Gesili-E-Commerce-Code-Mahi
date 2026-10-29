package com.gesili.ecommerce.auth.controller;

import com.gesili.ecommerce.auth.security.RsaKeyProvider;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * JWKS (JSON Web Key Set) endpoint.
 *
 * <p>Publishes the auth-service's PUBLIC key(s) at {@code /.well-known/jwks.json} so the
 * gateway and every resource server can verify RS256 tokens without a shared secret. During
 * key rotation this set contains BOTH the outgoing and incoming public keys (each with its
 * own {@code kid}) for an overlap window, so tokens signed by the old key keep validating
 * until they expire. Here we serve the single active key; adding the overlapping key is a
 * matter of listing a second {@link RSAKey}.
 */
@RestController
@Tag(name = "JWKS", description = "Public keys for JWT verification")
public class JwksController {

    private final RsaKeyProvider keys;

    public JwksController(RsaKeyProvider keys) {
        this.keys = keys;
    }

    @Operation(summary = "Get the JSON Web Key Set (public keys)")
    @GetMapping(value = "/.well-known/jwks.json", produces = "application/json")
    public Map<String, Object> jwks() {
        RSAKey jwk =
                new RSAKey.Builder(keys.publicKey())
                        .keyUse(KeyUse.SIGNATURE)
                        .keyID(keys.keyId())
                        .algorithm(com.nimbusds.jose.JWSAlgorithm.RS256)
                        .build();
        return new JWKSet(jwk).toJSONObject();
    }
}
