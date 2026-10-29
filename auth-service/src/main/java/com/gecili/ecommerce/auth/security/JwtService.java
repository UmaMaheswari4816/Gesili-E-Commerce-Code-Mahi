package com.gecili.ecommerce.auth.security;

import com.gecili.ecommerce.auth.config.AuthProperties;
import com.gecili.ecommerce.auth.entity.UserAccount;
import io.jsonwebtoken.Jwts;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Issues RS256-signed JWT access tokens.
 *
 * <p>Claims: {@code sub} (user id), {@code email}, {@code roles}, {@code iss}, {@code iat},
 * {@code exp}, and a {@code kid} header so verifiers can select the right JWKS key during
 * key rotation. Access tokens are short-lived; the refresh token (opaque, rotating) is
 * handled by {@code RefreshTokenService}.
 */
@Service
public class JwtService {

    private final RsaKeyProvider keys;
    private final AuthProperties props;

    public JwtService(RsaKeyProvider keys, AuthProperties props) {
        this.keys = keys;
        this.props = props;
    }

    public String issueAccessToken(UserAccount user) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(props.accessTtlSeconds());
        List<String> roles = user.getRoles().stream().map(Enum::name).toList();

        return Jwts.builder()
                .header()
                .keyId(keys.keyId())
                .and()
                .issuer(props.issuer())
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(keys.privateKey(), Jwts.SIG.RS256)
                .compact();
    }

    public long accessTtlSeconds() {
        return props.accessTtlSeconds();
    }
}
