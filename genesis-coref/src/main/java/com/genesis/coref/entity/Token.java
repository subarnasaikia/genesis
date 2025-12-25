package com.genesis.coref.entity;

import com.genesis.common.entity.BaseEntity;
import com.genesis.workspace.entity.Document;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Token entity representing a single token within a document.
 *
 * <p>
 * Tokens are the fundamental units for annotation. Each token has a position
 * (token_index) within its document and character offsets (start_offset, end_offset)
 * that map it to the original text. Tokens are numbered continuously across all
 * documents in a workspace.
 */
@Entity
@Table(name = "tokens", indexes = {
        @Index(name = "idx_tokens_document_id", columnList = "document_id"),
        @Index(name = "idx_tokens_token_index", columnList = "document_id, token_index")
})
public class Token extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    /**
     * Position of this token in the document's token sequence.
     * Tokens are numbered continuously across all documents in a workspace.
     */
    @Column(name = "token_index", nullable = false)
    private Integer tokenIndex;

    /**
     * The actual text content of this token.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    /**
     * Character offset where this token starts in the document text.
     */
    @Column(name = "start_offset", nullable = false)
    private Integer startOffset;

    /**
     * Character offset where this token ends in the document text.
     */
    @Column(name = "end_offset", nullable = false)
    private Integer endOffset;

    // Default constructor required by JPA
    public Token() {
    }

    // Getters and Setters

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public Integer getTokenIndex() {
        return tokenIndex;
    }

    public void setTokenIndex(Integer tokenIndex) {
        this.tokenIndex = tokenIndex;
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
}
