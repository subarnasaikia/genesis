package com.genesis.pos.dto;

import com.genesis.pos.entity.PosAnnotationEntity;
import java.time.Instant;
import java.util.UUID;

public class PosAnnotationDto {

    private UUID id;
    private UUID tokenId;
    private String annotatorId;
    private String posTag;
    private Instant timestamp;

    public PosAnnotationDto() {
    }

    public PosAnnotationDto(UUID id, UUID tokenId, String annotatorId, String posTag, Instant timestamp) {
        this.id = id;
        this.tokenId = tokenId;
        this.annotatorId = annotatorId;
        this.posTag = posTag;
        this.timestamp = timestamp;
    }

    public static PosAnnotationDto from(PosAnnotationEntity e) {
        if (e == null) {
            return null;
        }
        return new PosAnnotationDto(
                e.getId(),
                e.getTokenId(),
                e.getAnnotatorId(),
                e.getPosTag(),
                e.getTimestamp());
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTokenId() {
        return tokenId;
    }

    public void setTokenId(UUID tokenId) {
        this.tokenId = tokenId;
    }

    public String getAnnotatorId() {
        return annotatorId;
    }

    public void setAnnotatorId(String annotatorId) {
        this.annotatorId = annotatorId;
    }

    public String getPosTag() {
        return posTag;
    }

    public void setPosTag(String posTag) {
        this.posTag = posTag;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
