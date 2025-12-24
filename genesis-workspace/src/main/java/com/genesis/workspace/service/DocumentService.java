package com.genesis.workspace.service;

import com.genesis.common.exception.ResourceNotFoundException;
import com.genesis.infra.storage.FileStorageService;
import com.genesis.infra.storage.StoredFile;
import com.genesis.workspace.dto.DocumentResponse;
import com.genesis.workspace.entity.Document;
import com.genesis.workspace.entity.DocumentStatus;
import com.genesis.workspace.entity.Workspace;
import com.genesis.workspace.repository.DocumentRepository;
import com.genesis.workspace.repository.WorkspaceRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service for document operations.
 */
@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final WorkspaceRepository workspaceRepository;
    private final FileStorageService fileStorageService;

    public DocumentService(DocumentRepository documentRepository,
            WorkspaceRepository workspaceRepository,
            FileStorageService fileStorageService) {
        this.documentRepository = documentRepository;
        this.workspaceRepository = workspaceRepository;
        this.fileStorageService = fileStorageService;
    }

    /**
     * Upload a document to a workspace.
     *
     * @param workspaceId the workspace ID
     * @param file        the file to upload
     * @return the created document response
     */
    @Transactional
    public DocumentResponse upload(@NonNull UUID workspaceId, @NonNull MultipartFile file) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", workspaceId));

        // Get next orderIndex
        int nextOrderIndex = documentRepository.findMaxOrderIndexByWorkspaceId(workspaceId)
                .map(max -> max + 1)
                .orElse(0);

        // Store file in Cloudinary
        String folder = "workspaces/" + workspaceId + "/documents";
        StoredFile storedFile = fileStorageService.store(file, folder);

        // Create document
        Document document = new Document();
        document.setName(file.getOriginalFilename());
        document.setOrderIndex(nextOrderIndex);
        document.setStatus(DocumentStatus.UPLOADED);
        document.setWorkspace(workspace);
        document.setStoredFile(storedFile);

        Document saved = documentRepository.save(document);
        return mapToResponse(saved);
    }

    /**
     * Get all documents for a workspace ordered by orderIndex.
     *
     * @param workspaceId the workspace ID
     * @return list of document responses
     */
    public List<DocumentResponse> getByWorkspaceId(@NonNull UUID workspaceId) {
        if (!workspaceRepository.existsById(workspaceId)) {
            throw new ResourceNotFoundException("Workspace", workspaceId);
        }

        return documentRepository.findByWorkspaceIdOrderByOrderIndexAsc(workspaceId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get document by ID.
     *
     * @param documentId the document ID
     * @return the document response
     */
    public DocumentResponse getById(@NonNull UUID documentId) {
        Document document = findDocumentById(documentId);
        return mapToResponse(document);
    }

    /**
     * Update document status.
     *
     * @param documentId the document ID
     * @param status     the new status
     * @return the updated document response
     */
    @Transactional
    public DocumentResponse updateStatus(@NonNull UUID documentId, @NonNull DocumentStatus status) {
        Document document = findDocumentById(documentId);
        document.setStatus(status);
        Document saved = documentRepository.save(document);
        return mapToResponse(saved);
    }

    /**
     * Update token indices for a document (after import/tokenization).
     *
     * @param documentId      the document ID
     * @param tokenStartIndex the start token index
     * @param tokenEndIndex   the end token index
     * @return the updated document response
     */
    @Transactional
    public DocumentResponse updateTokenIndices(@NonNull UUID documentId,
            int tokenStartIndex, int tokenEndIndex) {
        Document document = findDocumentById(documentId);
        document.setTokenStartIndex(tokenStartIndex);
        document.setTokenEndIndex(tokenEndIndex);
        Document saved = documentRepository.save(document);
        return mapToResponse(saved);
    }

    /**
     * Delete a document.
     *
     * @param documentId the document ID
     */
    @Transactional
    public void delete(@NonNull UUID documentId) {
        Document document = findDocumentById(documentId);

        // Delete stored file if exists
        if (document.getStoredFile() != null) {
            fileStorageService.delete(document.getStoredFile().getId());
        }

        documentRepository.delete(document);
    }

    /**
     * Count documents in a workspace.
     *
     * @param workspaceId the workspace ID
     * @return the document count
     */
    public long countByWorkspaceId(@NonNull UUID workspaceId) {
        return documentRepository.countByWorkspaceId(workspaceId);
    }

    private Document findDocumentById(UUID documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", documentId));
    }

    private DocumentResponse mapToResponse(Document document) {
        DocumentResponse response = new DocumentResponse();
        response.setId(document.getId());
        response.setName(document.getName());
        response.setOrderIndex(document.getOrderIndex());
        response.setStatus(document.getStatus());
        response.setWorkspaceId(document.getWorkspace().getId());
        response.setTokenStartIndex(document.getTokenStartIndex());
        response.setTokenEndIndex(document.getTokenEndIndex());
        response.setCreatedAt(document.getCreatedAt());
        response.setUpdatedAt(document.getUpdatedAt());

        if (document.getStoredFile() != null) {
            response.setStoredFileUrl(document.getStoredFile().getUrl());
        }

        return response;
    }
}
