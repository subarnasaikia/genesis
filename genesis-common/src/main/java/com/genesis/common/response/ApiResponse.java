package com.genesis.common.response;

import java.time.Instant;

/**
 * Generic wrapper for all API responses providing a consistent response format.
 *
 * <p>
 * All API endpoints should return responses wrapped in this class to ensure
 * consistent structure across the application.
 *
 * @param <T> the type of the response data
 */
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        Instant timestamp) {

    /**
     * Creates a successful response with data and default message.
     *
     * @param data the response data
     * @param <T>  the type of the response data
     * @return a successful ApiResponse
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "Success", data, Instant.now());
    }

    /**
     * Creates a successful response with data and custom message.
     *
     * @param data    the response data
     * @param message custom success message
     * @param <T>     the type of the response data
     * @return a successful ApiResponse
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, message, data, Instant.now());
    }

    /**
     * Creates an error response with a message and no data.
     *
     * @param message error message
     * @param <T>     the type of the response data (will be null)
     * @return an error ApiResponse
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, Instant.now());
    }
}
