package com.gecili.ecommerce.auth.security;

import com.gecili.ecommerce.auth.config.AuthProperties;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

/**
 * Supplies the RSA keypair used to sign (private) and verify (public) RS256 JWTs.
 *
 * <p>Loads a PEM (PKCS#8 private / X.509 public) keypair from the configured locations. If
 * none is configured — the default for local dev — it generates an <strong>ephemeral</strong>
 * 2048-bit keypair at startup and logs a prominent warning. The private key is never
 * committed: real deployments inject it via a file/secret (see
 * {@code scripts/gen-jwt-keys} and the README).
 *
 * <p>Asymmetric (RS256) signing means other services verify tokens with only the PUBLIC key
 * (served via JWKS); compromising a resource server cannot mint tokens.
 */
@Slf4j
@Component
public class RsaKeyProvider {

    private final RSAPublicKey publicKey;
    private final RSAPrivateKey privateKey;
    private final String keyId;

    public RsaKeyProvider(AuthProperties props) throws Exception {
        String priv = props.jwt().privateKeyLocation();
        String pub = props.jwt().publicKeyLocation();
        String kid = props.jwt().activeKeyId();

        if (priv == null || priv.isBlank()) {
            log.warn(
                    "==== No JWT private key configured — generating an EPHEMERAL dev keypair. "
                            + "Tokens will be invalid after restart. Configure gecili.auth.jwt.private-key-location "
                            + "for any shared/persistent environment. ====");
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            KeyPair pair = gen.generateKeyPair();
            this.publicKey = (RSAPublicKey) pair.getPublic();
            this.privateKey = (RSAPrivateKey) pair.getPrivate();
            this.keyId = (kid == null || kid.isBlank()) ? "dev-ephemeral" : kid;
        } else {
            DefaultResourceLoader loader = new DefaultResourceLoader();
            this.privateKey = readPrivate(loader.getResource(priv));
            this.publicKey = readPublic(loader.getResource(pub));
            this.keyId = (kid == null || kid.isBlank()) ? "key-1" : kid;
            log.info("Loaded RSA JWT keypair with kid '{}'", this.keyId);
        }
    }

    public RSAPublicKey publicKey() {
        return publicKey;
    }

    public RSAPrivateKey privateKey() {
        return privateKey;
    }

    public String keyId() {
        return keyId;
    }

    private static RSAPrivateKey readPrivate(Resource res) throws Exception {
        String pem = asString(res)
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(pem);
        return (RSAPrivateKey)
                KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
    }

    private static RSAPublicKey readPublic(Resource res) throws Exception {
        String pem = asString(res)
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(pem);
        return (RSAPublicKey)
                KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
    }

    private static String asString(Resource res) throws Exception {
        try (var in = res.getInputStream()) {
            return StreamUtils.copyToString(in, java.nio.charset.StandardCharsets.UTF_8);
        }
    }
}
