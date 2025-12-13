package com.genesis.common.exception;

/**
 * Exception thrown when authentication or authorization fails.
 *
 * <p>
 * This exception results in a 401 Unauthorized or 403 Forbidden HTTP response.
 */
public class UnauthorizedException extends GenesisException {

    private static final String ERROR_CODE = "UNAUTHORIZED";

    private final boolean isForbidden;

    /**
     * Creates an UnauthorizedException with a message (401 Unauthorized).
     *
     * @param message the error message
     */
    public UnauthorizedException(String message) {
        super(message, ERROR_CODE);
        this.isForbidden = false;
    }

    /**
     * Creates an UnauthorizedException with a message and forbidden flag.
     *
     * @param message     the error message
     * @param isForbidden true for 403 Forbidden, false for 401 Unauthorized
     */
    public UnauthorizedException(String message, boolean isForbidden) {
        super(message, isForbidden ? "FORBIDDEN" : ERROR_CODE);
        this.isForbidden = isForbidden;
    }

    /**
     * Returns true if this represents a 403 Forbidden error.
     *
     * @return true for forbidden, false for unauthorized
     */
    public boolean isForbidden() {
        return isForbidden;
    }
}
