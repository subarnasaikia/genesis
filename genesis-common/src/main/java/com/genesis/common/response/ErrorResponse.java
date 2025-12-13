package com.genesis.common.response;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Standardized error response format for API errors.
 *
 * <p>
 * Used by the global exception handler to ensure consistent error responses.
 */
public record ErrorResponse(
        boolean success,
        String error,
        String message,
        String path,
        int status,
        Map<String, List<String>> fieldErrors,
        Instant timestamp) {

    /**
     * Creates a simple error response without field errors.
     *
     * @param error   error code/type
     * @param message error message
     * @param path    request path
     * @param status  HTTP status code
     * @return an ErrorResponse
     */
    public static ErrorResponse of(String error, String message, String path, int status) {
        return new ErrorResponse(false, error, message, path, status, null, Instant.now());
    }

    /**
     * Creates an error response with field-level validation errors.
     *
     * @param error       error code/type
     * @param message     error message
     * @param path        request path
     * @param status      HTTP status code
     * @param fieldErrors map of field names to error messages
     * @return an ErrorResponse
     */
    public static ErrorResponse withFieldErrors(
            String error,
            String message,
            String path,
            int status,
            Map<String, List<String>> fieldErrors) {
        return new ErrorResponse(false, error, message, path, status, fieldErrors, Instant.now());
    }
}
