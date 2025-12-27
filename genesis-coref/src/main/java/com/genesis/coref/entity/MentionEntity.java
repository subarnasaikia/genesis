package com.genesis.coref.entity;

import com.genesis.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Represents a mention (span of tokens) that refers to an entity.
 *
 * <p>
 * Mentions are workspace-scoped and link to a specific document, sentence,
 * and token range. They can be assigned to a cluster.
 */
@Entity
@Table(name = "coref_mentions", indexes = {
        @Index(name = "idx_mention_workspace", columnList = "workspace_id"),
        @Index(name = "idx_mention_document", columnList = "document_id"),
        @Index(name = "idx_mention_cluster", columnList = "cluster_id"),
        @Index(name = "idx_mention_doc_sentence", columnList = "document_id, sentence_index")
})
public class MentionEntity extends BaseEntity {

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    /**
     * Cluster this mention belongs to (nullable if unassigned).
     */
    @Column(name = "cluster_id")
    private UUID clusterId;

    /**
     * 0-based sentence index within the document.
     */
    @Column(name = "sentence_index", nullable = false)
    private Integer sentenceIndex;

    /**
     * Start token index within the sentence (inclusive, 0-based).
     */
    @Column(name = "start_token_index", nullable = false)
    private Integer startTokenIndex;

    /**
     * End token index within the sentence (inclusive, 0-based).
     */
    @Column(name = "end_token_index", nullable = false)
    private Integer endTokenIndex;

    /**
     * Global start token index within the workspace (for cross-document reference).
     */
    @Column(name = "global_start_index")
    private Integer globalStartIndex;

    /**
     * Global end token index within the workspace.
     */
    @Column(name = "global_end_index")
    private Integer globalEndIndex;

    /**
     * Cached text of the mention (for display).
     */
    @Column(name = "text", length = 2000)
    private String text;

    /**
     * Mention type (optional, e.g., "PROPER", "NOMINAL", "PRONOMINAL").
     */
    @Column(name = "mention_type", length = 50)
    private String mentionType;

    // Getters and Setters

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(UUID workspaceId) {
        this.workspaceId = workspaceId;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public UUID getClusterId() {
        return clusterId;
    }

    public void setClusterId(UUID clusterId) {
        this.clusterId = clusterId;
    }

    public Integer getSentenceIndex() {
        return sentenceIndex;
    }

    public void setSentenceIndex(Integer sentenceIndex) {
        this.sentenceIndex = sentenceIndex;
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

    public Integer getGlobalStartIndex() {
        return globalStartIndex;
    }

    public void setGlobalStartIndex(Integer globalStartIndex) {
        this.globalStartIndex = globalStartIndex;
    }

    public Integer getGlobalEndIndex() {
        return globalEndIndex;
    }

    public void setGlobalEndIndex(Integer globalEndIndex) {
        this.globalEndIndex = globalEndIndex;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getMentionType() {
        return mentionType;
    }

    public void setMentionType(String mentionType) {
        this.mentionType = mentionType;
    }

    /**
     * Check if this is a single-token mention.
     */
    public boolean isSingleToken() {
        return startTokenIndex.equals(endTokenIndex);
    }
}
