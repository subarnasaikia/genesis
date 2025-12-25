package com.genesis.coref.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for creating a new mention.
 */
public class CreateMentionRequest {

    @NotNull(message = "Token start index is required")
    private Integer tokenStartIndex;

    @NotNull(message = "Token end index is required")
    private Integer tokenEndIndex;

    // Default constructor
    public CreateMentionRequest() {
    }

    // Getters and Setters

    public Integer getTokenStartIndex() {
        return tokenStartIndex;
    }

    public void setTokenStartIndex(Integer tokenStartIndex) {
        this.tokenStartIndex = tokenStartIndex;
    }

    public Integer getTokenEndIndex() {
        return tokenEndIndex;
    }

    public void setTokenEndIndex(Integer tokenEndIndex) {
        this.tokenEndIndex = tokenEndIndex;
    }
}
