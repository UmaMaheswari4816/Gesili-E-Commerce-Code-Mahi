package com.gesili.ecommerce.auth.service;

import com.gesili.ecommerce.auth.config.AuthProperties;
import com.gesili.ecommerce.auth.entity.RefreshToken;
import com.gesili.ecommerce.auth.repository.RefreshTokenRepository;
import com.gesili.ecommerce.common.exception.ConflictException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages opaque refresh tokens with <strong>rotation</strong> and
 * <strong>reuse detection</strong>.
 *
 * <ul>
 *   <li>Issue: generate a cryptographically random opaque token, store only its SHA-256
 *       hash, in a new family.</li>
 *   <li>Rotate: on a valid refresh, mark the old token {@code used} and issue a new token in
 *       the SAME family. The caller gets a brand-new refresh token each time.</li>
 *   <li>Reuse detection: if a token that is already {@code used} (or revoked) is presented,
 *       that means it was captured and replayed — revoke the ENTIRE family so neither the
 *       attacker nor the victim can continue, forcing a fresh login.</li>
 * </ul>
 *
 * Storing only the hash means a database leak does not expose usable tokens.
 */
@Slf4j
@Service
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final AuthProperties props;
    private final SecureRandom random = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository repository, AuthProperties props) {
        this.repository = repository;
        this.props = props;
    }

    /** The raw token returned to the client paired with its persisted record. */
    public record IssuedToken(String rawToken, RefreshToken record) {}

    @Transactional
    public IssuedToken issueNewFamily(UUID userId) {
        return persist(userId, UUID.randomUUID());
    }

    /**
     * Validate a presented refresh token and rotate it. Detects reuse and revokes the family
     * if the token was already consumed/revoked.
     *
     * @return the user id and the freshly issued token
     */
    @Transactional
    public RotationResult rotate(String presentedRawToken) {
        String hash = sha256(presentedRawToken);
        RefreshToken existing =
                repository
                        .findByTokenHash(hash)
                        .orElseThrow(() -> new ConflictException("Invalid refresh token"));

        if (existing.isUsed() || existing.isRevoked()) {
            // Reuse of an already-rotated/revoked token => theft. Revoke the whole family.
            int revoked = repository.revokeFamily(existing.getFamilyId());
            log.warn(
                    "Refresh token reuse detected for family {}; revoked {} tokens",
                    existing.getFamilyId(),
                    revoked);
            throw new ConflictException("Refresh token reuse detected; please log in again");
        }
        if (existing.getExpiresAt().isBefore(Instant.now())) {
            throw new ConflictException("Refresh token expired");
        }

        existing.setUsed(true);
        repository.save(existing);

        IssuedToken next = persist(existing.getUserId(), existing.getFamilyId());
        return new RotationResult(existing.getUserId(), next);
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        repository.revokeAllForUser(userId);
    }

    public record RotationResult(UUID userId, IssuedToken issued) {}

    private IssuedToken persist(UUID userId, UUID familyId) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expires = Instant.now().plusSeconds(props.refreshTtlSeconds());
        RefreshToken record = RefreshToken.issue(userId, familyId, sha256(raw), expires);
        repository.save(record);
        return new IssuedToken(raw, record);
    }

    private static String sha256(String value) {
        try {
            byte[] digest =
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
