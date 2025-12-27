package com.genesis.importexport.dto;

import java.util.UUID;

/**
 * Data transfer object for a token.
 */
public class TokenDto {

    private UUID id;
    private UUID documentId;
    private Integer sentenceIndex;
    private Integer tokenIndex;
    private Integer globalIndex;
    private String form;
    private String pos;
    private String lemma;
    private String nerTag;
    private Integer startOffset;
    private Integer endOffset;

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public Integer getSentenceIndex() {
        return sentenceIndex;
    }

    public void setSentenceIndex(Integer sentenceIndex) {
        this.sentenceIndex = sentenceIndex;
    }

    public Integer getTokenIndex() {
        return tokenIndex;
    }

    public void setTokenIndex(Integer tokenIndex) {
        this.tokenIndex = tokenIndex;
    }

    public Integer getGlobalIndex() {
        return globalIndex;
    }

    public void setGlobalIndex(Integer globalIndex) {
        this.globalIndex = globalIndex;
    }

    public String getForm() {
        return form;
    }

    public void setForm(String form) {
        this.form = form;
    }

    public String getPos() {
        return pos;
    }

    public void setPos(String pos) {
        this.pos = pos;
    }

    public String getLemma() {
        return lemma;
    }

    public void setLemma(String lemma) {
        this.lemma = lemma;
    }

    public String getNerTag() {
        return nerTag;
    }

    public void setNerTag(String nerTag) {
        this.nerTag = nerTag;
    }

    public Integer getStartOffset() {
        return startOffset;
    }

    public void setStartOffset(Integer startOffset) {
        this.startOffset = startOffset;
    }

    public Integer getEndOffset() {
        return endOffset;
    }

    public void setEndOffset(Integer endOffset) {
        this.endOffset = endOffset;
    }
}
