package com.genesis.workspace.entity;

import com.genesis.common.entity.BaseEntity;
import com.genesis.infra.storage.StoredFile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Document entity representing a file within a workspace.
 *
 * <p>
 * Documents are ordered by their orderIndex which is assigned based on upload
 * time.
 * All documents in a workspace are treated as one continuous annotation task
 * with
 * token indices spanning across document boundaries.
 */
@Entity
@Table(name = "documents", indexes = {
        @Index(name = "idx_documents_workspace_id", columnList = "workspace_id"),
        @Index(name = "idx_documents_status", columnList = "status"),
        @Index(name = "idx_documents_order_index", columnList = "workspace_id, order_index")
})
public class Document extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentStatus status = DocumentStatus.UPLOADED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stored_file_id")
    private StoredFile storedFile;

    /**
     * First token index in this document (for continuous tokenization).
     */
    @Column(name = "token_start_index")
    private Integer tokenStartIndex;

    /**
     * Last token index in this document (for continuous tokenization).
     */
    @Column(name = "token_end_index")
    private Integer tokenEndIndex;

    // Default constructor required by JPA
    public Document() {
    }

    // Getters and Setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public void setStatus(DocumentStatus status) {
        this.status = status;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    public StoredFile getStoredFile() {
        return storedFile;
    }

    public void setStoredFile(StoredFile storedFile) {
        this.storedFile = storedFile;
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
}
