package com.gesili.ecommerce.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A refresh token record supporting <strong>rotation</strong> and <strong>reuse
 * detection</strong>.
 *
 * <p>Only the SHA-256 hash of the token value is stored, never the raw token (same principle
 * as password hashing: a DB leak must not hand out usable tokens).
 *
 * <p>Tokens are grouped into a <em>family</em> ({@code familyId}): each refresh issues a new
 * token in the same family and marks the previous one {@code used}. If a token that has
 * already been {@code used} is presented again, that signals theft — the whole family is
 * revoked.
 */
@Entity
@Table(
        name = "refresh_tokens",
        indexes = {
            @Index(name = "idx_refresh_token_hash", columnList = "token_hash"),
            @Index(name = "idx_refresh_family", columnList = "family_id")
        })
@Getter
@Setter
@NoArgsConstructor
public class RefreshToken {

    @Id private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** SHA-256 hash of the opaque refresh token value. */
    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    /** All rotated tokens descended from one login share a family id. */
    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    /** True once this token has been exchanged (rotated). Reuse after this = theft. */
    @Column(nullable = false)
    private boolean used = false;

    /** True if explicitly revoked (logout, or reuse-detection family revocation). */
    @Column(nullable = false)
    private boolean revoked = false;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static RefreshToken issue(
            UUID userId, UUID familyId, String tokenHash, Instant expiresAt) {
        RefreshToken t = new RefreshToken();
        t.id = UUID.randomUUID();
        t.userId = userId;
        t.familyId = familyId;
        t.tokenHash = tokenHash;
        t.expiresAt = expiresAt;
        t.createdAt = Instant.now();
        return t;
    }

    public boolean isActive() {
        return !used && !revoked && expiresAt.isAfter(Instant.now());
    }
}
