package com.genesis.common.exception;

/**
 * Exception thrown when a requested resource is not found.
 *
 * <p>
 * This exception results in a 404 Not Found HTTP response.
 */
public class ResourceNotFoundException extends GenesisException {

    private static final String ERROR_CODE = "RESOURCE_NOT_FOUND";

    /**
     * Creates a ResourceNotFoundException with a message.
     *
     * @param message the error message
     */
    public ResourceNotFoundException(String message) {
        super(message, ERROR_CODE);
    }

    /**
     * Creates a ResourceNotFoundException for a specific resource type and
     * identifier.
     *
     * @param resourceType the type of resource (e.g., "User", "Document")
     * @param identifier   the resource identifier
     */
    public ResourceNotFoundException(String resourceType, Object identifier) {
        super(String.format("%s not found with identifier: %s", resourceType, identifier), ERROR_CODE);
    }
}
