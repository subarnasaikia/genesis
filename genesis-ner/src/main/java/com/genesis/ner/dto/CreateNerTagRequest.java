package com.genesis.ner.dto;

import com.genesis.ner.entity.NerTagScope;
import java.util.UUID;

public class CreateNerTagRequest {

    private String tag;
    private String description;
    private NerTagScope scope;
    private UUID workspaceId;

    public CreateNerTagRequest() {
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
}
