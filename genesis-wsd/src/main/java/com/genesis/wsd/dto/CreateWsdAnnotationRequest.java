package com.genesis.wsd.dto;

import java.util.UUID;

public class CreateWsdAnnotationRequest {

    private UUID tokenId;
    private UUID senseId;

    public UUID getTokenId() { return tokenId; }
    public void setTokenId(UUID tokenId) { this.tokenId = tokenId; }

    public UUID getSenseId() { return senseId; }
    public void setSenseId(UUID senseId) { this.senseId = senseId; }
}
