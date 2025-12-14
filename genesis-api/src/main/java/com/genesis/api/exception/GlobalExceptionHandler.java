package com.genesis.api.exception;

import com.genesis.common.exception.GenesisException;
import com.genesis.common.exception.ResourceNotFoundException;
import com.genesis.common.exception.UnauthorizedException;
import com.genesis.common.exception.ValidationException;
import com.genesis.common.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for all REST controllers.
 *
 * <p>
 * Provides consistent error response format across all endpoints.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

        private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

        /**
         * Handles ResourceNotFoundException (404).
         */
        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleResourceNotFound(
                        ResourceNotFoundException ex,
                        HttpServletRequest request) {
                log.warn("Resource not found: {}", ex.getMessage());
                ErrorResponse response = ErrorResponse.of(
                                ex.getErrorCode(),
                                ex.getMessage(),
                                request.getRequestURI(),
                                HttpStatus.NOT_FOUND.value());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        /**
         * Handles ValidationException (400).
         */
        @ExceptionHandler(ValidationException.class)
        public ResponseEntity<ErrorResponse> handleValidation(
                        ValidationException ex,
                        HttpServletRequest request) {
                log.warn("Validation error: {}", ex.getMessage());
                ErrorResponse response = ErrorResponse.withFieldErrors(
                                ex.getErrorCode(),
                                ex.getMessage(),
                                request.getRequestURI(),
                                HttpStatus.BAD_REQUEST.value(),
                                ex.getFieldErrors());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        /**
         * Handles UnauthorizedException (401/403).
         */
        @ExceptionHandler(UnauthorizedException.class)
        public ResponseEntity<ErrorResponse> handleUnauthorized(
                        UnauthorizedException ex,
                        HttpServletRequest request) {
                log.warn("Authorization error: {}", ex.getMessage());
                HttpStatus status = ex.isForbidden() ? HttpStatus.FORBIDDEN : HttpStatus.UNAUTHORIZED;
                ErrorResponse response = ErrorResponse.of(
                                ex.getErrorCode(),
                                ex.getMessage(),
                                request.getRequestURI(),
                                status.value());
                return ResponseEntity.status(status).body(response);
        }

        /**
         * Handles generic GenesisException (500).
         */
        @ExceptionHandler(GenesisException.class)
        public ResponseEntity<ErrorResponse> handleGenesisException(
                        GenesisException ex,
                        HttpServletRequest request) {
                log.error("Application error: {}", ex.getMessage(), ex);
                ErrorResponse response = ErrorResponse.of(
                                ex.getErrorCode(),
                                ex.getMessage(),
                                request.getRequestURI(),
                                HttpStatus.INTERNAL_SERVER_ERROR.value());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        /**
         * Handles Spring validation errors from @Valid annotations (400).
         */
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
                        MethodArgumentNotValidException ex,
                        HttpServletRequest request) {
                log.warn("Method argument validation failed");
                Map<String, List<String>> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                                .collect(Collectors.groupingBy(
                                                FieldError::getField,
                                                HashMap::new,
                                                Collectors.mapping(FieldError::getDefaultMessage,
                                                                Collectors.toList())));
                ErrorResponse response = ErrorResponse.withFieldErrors(
                                "VALIDATION_ERROR",
                                "Validation failed",
                                request.getRequestURI(),
                                HttpStatus.BAD_REQUEST.value(),
                                fieldErrors);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        /**
         * Handles malformed JSON (400).
         */
        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
                        HttpMessageNotReadableException ex,
                        HttpServletRequest request) {
                log.warn("Malformed request body: {}", ex.getMessage());
                ErrorResponse response = ErrorResponse.of(
                                "MALFORMED_REQUEST",
                                "Malformed request body",
                                request.getRequestURI(),
                                HttpStatus.BAD_REQUEST.value());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        /**
         * Handles bad credentials (401).
         */
        @ExceptionHandler(BadCredentialsException.class)
        public ResponseEntity<ErrorResponse> handleBadCredentials(
                        BadCredentialsException ex,
                        HttpServletRequest request) {
                log.warn("Authentication failed: {}", ex.getMessage());
                ErrorResponse response = ErrorResponse.of(
                                "INVALID_CREDENTIALS",
                                "Invalid username or password",
                                request.getRequestURI(),
                                HttpStatus.UNAUTHORIZED.value());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        /**
         * Handles disabled account (403).
         */
        @ExceptionHandler(DisabledException.class)
        public ResponseEntity<ErrorResponse> handleDisabled(
                        DisabledException ex,
                        HttpServletRequest request) {
                log.warn("Account disabled: {}", ex.getMessage());
                ErrorResponse response = ErrorResponse.of(
                                "ACCOUNT_DISABLED",
                                "Your account has been disabled",
                                request.getRequestURI(),
                                HttpStatus.FORBIDDEN.value());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        /**
         * Handles locked account (403).
         */
        @ExceptionHandler(LockedException.class)
        public ResponseEntity<ErrorResponse> handleLocked(
                        LockedException ex,
                        HttpServletRequest request) {
                log.warn("Account locked: {}", ex.getMessage());
                ErrorResponse response = ErrorResponse.of(
                                "ACCOUNT_LOCKED",
                                "Your account has been locked",
                                request.getRequestURI(),
                                HttpStatus.FORBIDDEN.value());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        /**
         * Handles illegal argument exceptions (400).
         */
        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ErrorResponse> handleIllegalArgument(
                        IllegalArgumentException ex,
                        HttpServletRequest request) {
                log.warn("Bad request: {}", ex.getMessage());
                ErrorResponse response = ErrorResponse.of(
                                "BAD_REQUEST",
                                ex.getMessage(),
                                request.getRequestURI(),
                                HttpStatus.BAD_REQUEST.value());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        /**
         * Fallback handler for unexpected exceptions (500).
         */
        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGenericException(
                        Exception ex,
                        HttpServletRequest request) {
                log.error("Unexpected error occurred: {}", ex.getMessage(), ex);
                ErrorResponse response = ErrorResponse.of(
                                "INTERNAL_ERROR",
                                "An unexpected error occurred",
                                request.getRequestURI(),
                                HttpStatus.INTERNAL_SERVER_ERROR.value());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
}
