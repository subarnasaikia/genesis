package com.genesis.infra.security;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for RefreshToken entity.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Find a refresh token by its token string.
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Find a non-revoked, non-expired token by its token string.
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.token = :token AND rt.revoked = false")
    Optional<RefreshToken> findByTokenAndNotRevoked(@Param("token") String token);

    /**
     * Revoke all refresh tokens for a user.
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user.id = :userId")
    void revokeAllUserTokens(@Param("userId") UUID userId);

    /**
     * Delete all expired tokens.
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiryDate < CURRENT_TIMESTAMP")
    void deleteAllExpiredTokens();
}
