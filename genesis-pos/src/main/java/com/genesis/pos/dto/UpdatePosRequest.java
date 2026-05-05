package com.genesis.pos.dto;

public class UpdatePosRequest {

    private String pos;

    public UpdatePosRequest() {
    }

    public UpdatePosRequest(String pos) {
        this.pos = pos;
    }

    public String getPos() {
        return pos;
    }

    public void setPos(String pos) {
        this.pos = pos;
    }
}
