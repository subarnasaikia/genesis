package com.genesis.workspace.service;

import com.genesis.common.exception.ResourceNotFoundException;
import com.genesis.infra.storage.FileStorageService;
import com.genesis.infra.storage.StoredFile;
import com.genesis.workspace.dto.DocumentResponse;
import com.genesis.workspace.entity.Document;
import com.genesis.workspace.entity.DocumentStatus;
import com.genesis.workspace.entity.ProcessingStatus;
import com.genesis.workspace.entity.Workspace;
import com.genesis.common.event.WorkspaceActivityEvent;
import com.genesis.workspace.event.DocumentUploadedEvent;
import com.genesis.workspace.repository.DocumentRepository;
import com.genesis.workspace.repository.WorkspaceRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
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
    private final WorkspaceAccessControl accessControl;
    private final ApplicationEventPublisher eventPublisher;

    public DocumentService(DocumentRepository documentRepository,
            WorkspaceRepository workspaceRepository,
            FileStorageService fileStorageService,
            WorkspaceAccessControl accessControl,
            ApplicationEventPublisher eventPublisher) {
        this.documentRepository = documentRepository;
        this.workspaceRepository = workspaceRepository;
        this.fileStorageService = fileStorageService;
        this.accessControl = accessControl;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Upload a document to a workspace. Caller must be a workspace member.
     */
    @Transactional
    public DocumentResponse upload(@NonNull UUID workspaceId, @NonNull MultipartFile file, @NonNull UUID userId) {
        accessControl.requireMember(workspaceId, userId);
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", workspaceId));

        int nextOrderIndex = documentRepository.findMaxOrderIndexByWorkspaceId(workspaceId)
                .map(max -> max + 1)
                .orElse(0);

        String folder = "workspaces/" + workspaceId + "/documents";
        StoredFile storedFile = fileStorageService.store(file, folder);

        Document document = new Document();
        document.setName(file.getOriginalFilename());
        document.setOrderIndex(nextOrderIndex);
        document.setFileSize(file.getSize());
        document.setStatus(DocumentStatus.UPLOADED);
        document.setProcessingStatus(ProcessingStatus.PENDING);
        document.setWorkspace(workspace);
        document.setStoredFile(storedFile);

        Document saved = documentRepository.save(document);

        eventPublisher.publishEvent(new DocumentUploadedEvent(
                this,
                saved.getId(),
                workspaceId,
                storedFile.getUrl(),
                userId,
                saved.getName()));

        eventPublisher.publishEvent(new WorkspaceActivityEvent(this, workspaceId));

        return mapToResponse(saved);
    }

    /**
     * Get all documents for a workspace. Caller must be a workspace member.
     */
    public List<DocumentResponse> getByWorkspaceId(@NonNull UUID workspaceId, @NonNull UUID callerId) {
        accessControl.requireMember(workspaceId, callerId);
        return getByWorkspaceIdInternal(workspaceId);
    }

    /**
     * Internal variant — no authorization check. For server-internal callers
     * (event handlers, async processors, share-token export) that have already
     * verified access through other means.
     */
    public List<DocumentResponse> getByWorkspaceIdInternal(@NonNull UUID workspaceId) {
        if (!workspaceRepository.existsById(workspaceId)) {
            throw new ResourceNotFoundException("Workspace", workspaceId);
        }

        return documentRepository.findByWorkspaceIdOrderByOrderIndexAsc(workspaceId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get document by ID. Caller must be a member of the document's workspace.
     */
    public DocumentResponse getById(@NonNull UUID documentId, @NonNull UUID callerId) {
        Document document = findDocumentById(documentId);
        accessControl.requireMember(document.getWorkspace().getId(), callerId);
        return mapToResponse(document);
    }

    /**
     * Internal variant — no authorization check. See
     * {@link #getByWorkspaceIdInternal(UUID)} for callers.
     */
    public DocumentResponse getByIdInternal(@NonNull UUID documentId) {
        Document document = findDocumentById(documentId);
        return mapToResponse(document);
    }

    /**
     * Update document status. Caller must be a workspace member.
     */
    @Transactional
    public DocumentResponse updateStatus(@NonNull UUID documentId, @NonNull DocumentStatus status,
            @NonNull UUID callerId) {
        Document document = findDocumentById(documentId);
        accessControl.requireMember(document.getWorkspace().getId(), callerId);
        return doUpdateStatus(document, status);
    }

    /**
     * Internal variant — no authorization check. Used by annotation services
     * (e.g. MentionService) to flip a doc into ANNOTATING when an annotator
     * begins working; access has already been verified upstream.
     */
    @Transactional
    public DocumentResponse updateStatusInternal(@NonNull UUID documentId, @NonNull DocumentStatus status) {
        Document document = findDocumentById(documentId);
        return doUpdateStatus(document, status);
    }

    private DocumentResponse doUpdateStatus(Document document, DocumentStatus status) {
        document.setStatus(status);
        Document saved = documentRepository.save(document);
        eventPublisher.publishEvent(new WorkspaceActivityEvent(this, document.getWorkspace().getId()));

        if (status == DocumentStatus.COMPLETE) {
            eventPublisher.publishEvent(new com.genesis.workspace.event.DocumentAnnotationCompletedEvent(
                    this,
                    document.getId(),
                    document.getWorkspace().getId(),
                    document.getName(),
                    null));
        }

        return mapToResponse(saved);
    }

    /**
     * Update token indices for a document (server-internal — called by the
     * tokenization pipeline). No authorization check.
     */
    @Transactional
    public DocumentResponse updateTokenIndices(@NonNull UUID documentId,
            int tokenStartIndex, int tokenEndIndex) {
        Document document = findDocumentById(documentId);
        document.setTokenStartIndex(tokenStartIndex);
        document.setTokenEndIndex(tokenEndIndex);
        Document saved = documentRepository.save(document);
        eventPublisher.publishEvent(new WorkspaceActivityEvent(this, document.getWorkspace().getId()));
        return mapToResponse(saved);
    }

    /**
     * Update document progress (server-internal — called by the annotation
     * pipeline). No authorization check.
     */
    @Transactional
    public DocumentResponse updateProgress(@NonNull UUID documentId, @NonNull Double progress) {
        Document document = findDocumentById(documentId);
        document.setProgress(progress);
        Document saved = documentRepository.save(document);
        return mapToResponse(saved);
    }

    /**
     * Delete a document. Caller must be an ADMIN of the document's workspace.
     *
     * <p>
     * Also reached via the {@code WorkspaceService.delete} cascade — that path
     * has already verified the caller is an admin, so the second check here is
     * redundant but harmless.
     */
    @Transactional
    public void delete(@NonNull UUID documentId, @NonNull UUID userId) {
        Document document = findDocumentById(documentId);
        UUID workspaceId = document.getWorkspace().getId();
        accessControl.requireAdmin(workspaceId, userId);

        if (document.getStoredFile() != null) {
            fileStorageService.delete(document.getStoredFile().getId());
        }

        String documentName = document.getName();
        documentRepository.delete(document);
        eventPublisher.publishEvent(new WorkspaceActivityEvent(this, workspaceId));
        eventPublisher.publishEvent(
                new com.genesis.workspace.event.DocumentDeletedEvent(this, documentId, workspaceId, documentName,
                        userId));
    }

    /**
     * Count documents in a workspace. Caller must be a workspace member.
     */
    public long countByWorkspaceId(@NonNull UUID workspaceId, @NonNull UUID callerId) {
        accessControl.requireMember(workspaceId, callerId);
        return documentRepository.countByWorkspaceId(workspaceId);
    }

    /**
     * Internal variant — no authorization check.
     */
    public long countByWorkspaceIdInternal(@NonNull UUID workspaceId) {
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
        response.setProgress(document.getProgress());
        response.setProcessingStatus(document.getProcessingStatus());
        response.setProcessingError(document.getProcessingError());
        response.setWorkspaceId(document.getWorkspace().getId());
        response.setTokenStartIndex(document.getTokenStartIndex());
        response.setTokenEndIndex(document.getTokenEndIndex());
        response.setCreatedAt(document.getCreatedAt());
        response.setUpdatedAt(document.getUpdatedAt());
        response.setFileSize(document.getFileSize());

        if (document.getStoredFile() != null) {
            response.setStoredFileUrl(document.getStoredFile().getUrl());
        }

        return response;
    }
}
