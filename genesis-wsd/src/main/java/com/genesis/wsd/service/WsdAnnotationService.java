package com.genesis.wsd.service;

import com.genesis.common.exception.ResourceNotFoundException;
import com.genesis.common.exception.UnauthorizedException;
import com.genesis.common.exception.ValidationException;
import com.genesis.importexport.entity.TokenEntity;
import com.genesis.importexport.repository.TokenRepository;
import com.genesis.workspace.entity.Document;
import com.genesis.workspace.repository.DocumentRepository;
import com.genesis.workspace.repository.WorkspaceMemberRepository;
import com.genesis.wsd.dto.CreateWsdAnnotationRequest;
import com.genesis.wsd.dto.WsdAnnotationDto;
import com.genesis.wsd.entity.WsdAnnotationEntity;
import com.genesis.wsd.entity.WsdSenseEntity;
import com.genesis.wsd.repository.WsdAnnotationRepository;
import com.genesis.wsd.repository.WsdSenseRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
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
    private final TokenRepository tokenRepository;
    private final DocumentRepository documentRepository;
    private final WorkspaceMemberRepository memberRepository;

    public WsdAnnotationService(WsdAnnotationRepository annotationRepository,
            WsdSenseRepository senseRepository,
            TokenRepository tokenRepository,
            DocumentRepository documentRepository,
            WorkspaceMemberRepository memberRepository) {
        this.annotationRepository = annotationRepository;
        this.senseRepository = senseRepository;
        this.tokenRepository = tokenRepository;
        this.documentRepository = documentRepository;
        this.memberRepository = memberRepository;
    }

    public WsdAnnotationDto upsert(UUID workspaceId,
            UUID callerUserId,
            String annotatorId,
            CreateWsdAnnotationRequest request) {
        requireMember(workspaceId, callerUserId);
        if (request == null || request.getTokenId() == null || request.getSenseId() == null) {
            throw new ValidationException("tokenId and senseId are required");
        }
        if (annotatorId == null || annotatorId.isBlank()) {
            throw new ValidationException("annotatorId", "annotator must be authenticated");
        }

        // Token must belong to the workspace (via its document).
        TokenEntity token = tokenRepository.findById(request.getTokenId())
                .orElseThrow(() -> new ResourceNotFoundException("Token not found: " + request.getTokenId()));
        Document document = documentRepository.findById(token.getDocumentId())
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + token.getDocumentId()));
        if (!document.getWorkspace().getId().equals(workspaceId)) {
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
        return WsdAnnotationDto.from(annotationRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<WsdAnnotationDto> getByToken(UUID workspaceId, UUID tokenId, UUID callerUserId) {
        requireMember(workspaceId, callerUserId);
        return annotationRepository.findByTokenId(tokenId).stream()
                .filter(a -> workspaceId.equals(a.getWorkspaceId()))
                .map(WsdAnnotationDto::from)
                .collect(Collectors.toList());
    }

    public void deleteByAnnotator(UUID workspaceId, UUID annotationId, String annotatorId, UUID callerUserId) {
        requireMember(workspaceId, callerUserId);
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

    private void requireMember(UUID workspaceId, UUID userId) {
        if (!memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new UnauthorizedException("Not a member of this workspace", true);
        }
    }
}
