package com.genesis.user.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for refresh token operations.
 */
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token is required")
    private String refreshToken;

    // Getters and Setters

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
