package com.genesis.workspace.dto;

import com.genesis.workspace.entity.MemberRole;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request DTO for adding a member to a workspace.
 */
public class AddMemberRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull(message = "Role is required")
    private MemberRole role;

    // Default constructor
    public AddMemberRequest() {
    }

    public AddMemberRequest(UUID userId, MemberRole role) {
        this.userId = userId;
        this.role = role;
    }

    // Getters and Setters

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public MemberRole getRole() {
        return role;
    }

    public void setRole(MemberRole role) {
        this.role = role;
    }
}
