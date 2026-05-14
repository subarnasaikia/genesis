package com.genesis.pos.entity;

import com.genesis.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * User-defined POS tag. The 17 Universal Dependencies tags remain hardcoded and
 * always available; rows here are additive customs, either workspace-local or
 * global. Validation in {@code PosTaggingService} composes universal + applicable
 * customs into an effective set per workspace.
 *
 * <p>Uniqueness is enforced by {@code idx_pos_tag_unique} on
 * {@code (tag, scope, workspace_id)}. For {@code GLOBAL} scope rows
 * {@code workspace_id} is null; Postgres treats two NULLs as distinct in a
 * unique index, so global uniqueness is enforced at the service layer.
 */
@Entity
@Table(name = "pos_tag_definitions", indexes = {
        @Index(name = "idx_pos_tag_workspace", columnList = "workspace_id"),
        @Index(name = "idx_pos_tag_scope", columnList = "scope"),
        @Index(name = "idx_pos_tag_unique", columnList = "tag, scope, workspace_id", unique = true)
})
public class PosTagDefinitionEntity extends BaseEntity {

    @Column(name = "tag", nullable = false, length = 20)
    private String tag;

    @Column(name = "description", length = 200)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 20)
    private PosTagScope scope;

    @Column(name = "workspace_id")
    private UUID workspaceId;

    @Column(name = "created_by_user_id", nullable = false, length = 100)
    private String createdByUserId;

    @Override
    public UUID getId() {
        return super.getId();
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public PosTagScope getScope() {
        return scope;
    }

    public void setScope(PosTagScope scope) {
        this.scope = scope;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(UUID workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(String createdByUserId) {
        this.createdByUserId = createdByUserId;
    }
}
