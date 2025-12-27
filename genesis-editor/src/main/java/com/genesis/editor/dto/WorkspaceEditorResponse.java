package com.genesis.editor.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response for opening a workspace in the editor.
 * Contains session info, documents list, and aggregate stats.
 */
public class WorkspaceEditorResponse {

    private UUID workspaceId;
    private String workspaceName;
    private EditorSessionResponse session;
    private List<EditorDocumentInfo> documents;
    private Integer totalDocuments;
    private Integer totalSentences;
    private Integer totalTokens;
    private Integer tokenizedDocuments;
    private Instant lastAccessedAt;

    // Getters and Setters

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(UUID workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getWorkspaceName() {
        return workspaceName;
    }

    public void setWorkspaceName(String workspaceName) {
        this.workspaceName = workspaceName;
    }

    public EditorSessionResponse getSession() {
        return session;
    }

    public void setSession(EditorSessionResponse session) {
        this.session = session;
    }

    public List<EditorDocumentInfo> getDocuments() {
        return documents;
    }

    public void setDocuments(List<EditorDocumentInfo> documents) {
        this.documents = documents;
    }

    public Integer getTotalDocuments() {
        return totalDocuments;
    }

    public void setTotalDocuments(Integer totalDocuments) {
        this.totalDocuments = totalDocuments;
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

    public Integer getTokenizedDocuments() {
        return tokenizedDocuments;
    }

    public void setTokenizedDocuments(Integer tokenizedDocuments) {
        this.tokenizedDocuments = tokenizedDocuments;
    }

    public Instant getLastAccessedAt() {
        return lastAccessedAt;
    }

    public void setLastAccessedAt(Instant lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }
}
