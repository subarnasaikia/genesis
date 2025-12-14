package com.genesis.infra.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collections;

/**
 * TDD Tests for JwtTokenProvider.
 * Tests are written FIRST before implementation.
 */
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private SecurityProperties securityProperties;
    private UserDetails testUserDetails;

    @BeforeEach
    void setUp() {
        securityProperties = new SecurityProperties();
        securityProperties.getJwt()
                .setSecret("testSecretKeyForJwtTokenGenerationThatIsAtLeast256BitsLongForHS256Algorithm");
        securityProperties.getJwt().setAccessTokenExpiry(java.time.Duration.ofMinutes(15));
        securityProperties.getJwt().setRefreshTokenExpiry(java.time.Duration.ofDays(7));

        jwtTokenProvider = new JwtTokenProvider(securityProperties);

        testUserDetails = User.builder()
                .username("testuser")
                .password("password")
                .authorities(Collections.emptyList())
                .build();
    }

    @Test
    @DisplayName("generateAccessToken - valid user - returns token")
    void generateAccessToken_validUser_returnsToken() {
        String token = jwtTokenProvider.generateAccessToken(testUserDetails);

        assertThat(token).isNotNull();
        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts
    }

    @Test
    @DisplayName("validateToken - valid token - returns true")
    void validateToken_validToken_returnsTrue() {
        String token = jwtTokenProvider.generateAccessToken(testUserDetails);

        boolean isValid = jwtTokenProvider.validateToken(token);

        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("validateToken - invalid signature - returns false")
    void validateToken_invalidSignature_returnsFalse() {
        String token = jwtTokenProvider.generateAccessToken(testUserDetails);
        // Modify the signature part (last segment)
        String[] parts = token.split("\\.");
        String tamperedToken = parts[0] + "." + parts[1] + ".invalidSignature";

        boolean isValid = jwtTokenProvider.validateToken(tamperedToken);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("validateToken - malformed token - returns false")
    void validateToken_malformedToken_returnsFalse() {
        boolean isValid = jwtTokenProvider.validateToken("not.a.valid.jwt.token");

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("getUsernameFromToken - valid token - returns username")
    void getUsernameFromToken_validToken_returnsUsername() {
        String token = jwtTokenProvider.generateAccessToken(testUserDetails);

        String username = jwtTokenProvider.getUsernameFromToken(token);

        assertThat(username).isEqualTo("testuser");
    }

    @Test
    @DisplayName("generateAccessToken - contains expected claims")
    void generateAccessToken_containsExpectedClaims() {
        String token = jwtTokenProvider.generateAccessToken(testUserDetails);

        String username = jwtTokenProvider.getUsernameFromToken(token);
        assertThat(username).isEqualTo("testuser");
    }
}
