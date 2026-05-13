package com.genesis.wsd.dto;

public class CreateSenseRequest {

    private String word;
    private String senseLabel;
    private String description;

    public String getWord() { return word; }
    public void setWord(String word) { this.word = word; }

    public String getSenseLabel() { return senseLabel; }
    public void setSenseLabel(String senseLabel) { this.senseLabel = senseLabel; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
