package com.genesis.workspace.dto;

import com.genesis.workspace.entity.MemberRole;
import java.util.UUID;

/**
 * Response DTO for workspace member data.
 */
public class MemberResponse {

    private UUID userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private MemberRole role;

    // Default constructor
    public MemberResponse() {
    }

    // Getters and Setters

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public MemberRole getRole() {
        return role;
    }

    public void setRole(MemberRole role) {
        this.role = role;
    }
}
