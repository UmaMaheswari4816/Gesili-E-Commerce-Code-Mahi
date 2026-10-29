package com.gesili.ecommerce.auth.repository;

import com.gesili.ecommerce.auth.entity.RefreshToken;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Revoke every token in a family — the reuse-detection "theft response". */
    @Modifying
    @Query("update RefreshToken t set t.revoked = true where t.familyId = :familyId")
    int revokeFamily(@Param("familyId") UUID familyId);

    @Modifying
    @Query("update RefreshToken t set t.revoked = true where t.userId = :userId and t.revoked = false")
    int revokeAllForUser(@Param("userId") UUID userId);
}
