package com.genesis.importexport.entity;

import com.genesis.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Represents a sentence within a document.
 *
 * <p>
 * Sentences group tokens and track their position in the document.
 * Used for CoNLL-2012 export where sentences are separated by blank lines.
 */
@Entity
@Table(name = "sentences", indexes = {
        @Index(name = "idx_sentence_document", columnList = "document_id"),
        @Index(name = "idx_sentence_document_index", columnList = "document_id, sentence_index")
})
public class SentenceEntity extends BaseEntity {

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    /**
     * 0-based sentence index within the document.
     */
    @Column(name = "sentence_index", nullable = false)
    private Integer sentenceIndex;

    /**
     * Full text of the sentence.
     */
    @Column(name = "text", columnDefinition = "TEXT")
    private String text;

    /**
     * Starting character offset in the original document.
     */
    @Column(name = "start_offset", nullable = false)
    private Integer startOffset;

    /**
     * Ending character offset in the original document (exclusive).
     */
    @Column(name = "end_offset", nullable = false)
    private Integer endOffset;

    /**
     * Number of tokens in this sentence.
     */
    @Column(name = "token_count")
    private Integer tokenCount;

    // Getters and Setters

    @Override
    public UUID getId() {
        return super.getId();
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

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
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

    public Integer getTokenCount() {
        return tokenCount;
    }

    public void setTokenCount(Integer tokenCount) {
        this.tokenCount = tokenCount;
    }
}
