package com.genesis.wsd.dto;

import com.genesis.wsd.entity.WsdSenseEntity;
import java.util.UUID;

public class WsdSenseDto {

    private UUID id;
    private UUID workspaceId;
    private String word;
    private String senseLabel;
    private String description;

    public static WsdSenseDto from(WsdSenseEntity e) {
        WsdSenseDto d = new WsdSenseDto();
        d.id = e.getId();
        d.workspaceId = e.getWorkspaceId();
        d.word = e.getWord();
        d.senseLabel = e.getSenseLabel();
        d.description = e.getDescription();
        return d;
    }

    public UUID getId() { return id; }
    public UUID getWorkspaceId() { return workspaceId; }
    public String getWord() { return word; }
    public String getSenseLabel() { return senseLabel; }
    public String getDescription() { return description; }
}
