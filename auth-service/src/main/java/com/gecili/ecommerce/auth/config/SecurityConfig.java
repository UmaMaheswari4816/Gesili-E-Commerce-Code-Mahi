package com.gecili.ecommerce.auth.config;

import com.gecili.ecommerce.auth.security.RsaKeyProvider;
import java.util.Collection;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security for auth-service. It is both the token <em>issuer</em> and a resource server that
 * validates its own access tokens on protected endpoints.
 *
 * <p>Public (no token): register, login, refresh, JWKS, health, and Swagger. Everything else
 * requires a valid RS256 token verified against the local public key. Roles in the
 * {@code roles} claim are mapped to {@code ROLE_*} authorities.
 */
@Configuration
public class SecurityConfig {

    private static final String[] PUBLIC = {
        "/api/v1/auth/register",
        "/api/v1/auth/login",
        "/api/v1/auth/refresh",
        "/.well-known/jwks.json",
        "/actuator/health/**",
        "/actuator/info",
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(HttpMethod.GET, "/.well-known/jwks.json")
                                        .permitAll()
                                        .requestMatchers(PUBLIC)
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(authoritiesConverter())));
        return http.build();
    }

    /** Verify access tokens locally with the service's own public key. */
    @Bean
    public JwtDecoder jwtDecoder(RsaKeyProvider keys) {
        return NimbusJwtDecoder.withPublicKey(keys.publicKey()).build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private JwtAuthenticationConverter authoritiesConverter() {
        JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(
                jwt -> {
                    Collection<GrantedAuthority> authorities = scopes.convert(jwt);
                    List<String> roles = jwt.getClaimAsStringList("roles");
                    if (roles != null) {
                        roles.forEach(r -> authorities.add(new SimpleGrantedAuthority("ROLE_" + r)));
                    }
                    return authorities;
                });
        return converter;
    }
}
