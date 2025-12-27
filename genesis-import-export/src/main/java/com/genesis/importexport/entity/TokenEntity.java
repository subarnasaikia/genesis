package com.genesis.importexport.entity;

import com.genesis.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Represents a token (word) in a document with CoNLL-2012 compatible fields.
 *
 * <p>
 * Each token stores its position within the document (sentence index, token
 * index),
 * character offsets for UI highlighting, and optional annotation fields for
 * POS, NER, etc.
 */
@Entity
@Table(name = "tokens", indexes = {
        @Index(name = "idx_token_document", columnList = "document_id"),
        @Index(name = "idx_token_document_sentence", columnList = "document_id, sentence_index"),
        @Index(name = "idx_token_global", columnList = "document_id, global_index")
})
public class TokenEntity extends BaseEntity {

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    /**
     * 0-based sentence number within the document.
     */
    @Column(name = "sentence_index", nullable = false)
    private Integer sentenceIndex;

    /**
     * 0-based token index within the sentence (matches CoNLL-2012 Word# column).
     */
    @Column(name = "token_index", nullable = false)
    private Integer tokenIndex;

    /**
     * 0-based token index across the entire document (for continuous numbering).
     */
    @Column(name = "global_index", nullable = false)
    private Integer globalIndex;

    /**
     * Raw token text (word form).
     */
    @Column(name = "form", nullable = false, length = 500)
    private String form;

    /**
     * Part-of-speech tag (optional, for annotation).
     */
    @Column(name = "pos", length = 20)
    private String pos;

    /**
     * Lemma/base form of the token (optional).
     */
    @Column(name = "lemma", length = 500)
    private String lemma;

    /**
     * Named entity tag (optional, for NER annotation).
     */
    @Column(name = "ner_tag", length = 100)
    private String nerTag;

    /**
     * Starting character offset in the original document text.
     */
    @Column(name = "start_offset", nullable = false)
    private Integer startOffset;

    /**
     * Ending character offset in the original document text (exclusive).
     */
    @Column(name = "end_offset", nullable = false)
    private Integer endOffset;

    // Getters and Setters

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
