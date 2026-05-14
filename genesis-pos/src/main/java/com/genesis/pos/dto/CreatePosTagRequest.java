package com.genesis.pos.dto;

import com.genesis.pos.entity.PosTagScope;
import java.util.UUID;

public class CreatePosTagRequest {

    private String tag;
    private String description;
    private PosTagScope scope;
    private UUID workspaceId;

    public CreatePosTagRequest() {
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
}
