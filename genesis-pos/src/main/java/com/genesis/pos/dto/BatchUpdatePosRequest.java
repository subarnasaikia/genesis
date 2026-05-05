package com.genesis.pos.dto;

import java.util.List;
import java.util.UUID;

public class BatchUpdatePosRequest {

    private List<Item> updates;

    public BatchUpdatePosRequest() {
    }

    public List<Item> getUpdates() {
        return updates;
    }

    public void setUpdates(List<Item> updates) {
        this.updates = updates;
    }

    public static class Item {
        private UUID tokenId;
        private String pos;

        public Item() {
        }

        public Item(UUID tokenId, String pos) {
            this.tokenId = tokenId;
            this.pos = pos;
        }

        public UUID getTokenId() {
            return tokenId;
        }

        public void setTokenId(UUID tokenId) {
            this.tokenId = tokenId;
        }

        public String getPos() {
            return pos;
        }

        public void setPos(String pos) {
            this.pos = pos;
        }
    }
}
