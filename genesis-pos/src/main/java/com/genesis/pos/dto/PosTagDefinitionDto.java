package com.genesis.pos.dto;

import com.genesis.pos.entity.PosTagDefinitionEntity;
import com.genesis.pos.entity.PosTagScope;
import java.util.UUID;

public class PosTagDefinitionDto {

    private UUID id;
    private String tag;
    private String description;
    private PosTagScope scope;
    private UUID workspaceId;
    private boolean builtin;

    public PosTagDefinitionDto() {
    }

    public PosTagDefinitionDto(UUID id, String tag, String description, PosTagScope scope,
            UUID workspaceId, boolean builtin) {
        this.id = id;
        this.tag = tag;
        this.description = description;
        this.scope = scope;
        this.workspaceId = workspaceId;
        this.builtin = builtin;
    }

    public static PosTagDefinitionDto from(PosTagDefinitionEntity e) {
        if (e == null) {
            return null;
        }
        return new PosTagDefinitionDto(e.getId(), e.getTag(), e.getDescription(),
                e.getScope(), e.getWorkspaceId(), false);
    }

    public static PosTagDefinitionDto builtin(String tag) {
        return new PosTagDefinitionDto(null, tag, null, PosTagScope.GLOBAL, null, true);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public boolean isBuiltin() {
        return builtin;
    }

    public void setBuiltin(boolean builtin) {
        this.builtin = builtin;
    }
}
