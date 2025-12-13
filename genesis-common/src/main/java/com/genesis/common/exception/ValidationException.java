package com.genesis.common.exception;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Exception thrown when validation fails.
 *
 * <p>
 * This exception results in a 400 Bad Request HTTP response and can include
 * field-level validation errors.
 */
public class ValidationException extends GenesisException {

    private static final String ERROR_CODE = "VALIDATION_ERROR";

    private final Map<String, List<String>> fieldErrors;

    /**
     * Creates a ValidationException with a message.
     *
     * @param message the error message
     */
    public ValidationException(String message) {
        super(message, ERROR_CODE);
        this.fieldErrors = Collections.emptyMap();
    }

    /**
     * Creates a ValidationException with field-level errors.
     *
     * @param message     the error message
     * @param fieldErrors map of field names to list of error messages
     */
    public ValidationException(String message, Map<String, List<String>> fieldErrors) {
        super(message, ERROR_CODE);
        this.fieldErrors = fieldErrors != null ? fieldErrors : Collections.emptyMap();
    }

    /**
     * Creates a ValidationException for a single field error.
     *
     * @param field the field name
     * @param error the error message
     */
    public ValidationException(String field, String error) {
        super(String.format("Validation failed for field '%s': %s", field, error), ERROR_CODE);
        this.fieldErrors = Map.of(field, List.of(error));
    }

    public Map<String, List<String>> getFieldErrors() {
        return fieldErrors;
    }
}
