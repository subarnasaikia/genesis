package com.genesis.workspace.dto;

import com.genesis.workspace.entity.MemberRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for adding a member to a workspace.
 */
public class AddMemberRequest {

    @NotNull(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotNull(message = "Role is required")
    private MemberRole role;

    // Default constructor
    public AddMemberRequest() {
    }

    public AddMemberRequest(String email, MemberRole role) {
        this.email = email;
        this.role = role;
    }

    // Getters and Setters

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public MemberRole getRole() {
        return role;
    }

    public void setRole(MemberRole role) {
        this.role = role;
    }
}
