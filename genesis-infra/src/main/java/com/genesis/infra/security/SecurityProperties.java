package com.genesis.infra.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;

/**
 * Configuration properties for security settings.
 */
@ConfigurationProperties(prefix = "genesis.security")
public class SecurityProperties {

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
         * Must be at least 256 bits for HS256 algorithm.
         */
        private String secret = "defaultSecretKeyThatShouldBeChangedInProduction12345";

        /**
         * Access token expiration time. Default: 15 minutes.
         */
        private Duration accessTokenExpiry = Duration.ofMinutes(15);

        /**
         * Refresh token expiration time. Default: 7 days.
         */
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
