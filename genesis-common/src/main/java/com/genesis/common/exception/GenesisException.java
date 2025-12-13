package com.genesis.common.exception;

/**
 * Base exception class for all Genesis application exceptions.
 *
 * <p>
 * All custom exceptions should extend this class to ensure consistent
 * error handling across the application.
 */
public class GenesisException extends RuntimeException {

    private final String errorCode;

    /**
     * Creates a new GenesisException with message only.
     *
     * @param message the error message
     */
    public GenesisException(String message) {
        super(message);
        this.errorCode = "GENESIS_ERROR";
    }

    /**
     * Creates a new GenesisException with message and error code.
     *
     * @param message   the error message
     * @param errorCode the error code
     */
    public GenesisException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Creates a new GenesisException with message and cause.
     *
     * @param message the error message
     * @param cause   the underlying cause
     */
    public GenesisException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "GENESIS_ERROR";
    }

    /**
     * Creates a new GenesisException with message, error code, and cause.
     *
     * @param message   the error message
     * @param errorCode the error code
     * @param cause     the underlying cause
     */
    public GenesisException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
