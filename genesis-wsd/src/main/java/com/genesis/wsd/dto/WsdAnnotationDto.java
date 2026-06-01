package com.genesis.wsd.dto;

import com.genesis.wsd.entity.WsdAnnotationEntity;
import java.time.Instant;
import java.util.UUID;

public class WsdAnnotationDto {

    private UUID id;
    private UUID tokenId;
    private UUID senseId;
    private String senseLabel;
    private String annotatorId;
    private UUID workspaceId;
    private Instant timestamp;

    public static WsdAnnotationDto from(WsdAnnotationEntity e) {
        WsdAnnotationDto d = new WsdAnnotationDto();
        d.id = e.getId();
        d.tokenId = e.getTokenId();
        d.senseId = e.getSenseId();
        d.annotatorId = e.getAnnotatorId();
        d.workspaceId = e.getWorkspaceId();
        d.timestamp = e.getTimestamp();
        return d;
    }

    /** As {@link #from(WsdAnnotationEntity)} but also carries the resolved sense label. */
    public static WsdAnnotationDto from(WsdAnnotationEntity e, String senseLabel) {
        WsdAnnotationDto d = from(e);
        d.senseLabel = senseLabel;
        return d;
    }

    public UUID getId() { return id; }
    public UUID getTokenId() { return tokenId; }
    public UUID getSenseId() { return senseId; }
    public String getSenseLabel() { return senseLabel; }
    public String getAnnotatorId() { return annotatorId; }
    public UUID getWorkspaceId() { return workspaceId; }
    public Instant getTimestamp() { return timestamp; }
}
