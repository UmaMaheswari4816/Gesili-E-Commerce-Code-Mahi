package com.gecili.ecommerce.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.gecili.ecommerce.auth.config.AuthProperties;
import com.gecili.ecommerce.auth.config.AuthProperties.Jwt;
import com.gecili.ecommerce.auth.entity.RefreshToken;
import com.gecili.ecommerce.auth.repository.RefreshTokenRepository;
import com.gecili.ecommerce.common.exception.ConflictException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for refresh-token rotation + reuse detection, using an in-memory stand-in for
 * the repository so the security-critical logic is verified without a database.
 */
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock private RefreshTokenRepository repository;
    private RefreshTokenService service;

    // Simple in-memory store keyed by tokenHash to emulate persistence.
    private final Map<String, RefreshToken> byHash = new HashMap<>();

    @BeforeEach
    void setUp() {
        AuthProperties props =
                new AuthProperties("http://localhost:9001", 900, 604800, new Jwt(null, null, "k"));
        service = new RefreshTokenService(repository, props);
    }

    private void wireStore() {
        when(repository.save(any(RefreshToken.class)))
                .thenAnswer(
                        inv -> {
                            RefreshToken t = inv.getArgument(0);
                            byHash.put(t.getTokenHash(), t);
                            return t;
                        });
        when(repository.findByTokenHash(anyString()))
                .thenAnswer(inv -> Optional.ofNullable(byHash.get(inv.<String>getArgument(0))));
    }

    @Test
    void issueThenRotate_producesNewTokenInSameFamily() {
        wireStore();
        UUID userId = UUID.randomUUID();

        var first = service.issueNewFamily(userId);
        var rotation = service.rotate(first.rawToken());

        assertThat(rotation.userId()).isEqualTo(userId);
        assertThat(rotation.issued().rawToken()).isNotEqualTo(first.rawToken());
        // same family carried forward
        assertThat(rotation.issued().record().getFamilyId())
                .isEqualTo(first.record().getFamilyId());
        // original marked used
        assertThat(first.record().isUsed()).isTrue();
    }

    @Test
    void reusingAnAlreadyRotatedToken_revokesTheFamily() {
        wireStore();
        when(repository.revokeFamily(any()))
                .thenAnswer(
                        inv -> {
                            UUID fam = inv.getArgument(0);
                            byHash.values().stream()
                                    .filter(t -> t.getFamilyId().equals(fam))
                                    .forEach(t -> t.setRevoked(true));
                            return 1;
                        });

        UUID userId = UUID.randomUUID();
        var first = service.issueNewFamily(userId);
        service.rotate(first.rawToken()); // first is now 'used'

        // Presenting the already-used first token again = theft => family revoked.
        assertThatThrownBy(() -> service.rotate(first.rawToken()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("reuse detected");
    }

    @Test
    void unknownToken_isRejected() {
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.rotate("bogus"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Invalid refresh token");
    }
}
