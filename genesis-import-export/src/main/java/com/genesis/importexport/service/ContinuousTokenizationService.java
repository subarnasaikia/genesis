package com.genesis.importexport.service;

import com.genesis.workspace.dto.DocumentResponse;
import com.genesis.workspace.service.DocumentService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Service for handling continuous tokenization across documents in a workspace.
 *
 * <p>
 * This service ensures that all documents in a workspace have continuous
 * token numbering, where each document's tokens follow the previous document.
 */
@Service
public class ContinuousTokenizationService {

    private final DocumentService documentService;

    public ContinuousTokenizationService(DocumentService documentService) {
        this.documentService = documentService;
    }

    /**
     * Token data for a single document.
     */
    public static class DocumentTokens {
        private UUID documentId;
        private String documentName;
        private int startIndex;
        private int endIndex;
        private List<String> tokens;

        public DocumentTokens() {
            this.tokens = new ArrayList<>();
        }

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

        public int getStartIndex() {
            return startIndex;
        }

        public void setStartIndex(int startIndex) {
            this.startIndex = startIndex;
        }

        public int getEndIndex() {
            return endIndex;
        }

        public void setEndIndex(int endIndex) {
            this.endIndex = endIndex;
        }

        public List<String> getTokens() {
            return tokens;
        }

        public void setTokens(List<String> tokens) {
            this.tokens = tokens;
        }
    }

    /**
     * Result of tokenizing all documents in a workspace.
     */
    public static class WorkspaceTokenizationResult {
        private UUID workspaceId;
        private List<DocumentTokens> documents;
        private int totalTokenCount;

        public WorkspaceTokenizationResult() {
            this.documents = new ArrayList<>();
        }

        public UUID getWorkspaceId() {
            return workspaceId;
        }

        public void setWorkspaceId(UUID workspaceId) {
            this.workspaceId = workspaceId;
        }

        public List<DocumentTokens> getDocuments() {
            return documents;
        }

        public void setDocuments(List<DocumentTokens> documents) {
            this.documents = documents;
        }

        public int getTotalTokenCount() {
            return totalTokenCount;
        }

        public void setTotalTokenCount(int totalTokenCount) {
            this.totalTokenCount = totalTokenCount;
        }
    }

    /**
     * Get the tokenization result for a workspace.
     * Documents are ordered and each document's token indices are calculated
     * to continue from the previous document.
     *
     * @param workspaceId the workspace ID
     * @return the tokenization result with continuous token indices
     */
    public WorkspaceTokenizationResult getWorkspaceTokenization(UUID workspaceId) {
        List<DocumentResponse> documents = documentService.getByWorkspaceId(workspaceId);

        WorkspaceTokenizationResult result = new WorkspaceTokenizationResult();
        result.setWorkspaceId(workspaceId);

        int currentIndex = 0;
        for (DocumentResponse doc : documents) {
            DocumentTokens docTokens = new DocumentTokens();
            docTokens.setDocumentId(doc.getId());
            docTokens.setDocumentName(doc.getName());

            // Use stored indices if available
            if (doc.getTokenStartIndex() != null && doc.getTokenEndIndex() != null) {
                docTokens.setStartIndex(doc.getTokenStartIndex());
                docTokens.setEndIndex(doc.getTokenEndIndex());
                currentIndex = doc.getTokenEndIndex() + 1;
            } else {
                // Will be calculated during actual import
                docTokens.setStartIndex(currentIndex);
                docTokens.setEndIndex(currentIndex); // Will be updated
            }

            result.getDocuments().add(docTokens);
        }

        result.setTotalTokenCount(currentIndex);
        return result;
    }

    /**
     * Update document token indices after tokenization.
     *
     * @param workspaceId the workspace ID
     * @param tokenCounts list of token counts per document (in order)
     */
    public void updateTokenIndices(UUID workspaceId, List<Integer> tokenCounts) {
        List<DocumentResponse> documents = documentService.getByWorkspaceId(workspaceId);

        if (documents.size() != tokenCounts.size()) {
            throw new IllegalArgumentException(
                    "Token counts must match document count");
        }

        int currentIndex = 0;
        for (int i = 0; i < documents.size(); i++) {
            DocumentResponse doc = documents.get(i);
            int tokenCount = tokenCounts.get(i);

            documentService.updateTokenIndices(
                    doc.getId(),
                    currentIndex,
                    currentIndex + tokenCount - 1);

            currentIndex += tokenCount;
        }
    }
}
