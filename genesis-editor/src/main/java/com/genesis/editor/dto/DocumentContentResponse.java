package com.genesis.editor.dto;

import com.genesis.importexport.dto.SentenceDto;
import com.genesis.importexport.dto.TokenDto;
import java.util.List;
import java.util.UUID;

/**
 * Response containing document content with tokens for editor display.
 */
public class DocumentContentResponse {

    private UUID documentId;
    private String documentName;
    private Integer orderIndex;
    private List<SentenceDto> sentences;
    private List<TokenDto> tokens;
    private Integer totalSentences;
    private Integer totalTokens;
    private Integer globalTokenOffset; // Token offset from previous documents

    // Getters and Setters

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    public List<SentenceDto> getSentences() {
        return sentences;
    }

    public void setSentences(List<SentenceDto> sentences) {
        this.sentences = sentences;
    }

    public List<TokenDto> getTokens() {
        return tokens;
    }

    public void setTokens(List<TokenDto> tokens) {
        this.tokens = tokens;
    }

    public Integer getTotalSentences() {
        return totalSentences;
    }

    public void setTotalSentences(Integer totalSentences) {
        this.totalSentences = totalSentences;
    }

    public Integer getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(Integer totalTokens) {
        this.totalTokens = totalTokens;
    }

    public Integer getGlobalTokenOffset() {
        return globalTokenOffset;
    }

    public void setGlobalTokenOffset(Integer globalTokenOffset) {
        this.globalTokenOffset = globalTokenOffset;
    }
}
