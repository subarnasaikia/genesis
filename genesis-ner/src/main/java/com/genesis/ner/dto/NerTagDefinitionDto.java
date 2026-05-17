package com.genesis.ner.dto;

import com.genesis.ner.entity.NerTagDefinitionEntity;
import com.genesis.ner.entity.NerTagScope;
import java.util.UUID;

public class NerTagDefinitionDto {

    private UUID id;
    private String tag;
    private String description;
    private NerTagScope scope;
    private UUID workspaceId;
    private boolean builtin;

    public NerTagDefinitionDto() {
    }

    public NerTagDefinitionDto(UUID id, String tag, String description, NerTagScope scope,
            UUID workspaceId, boolean builtin) {
        this.id = id;
        this.tag = tag;
        this.description = description;
        this.scope = scope;
        this.workspaceId = workspaceId;
        this.builtin = builtin;
    }

    public static NerTagDefinitionDto from(NerTagDefinitionEntity e) {
        if (e == null) {
            return null;
        }
        return new NerTagDefinitionDto(e.getId(), e.getTag(), e.getDescription(),
                e.getScope(), e.getWorkspaceId(), false);
    }

    public static NerTagDefinitionDto builtin(String tag, String description) {
        return new NerTagDefinitionDto(null, tag, description, NerTagScope.GLOBAL, null, true);
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

    public NerTagScope getScope() {
        return scope;
    }

    public void setScope(NerTagScope scope) {
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
