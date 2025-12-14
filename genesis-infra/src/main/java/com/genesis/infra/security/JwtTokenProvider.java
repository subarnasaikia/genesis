package com.genesis.infra.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Utility class for JWT token generation and validation.
 */
@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecurityProperties securityProperties;
    private final SecretKey signingKey;

    public JwtTokenProvider(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
        this.signingKey = Keys.hmacShaKeyFor(
                securityProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate an access token for the given user.
     *
     * @param userDetails the user details
     * @return the JWT access token
     */
    public String generateAccessToken(UserDetails userDetails) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() +
                securityProperties.getJwt().getAccessTokenExpiry().toMillis());

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Extract username from JWT token.
     *
     * @param token the JWT token
     * @return the username
     */
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    /**
     * Validate a JWT token.
     *
     * @param token the JWT token
     * @return true if valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SignatureException ex) {
            logger.error("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            logger.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            logger.error("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            logger.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * Get the access token expiry duration in milliseconds.
     */
    public long getAccessTokenExpiryMs() {
        return securityProperties.getJwt().getAccessTokenExpiry().toMillis();
    }

    /**
     * Get the refresh token expiry duration in milliseconds.
     */
    public long getRefreshTokenExpiryMs() {
        return securityProperties.getJwt().getRefreshTokenExpiry().toMillis();
    }
}
