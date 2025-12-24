package com.genesis.workspace.entity;

import com.genesis.common.entity.BaseEntity;
import com.genesis.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Workspace member entity representing a user's membership in a workspace.
 *
 * <p>
 * Associates users with workspaces and defines their role (ADMIN, ANNOTATOR,
 * CURATOR).
 * Each user can only have one membership per workspace (enforced by unique
 * constraint).
 */
@Entity
@Table(name = "workspace_members", uniqueConstraints = {
        @UniqueConstraint(name = "uk_workspace_members_workspace_user", columnNames = { "workspace_id", "user_id" })
}, indexes = {
        @Index(name = "idx_workspace_members_workspace_id", columnList = "workspace_id"),
        @Index(name = "idx_workspace_members_user_id", columnList = "user_id"),
        @Index(name = "idx_workspace_members_role", columnList = "role")
})
public class WorkspaceMember extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole role;

    // Default constructor required by JPA
    public WorkspaceMember() {
    }

    // Getters and Setters

    public Workspace getWorkspace() {
        return workspace;
    }

    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public MemberRole getRole() {
        return role;
    }

    public void setRole(MemberRole role) {
        this.role = role;
    }
}
