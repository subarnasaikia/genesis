package com.genesis.ner.service;

import com.genesis.common.event.ActionType;
import com.genesis.common.event.AnnotationLogEvent;
import com.genesis.common.exception.ResourceNotFoundException;
import com.genesis.common.exception.UnauthorizedException;
import com.genesis.common.exception.ValidationException;
import com.genesis.importexport.repository.TokenRepository;
import com.genesis.ner.dto.CreateNerAnnotationRequest;
import com.genesis.ner.dto.NerAnnotationDto;
import com.genesis.ner.dto.UpdateNerAnnotationRequest;
import com.genesis.ner.entity.NerAnnotationEntity;
import com.genesis.ner.repository.NerAnnotationRepository;
import com.genesis.workspace.entity.Document;
import com.genesis.workspace.repository.DocumentRepository;
import com.genesis.workspace.service.WorkspaceAccessControl;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Span-level NER annotation service. Differs from {@code PosTaggingService} in
 * that each annotation covers a token range {@code [start..end]} (inclusive,
 * 0-based global index within the document) rather than a single token.
 *
 * <p>By design this service does NOT reject overlapping or nested spans — see
 * the entity javadoc for rationale. The only span-shape constraint is
 * {@code end >= start} (enforced both in Java and as a Postgres CHECK).
 */
@Service
@Transactional
public class NerAnnotationService {

    private final NerAnnotationRepository annotationRepository;
    private final TokenRepository tokenRepository;
    private final DocumentRepository documentRepository;
    private final NerTagDefinitionService tagDefinitionService;
    private final WorkspaceAccessControl accessControl;
    private final ApplicationEventPublisher eventPublisher;

    public NerAnnotationService(NerAnnotationRepository annotationRepository,
            TokenRepository tokenRepository,
            DocumentRepository documentRepository,
            NerTagDefinitionService tagDefinitionService,
            WorkspaceAccessControl accessControl,
            ApplicationEventPublisher eventPublisher) {
        this.annotationRepository = annotationRepository;
        this.tokenRepository = tokenRepository;
        this.documentRepository = documentRepository;
        this.tagDefinitionService = tagDefinitionService;
        this.accessControl = accessControl;
        this.eventPublisher = eventPublisher;
    }

    public NerAnnotationDto create(CreateNerAnnotationRequest request, UUID callerUserId) {
        if (request == null) {
            throw new ValidationException("body", "Request body required");
        }
        if (callerUserId == null) {
            throw new UnauthorizedException("Authentication required");
        }

        UUID documentId = request.getDocumentId();
        if (documentId == null) {
            throw new ValidationException("documentId", "documentId is required");
        }
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document not found: " + documentId));
        UUID workspaceId = document.getWorkspace() != null
                ? document.getWorkspace().getId() : null;
        requireWorkspaceMember(workspaceId, callerUserId);

        Integer start = request.getStartTokenIndex();
        Integer end = request.getEndTokenIndex();
        String label = request.getLabel();
        validateSpan(documentId, start, end);
        validateLabel(label, workspaceId, callerUserId);

        NerAnnotationEntity entity = new NerAnnotationEntity();
        entity.setDocumentId(documentId);
        entity.setStartTokenIndex(start);
        entity.setEndTokenIndex(end);
        entity.setLabel(label);
        entity.setAnnotatorId(callerUserId.toString());

        NerAnnotationEntity saved = annotationRepository.save(entity);

        publishLog(workspaceId, callerUserId.toString(), ActionType.NER_ANNOTATED,
                saved.getId(), saved.getDocumentId(), saved.getLabel(),
                saved.getStartTokenIndex(), saved.getEndTokenIndex());

        return NerAnnotationDto.from(saved);
    }

    public NerAnnotationDto update(UUID annotationId, UpdateNerAnnotationRequest request,
            UUID callerUserId) {
        if (request == null) {
            throw new ValidationException("body", "Request body required");
        }
        if (callerUserId == null) {
            throw new UnauthorizedException("Authentication required");
        }
        NerAnnotationEntity entity = annotationRepository.findById(annotationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "NER annotation not found: " + annotationId));

