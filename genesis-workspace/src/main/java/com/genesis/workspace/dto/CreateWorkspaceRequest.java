package com.genesis.workspace.dto;

import com.genesis.workspace.entity.AnnotationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new workspace.
 */
public class CreateWorkspaceRequest {

    @NotBlank(message = "Workspace name is required")
    @Size(max = 100, message = "Workspace name must not exceed 100 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @NotNull(message = "Annotation type is required")
    private AnnotationType annotationType;

    // Default constructor
    public CreateWorkspaceRequest() {
    }

    public CreateWorkspaceRequest(String name, String description, AnnotationType annotationType) {
        this.name = name;
        this.description = description;
        this.annotationType = annotationType;
    }

    // Getters and Setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public AnnotationType getAnnotationType() {
        return annotationType;
    }

    public void setAnnotationType(AnnotationType annotationType) {
        this.annotationType = annotationType;
    }
}
