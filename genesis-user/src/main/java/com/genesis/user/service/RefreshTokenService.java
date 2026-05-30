package com.genesis.user.service;

import com.genesis.user.entity.RefreshToken;
import com.genesis.user.entity.User;
import com.genesis.user.repository.RefreshTokenRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for refresh token operations.
 */
@Service
@Transactional
public class RefreshTokenService {

    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final Duration refreshTokenExpiry;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
            @Value("${genesis.security.jwt.refresh-token-expiry:7d}") Duration refreshTokenExpiry) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenExpiry = refreshTokenExpiry;
    }

    /**
     * Create a new refresh token for a user.
     *
     * @param user the user
     * @return the created refresh token
     */
    public RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(Instant.now().plus(refreshTokenExpiry));
        refreshToken.setRevoked(false);

        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Find a refresh token by its token string.
     */
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByTokenAndNotRevoked(token);
    }

    /**
     * Verify a refresh token is valid (not expired).
     *
     * @param token the refresh token
     * @return the token if valid
     * @throws IllegalArgumentException if token is expired
     */
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isExpired()) {
            refreshTokenRepository.delete(token);
            throw new IllegalArgumentException("Refresh token was expired. Please login again.");
        }
        return token;
    }

    /**
     * Rotate a refresh token: revoke the old one and issue a fresh one for
     * the same user. Used by the refresh-on-use flow that defeats indefinite
     * replay of a stolen refresh token (SECURITY_AUDIT MEDIUM-1).
     */
    public RefreshToken rotateToken(RefreshToken oldToken) {
        oldToken.setRevoked(true);
        refreshTokenRepository.save(oldToken);
        return createRefreshToken(oldToken.getUser());
    }

    /**
     * Revoke a refresh token.
     */
    public void revokeToken(String token) {
        refreshTokenRepository.findByToken(token)
                .ifPresent(rt -> {
                    rt.setRevoked(true);
                    refreshTokenRepository.save(rt);
                });
    }

    /**
     * Revoke all tokens for a user.
     */
    public void revokeAllUserTokens(UUID userId) {
        refreshTokenRepository.revokeAllUserTokens(userId);
    }

    /**
     * Clean up expired tokens.
     *
     * <p>Scheduled to run daily at 03:00 UTC. Also callable directly for
     * manual invocation or testing.
     */
    @Scheduled(cron = "0 0 3 * * *", zone = "UTC")
    public void deleteExpiredTokens() {
        refreshTokenRepository.deleteAllExpiredTokens();
        logger.info("Deleted expired refresh tokens (scheduled cleanup at 03:00 UTC)");
    }
}
