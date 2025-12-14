package com.genesis.user.entity;

/**
 * Authentication provider enum.
 * Supports local authentication and OAuth providers.
 */
public enum AuthProvider {
    /**
     * Local authentication using username/email and password.
     */
    LOCAL,

    /**
     * Google OAuth authentication (for future use).
     */
    GOOGLE
}
