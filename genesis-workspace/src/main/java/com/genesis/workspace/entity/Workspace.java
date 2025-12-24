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

/**
 * Workspace entity representing a project/workspace for annotation.
 *
 * <p>
 * A workspace is the main container for annotation work. It contains
 * multiple documents that are treated as one continuous annotation task.
 * Each workspace has an owner and can have multiple members with different
 * roles.
 */
@Entity
@Table(name = "workspaces", indexes = {
        @Index(name = "idx_workspaces_owner_id", columnList = "owner_id"),
        @Index(name = "idx_workspaces_status", columnList = "status")
})
public class Workspace extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "annotation_type", nullable = false, length = 20)
    private AnnotationType annotationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkspaceStatus status = WorkspaceStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    // Default constructor required by JPA
    public Workspace() {
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

    public WorkspaceStatus getStatus() {
        return status;
    }

    public void setStatus(WorkspaceStatus status) {
        this.status = status;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }
}
