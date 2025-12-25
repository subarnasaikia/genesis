package com.genesis.coref.entity;

import com.genesis.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Mention entity representing a single mention in a coreference cluster.
 *
 * <p>
 * A mention is a span of tokens that refers to an entity. Multiple mentions
 * that refer to the same entity are grouped into a cluster. The mention is
 * defined by token indices (start and end) rather than character offsets,
 * enabling consistent annotation across tokenized text.
 */
@Entity
@Table(name = "mentions", indexes = {
        @Index(name = "idx_mentions_cluster_id", columnList = "cluster_id"),
        @Index(name = "idx_mentions_token_indices", columnList = "token_start_index, token_end_index")
})
public class Mention extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cluster_id", nullable = false)
    private Cluster cluster;

    /**
     * Global token index where this mention starts (inclusive).
     * This is a continuous index across all documents in the workspace.
     */
    @Column(name = "token_start_index", nullable = false)
    private Integer tokenStartIndex;

    /**
     * Global token index where this mention ends (inclusive).
     * This is a continuous index across all documents in the workspace.
     */
    @Column(name = "token_end_index", nullable = false)
    private Integer tokenEndIndex;

    /**
     * Cached text of this mention for quick display.
     * Derived from the tokens in the range [tokenStartIndex, tokenEndIndex].
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    // Default constructor required by JPA
    public Mention() {
    }

    // Getters and Setters

    public Cluster getCluster() {
        return cluster;
    }

    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }

    public Integer getTokenStartIndex() {
        return tokenStartIndex;
    }

    public void setTokenStartIndex(Integer tokenStartIndex) {
        this.tokenStartIndex = tokenStartIndex;
    }

    public Integer getTokenEndIndex() {
        return tokenEndIndex;
    }

    public void setTokenEndIndex(Integer tokenEndIndex) {
        this.tokenEndIndex = tokenEndIndex;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
