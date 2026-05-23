package com.genesis.infra.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import java.time.Duration;

/**
 * Configuration properties for security settings.
 */
@Validated
@ConfigurationProperties(prefix = "genesis.security")
public class SecurityProperties {

    @Valid
    private final Jwt jwt = new Jwt();

    public Jwt getJwt() {
        return jwt;
    }

    /**
     * JWT-related configuration properties.
     */
    public static class Jwt {
        /**
         * Secret key for signing JWT tokens.
         * Must be at least 256 bits (32 ASCII chars) for HS256.
         * No default — JWT_SECRET env var is required at boot.
         */
        @NotBlank(message = "JWT_SECRET must be set (genesis.security.jwt.secret)")
        @Size(min = 32, message = "JWT_SECRET must be at least 256 bits (32 ASCII chars) for HS256")
        private String secret;

        /**
         * Access token expiration time. Default: 15 minutes.
         */
        @NotNull
        private Duration accessTokenExpiry = Duration.ofMinutes(15);

        /**
         * Refresh token expiration time. Default: 7 days.
         */
        @NotNull
        private Duration refreshTokenExpiry = Duration.ofDays(7);

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public Duration getAccessTokenExpiry() {
            return accessTokenExpiry;
        }

        public void setAccessTokenExpiry(Duration accessTokenExpiry) {
            this.accessTokenExpiry = accessTokenExpiry;
        }

        public Duration getRefreshTokenExpiry() {
            return refreshTokenExpiry;
        }

        public void setRefreshTokenExpiry(Duration refreshTokenExpiry) {
            this.refreshTokenExpiry = refreshTokenExpiry;
        }
    }
}
