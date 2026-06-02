package com.genesis.wsd.service;

import com.genesis.common.event.ActionType;
import com.genesis.common.event.AnnotationLogEvent;
import com.genesis.common.exception.ResourceNotFoundException;
import com.genesis.common.exception.UnauthorizedException;
import com.genesis.common.exception.ValidationException;
import com.genesis.common.port.DocumentQueryPort;
import com.genesis.common.port.TokenQueryPort;
import com.genesis.wsd.dto.CreateWsdAnnotationRequest;
import com.genesis.wsd.dto.WsdAnnotationDto;
import com.genesis.wsd.entity.WsdAnnotationEntity;
import com.genesis.wsd.entity.WsdSenseEntity;
import com.genesis.wsd.repository.WsdAnnotationRepository;
import com.genesis.wsd.repository.WsdSenseRepository;
import com.genesis.workspace.service.WorkspaceAccessControl;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Per-annotator WSD annotations.
 *
 * <p>Workspace scoping: only members of the workspace can read or write
 * annotations belonging to it. Annotators may only delete their own rows.
 * Upsert on the (token_id, annotator_id) unique constraint.
 */
@Service
@Transactional
public class WsdAnnotationService {

    private final WsdAnnotationRepository annotationRepository;
    private final WsdSenseRepository senseRepository;
    private final TokenQueryPort tokenQuery;
    private final DocumentQueryPort documentQuery;
    private final WorkspaceAccessControl accessControl;
    private final ApplicationEventPublisher eventPublisher;

    public WsdAnnotationService(WsdAnnotationRepository annotationRepository,
            WsdSenseRepository senseRepository,
            TokenQueryPort tokenQuery,
            DocumentQueryPort documentQuery,
            WorkspaceAccessControl accessControl,
            ApplicationEventPublisher eventPublisher) {
        this.annotationRepository = annotationRepository;
        this.senseRepository = senseRepository;
        this.tokenQuery = tokenQuery;
        this.documentQuery = documentQuery;
        this.accessControl = accessControl;
        this.eventPublisher = eventPublisher;
    }

    public WsdAnnotationDto upsert(UUID workspaceId,
            UUID callerUserId,
            String annotatorId,
            CreateWsdAnnotationRequest request) {
        accessControl.requireMember(workspaceId, callerUserId);
        if (request == null || request.getTokenId() == null || request.getSenseId() == null) {
            throw new ValidationException("tokenId and senseId are required");
        }
        if (annotatorId == null || annotatorId.isBlank()) {
            throw new ValidationException("annotatorId", "annotator must be authenticated");
        }

        // Token must belong to the workspace (via its document).
        UUID tokenDocumentId = tokenQuery.documentIdForToken(request.getTokenId());
        UUID tokenWorkspaceId = documentQuery.workspaceIdForDocument(tokenDocumentId);
        if (!workspaceId.equals(tokenWorkspaceId)) {
            throw new ValidationException("tokenId does not belong to workspace " + workspaceId);
        }

        // Sense must belong to the workspace.
        WsdSenseEntity sense = senseRepository.findById(request.getSenseId())
                .orElseThrow(() -> new ResourceNotFoundException("Sense not found: " + request.getSenseId()));
        if (!sense.getWorkspaceId().equals(workspaceId)) {
            throw new ValidationException("senseId does not belong to workspace " + workspaceId);
        }

        // Upsert.
        Optional<WsdAnnotationEntity> existing = annotationRepository
                .findByTokenIdAndAnnotatorId(request.getTokenId(), annotatorId);
        WsdAnnotationEntity entity = existing.orElseGet(WsdAnnotationEntity::new);
        entity.setTokenId(request.getTokenId());
        entity.setSenseId(request.getSenseId());
        entity.setAnnotatorId(annotatorId);
        entity.setWorkspaceId(workspaceId);
        entity.setDocumentId(tokenDocumentId);
        WsdAnnotationEntity saved = annotationRepository.save(entity);

        // Audit log: WSD_ANNOTATED. Persisted by genesis-logging listener
        // at AFTER_COMMIT — failure cannot roll back this annotation.
        eventPublisher.publishEvent(new AnnotationLogEvent(this,
                workspaceId,
                annotatorId,
                ActionType.WSD_ANNOTATED,
                saved.getTokenId(),
                String.format("{\"senseId\":\"%s\",\"word\":\"%s\"}",
                        saved.getSenseId(),
                        escape(tokenQuery.formForToken(saved.getTokenId())))));

        return WsdAnnotationDto.from(saved);
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @Transactional(readOnly = true)
    public List<WsdAnnotationDto> getByToken(UUID workspaceId, UUID tokenId, UUID callerUserId) {
        accessControl.requireMember(workspaceId, callerUserId);
        return annotationRepository.findByTokenId(tokenId).stream()
                .filter(a -> workspaceId.equals(a.getWorkspaceId()))
                .map(WsdAnnotationDto::from)
                .collect(Collectors.toList());
    }

    /**
     * All annotations for a document, across annotators, enriched with each
     * sense's label so the editor can show pre-loaded tags inline without
     * fetching per-word sense inventories.
     */
    @Transactional(readOnly = true)
    public List<WsdAnnotationDto> getByDocument(UUID workspaceId, UUID documentId, UUID callerUserId) {
        accessControl.requireMember(workspaceId, callerUserId);
        List<WsdAnnotationEntity> annotations =
                annotationRepository.findByWorkspaceIdAndDocumentId(workspaceId, documentId);

        Set<UUID> senseIds = annotations.stream()
                .map(WsdAnnotationEntity::getSenseId)
                .collect(Collectors.toSet());
        Map<UUID, String> labelById = senseRepository.findAllById(senseIds).stream()
                .collect(Collectors.toMap(WsdSenseEntity::getId, WsdSenseEntity::getSenseLabel));

        return annotations.stream()
                .map(a -> WsdAnnotationDto.from(a, labelById.get(a.getSenseId())))
                .collect(Collectors.toList());
    }

    public void deleteByAnnotator(UUID workspaceId, UUID annotationId, String annotatorId, UUID callerUserId) {
        accessControl.requireMember(workspaceId, callerUserId);
        WsdAnnotationEntity entity = annotationRepository.findById(annotationId)
                .orElseThrow(() -> new ResourceNotFoundException("Annotation not found: " + annotationId));
        if (!entity.getWorkspaceId().equals(workspaceId)) {
            throw new UnauthorizedException("Annotation belongs to a different workspace", true);
        }
        if (!entity.getAnnotatorId().equals(annotatorId)) {
            throw new UnauthorizedException("Annotators may only delete their own annotations", true);
        }
        annotationRepository.delete(entity);
    }
}
