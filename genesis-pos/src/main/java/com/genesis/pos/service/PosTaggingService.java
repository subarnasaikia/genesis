package com.genesis.pos.service;

import com.genesis.common.event.ActionType;
import com.genesis.common.event.AnnotationLogEvent;
import com.genesis.common.exception.ResourceNotFoundException;
import com.genesis.common.exception.ValidationException;
import com.genesis.importexport.entity.TokenEntity;
import com.genesis.importexport.repository.TokenRepository;
import com.genesis.pos.dto.BatchUpdatePosRequest;
import com.genesis.pos.dto.PosAnnotationDto;
import com.genesis.pos.entity.PosAnnotationEntity;
import com.genesis.pos.repository.PosAnnotationRepository;
import com.genesis.workspace.entity.Document;
import com.genesis.workspace.repository.DocumentRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PosTaggingService {

    public static final Set<String> UNIVERSAL_POS_TAGS = Set.of(
            "NOUN", "PROPN", "VERB", "ADJ", "ADV", "PRON", "DET", "ADP",
            "CONJ", "SCONJ", "AUX", "NUM", "PART", "INTJ", "SYM", "PUNCT", "X");

    private final PosAnnotationRepository posRepository;
    private final TokenRepository tokenRepository;
    private final DocumentRepository documentRepository;
    private final PosTagDefinitionService tagDefinitionService;
    private final ApplicationEventPublisher eventPublisher;

    public PosTaggingService(PosAnnotationRepository posRepository,
            TokenRepository tokenRepository,
            DocumentRepository documentRepository,
            PosTagDefinitionService tagDefinitionService,
            ApplicationEventPublisher eventPublisher) {
        this.posRepository = posRepository;
        this.tokenRepository = tokenRepository;
        this.documentRepository = documentRepository;
        this.tagDefinitionService = tagDefinitionService;
        this.eventPublisher = eventPublisher;
    }

    public PosAnnotationDto updatePos(UUID tokenId, String annotatorId, String posTag) {
        if (annotatorId == null || annotatorId.isBlank()) {
            throw new ValidationException("annotatorId", "annotator must be authenticated");
        }

        if (posTag == null) {
            posRepository.deleteByTokenIdAndAnnotatorId(tokenId, annotatorId);
            return null;
        }

        TokenEntity token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new ResourceNotFoundException("Token not found: " + tokenId));

        Document document = documentRepository.findById(token.getDocumentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document not found: " + token.getDocumentId()));
        UUID workspaceId = document.getWorkspace() != null
                ? document.getWorkspace().getId() : null;

        if (!tagDefinitionService.effectiveTagSet(workspaceId).contains(posTag)) {
            throw new ValidationException("posTag", "Invalid POS tag: " + posTag);
        }

        Optional<PosAnnotationEntity> existing = posRepository.findByTokenIdAndAnnotatorId(tokenId, annotatorId);
        PosAnnotationEntity entity = existing.orElseGet(PosAnnotationEntity::new);
        entity.setTokenId(tokenId);
        entity.setDocumentId(token.getDocumentId());
        entity.setAnnotatorId(annotatorId);
        entity.setPosTag(posTag);

        PosAnnotationEntity saved = posRepository.save(entity);

        if (workspaceId != null) {
            eventPublisher.publishEvent(new AnnotationLogEvent(this,
                    workspaceId,
                    annotatorId,
                    ActionType.POS_TAGGED,
                    saved.getTokenId(),
                    String.format("{\"posTag\":\"%s\",\"documentId\":\"%s\"}",
                            saved.getPosTag(),
                            saved.getDocumentId())));
        }

        return PosAnnotationDto.from(saved);
    }

    public List<PosAnnotationDto> batchUpdate(List<BatchUpdatePosRequest.Item> items, String annotatorId) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        List<PosAnnotationDto> results = new ArrayList<>(items.size());
        for (BatchUpdatePosRequest.Item item : items) {
            PosAnnotationDto dto = updatePos(item.getTokenId(), annotatorId, item.getPos());
            if (dto != null) {
                results.add(dto);
            }
        }
        return results;
    }

    @Transactional(readOnly = true)
    public List<PosAnnotationDto> getAnnotationsByToken(UUID tokenId) {
        return posRepository.findByTokenId(tokenId).stream()
                .map(PosAnnotationDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PosAnnotationDto> getAnnotationsByDocument(UUID documentId) {
        return posRepository.findByDocumentId(documentId).stream()
                .map(PosAnnotationDto::from)
                .collect(Collectors.toList());
    }

    /**
     * Returns token-id → majority POS tag for the document. Ties broken by most
     * recent timestamp (the repository query orders by COUNT DESC then MAX(timestamp) DESC).
     */
    @Transactional(readOnly = true)
    public Map<UUID, String> getMajorityPosByDocument(UUID documentId) {
        List<Object[]> rows = posRepository.findPosCountsByDocumentId(documentId);
        Map<UUID, String> majority = new LinkedHashMap<>();
        Set<UUID> seen = new HashSet<>();
        for (Object[] row : rows) {
            UUID tokenId = (UUID) row[0];
            String posTag = (String) row[1];
            if (seen.add(tokenId)) {
                majority.put(tokenId, posTag);
            }
        }
        return majority;
    }

    /**
     * Returns token-id → distinct annotator count for the document.
     * Feeds the sidecar CSV so downstream consumers can filter by
     * annotator agreement (e.g. exclude single-annotator tokens).
     */
    @Transactional(readOnly = true)
    public Map<UUID, Long> getAnnotatorCountsByDocument(UUID documentId) {
        List<Object[]> rows = posRepository.findAnnotatorCountsByDocumentId(documentId);
        Map<UUID, Long> counts = new LinkedHashMap<>();
        for (Object[] row : rows) {
            UUID tokenId = (UUID) row[0];
            Long count = ((Number) row[1]).longValue();
            counts.put(tokenId, count);
        }
        return counts;
    }
}