        Document document = documentRepository.findById(entity.getDocumentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document not found: " + entity.getDocumentId()));
        UUID workspaceId = document.getWorkspace() != null
                ? document.getWorkspace().getId() : null;
        requireWorkspaceMember(workspaceId, callerUserId);

        if (!entity.getAnnotatorId().equals(callerUserId.toString())) {
            throw new UnauthorizedException(
                    "Only the annotator can edit this NER span", true);
        }

        Integer start = request.getStartTokenIndex() != null
                ? request.getStartTokenIndex() : entity.getStartTokenIndex();
        Integer end = request.getEndTokenIndex() != null
                ? request.getEndTokenIndex() : entity.getEndTokenIndex();
        String label = request.getLabel() != null
                ? request.getLabel() : entity.getLabel();

        validateSpan(entity.getDocumentId(), start, end);
        validateLabel(label, workspaceId, callerUserId);

        entity.setStartTokenIndex(start);
        entity.setEndTokenIndex(end);
        entity.setLabel(label);

        NerAnnotationEntity saved = annotationRepository.save(entity);

        publishLog(workspaceId, callerUserId.toString(), ActionType.NER_ANNOTATED,
                saved.getId(), saved.getDocumentId(), saved.getLabel(),
                saved.getStartTokenIndex(), saved.getEndTokenIndex());

        return NerAnnotationDto.from(saved);
    }

    public void delete(UUID annotationId, UUID callerUserId) {
        if (callerUserId == null) {
            throw new UnauthorizedException("Authentication required");
        }
        NerAnnotationEntity entity = annotationRepository.findById(annotationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "NER annotation not found: " + annotationId));

        Document document = documentRepository.findById(entity.getDocumentId()).orElse(null);
        UUID workspaceId = document != null && document.getWorkspace() != null
                ? document.getWorkspace().getId() : null;
        requireWorkspaceMember(workspaceId, callerUserId);

        if (!entity.getAnnotatorId().equals(callerUserId.toString())) {
            throw new UnauthorizedException(
                    "Only the annotator can delete this NER span", true);
        }
        annotationRepository.delete(entity);

        publishLog(workspaceId, callerUserId.toString(), ActionType.NER_DELETED,
                entity.getId(), entity.getDocumentId(), entity.getLabel(),
                entity.getStartTokenIndex(), entity.getEndTokenIndex());
    }

    @Transactional(readOnly = true)
    public List<NerAnnotationDto> listByDocument(UUID documentId, UUID callerUserId) {
        requireDocumentWorkspaceMember(documentId, callerUserId);
        return annotationRepository.findByDocumentId(documentId).stream()
                .map(NerAnnotationDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NerAnnotationDto> listByDocumentAndAnnotator(UUID documentId, String annotatorId,
            UUID callerUserId) {
        requireDocumentWorkspaceMember(documentId, callerUserId);
        return annotationRepository.findByDocumentIdAndAnnotatorId(documentId, annotatorId).stream()
                .map(NerAnnotationDto::from)
                .collect(Collectors.toList());
    }

    private void requireDocumentWorkspaceMember(UUID documentId, UUID callerUserId) {
        if (callerUserId == null) {
            throw new UnauthorizedException("Authentication required");
        }
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document not found: " + documentId));
        UUID workspaceId = document.getWorkspace() != null
                ? document.getWorkspace().getId() : null;
        requireWorkspaceMember(workspaceId, callerUserId);
    }

    private void requireWorkspaceMember(UUID workspaceId, UUID callerUserId) {
        if (workspaceId == null) {
            throw new UnauthorizedException(
                    "Document is not associated with a workspace", true);
        }
        accessControl.requireMember(workspaceId, callerUserId);
    }

    private void validateSpan(UUID documentId, Integer start, Integer end) {
        if (start == null || end == null) {
            throw new ValidationException("tokenIndex",
                    "startTokenIndex and endTokenIndex are required");
        }
        if (start < 0) {
            throw new ValidationException("startTokenIndex",
                    "startTokenIndex must be >= 0");
        }
        if (end < start) {
            throw new ValidationException("endTokenIndex",
                    "endTokenIndex must be >= startTokenIndex");
        }
        long tokenCount = tokenRepository.countByDocumentId(documentId);
        if (end >= tokenCount) {
            throw new ValidationException("endTokenIndex",
                    "endTokenIndex " + end + " is out of document bounds (token count: "
                            + tokenCount + ")");
        }
    }

    private void validateLabel(String label, UUID workspaceId, UUID callerUserId) {
        if (label == null || label.isBlank()) {
            throw new ValidationException("label", "label is required");
        }
        Set<String> effective = tagDefinitionService.effectiveTagSet(workspaceId, callerUserId);
        if (!effective.contains(label)) {
            throw new ValidationException("label",
                    "Invalid NER label for this workspace: " + label);
        }
    }

    private void publishLog(UUID workspaceId, String actorId, ActionType action,
            UUID annotationId, UUID documentId, String label, Integer start, Integer end) {
        if (workspaceId == null) {
            return;
        }
        String details = String.format(
                "{\"annotationId\":\"%s\",\"documentId\":\"%s\",\"label\":\"%s\","
                        + "\"start\":%d,\"end\":%d}",
                annotationId, documentId, label, start, end);
        eventPublisher.publishEvent(new AnnotationLogEvent(this,
                workspaceId, actorId, action, annotationId, details));
    }
}
