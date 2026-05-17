package com.genesis.ner.dto;

public class UpdateNerAnnotationRequest {

    private Integer startTokenIndex;
    private Integer endTokenIndex;
    private String label;

    public UpdateNerAnnotationRequest() {
    }

    public Integer getStartTokenIndex() {
        return startTokenIndex;
    }

    public void setStartTokenIndex(Integer startTokenIndex) {
        this.startTokenIndex = startTokenIndex;
    }

    public Integer getEndTokenIndex() {
        return endTokenIndex;
    }

    public void setEndTokenIndex(Integer endTokenIndex) {
        this.endTokenIndex = endTokenIndex;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
