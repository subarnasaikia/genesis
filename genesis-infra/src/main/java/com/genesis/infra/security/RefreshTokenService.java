package com.genesis.infra.security;

import com.genesis.user.entity.User;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for refresh token operations.
 */
@Service
@Transactional
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecurityProperties securityProperties;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
            SecurityProperties securityProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.securityProperties = securityProperties;
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
        refreshToken.setExpiryDate(Instant.now().plus(
                securityProperties.getJwt().getRefreshTokenExpiry()));
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
     */
    public void deleteExpiredTokens() {
        refreshTokenRepository.deleteAllExpiredTokens();
    }
}
