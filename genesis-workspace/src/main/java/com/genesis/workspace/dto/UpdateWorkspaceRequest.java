package com.genesis.workspace.dto;

import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating a workspace.
 */
public class UpdateWorkspaceRequest {

    @Size(max = 100, message = "Workspace name must not exceed 100 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    // Default constructor
    public UpdateWorkspaceRequest() {
    }

    public UpdateWorkspaceRequest(String name, String description) {
        this.name = name;
        this.description = description;
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
}
