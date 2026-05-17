package com.genesis.ner.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
import com.genesis.workspace.entity.Workspace;
import com.genesis.workspace.repository.DocumentRepository;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class NerAnnotationServiceTest {

    @Mock private NerAnnotationRepository annotationRepository;
    @Mock private TokenRepository tokenRepository;
    @Mock private DocumentRepository documentRepository;
    @Mock private NerTagDefinitionService tagDefinitionService;
    @Mock private ApplicationEventPublisher eventPublisher;

    private NerAnnotationService service;

    private UUID documentId;
    private UUID workspaceId;
    private UUID annotatorId;

    @BeforeEach
    void setUp() {
        service = new NerAnnotationService(annotationRepository, tokenRepository,
                documentRepository, tagDefinitionService, eventPublisher);
        documentId = UUID.randomUUID();
        workspaceId = UUID.randomUUID();
        annotatorId = UUID.randomUUID();
    }

    private Document docInWorkspace() {
        Workspace w = new Workspace();
        w.setId(workspaceId);
        Document d = new Document();
        d.setId(documentId);
        d.setWorkspace(w);
        return d;
    }

    private CreateNerAnnotationRequest req(int start, int end, String label) {
        CreateNerAnnotationRequest r = new CreateNerAnnotationRequest();
        r.setDocumentId(documentId);
        r.setStartTokenIndex(start);
        r.setEndTokenIndex(end);
        r.setLabel(label);
        return r;
    }

    private void stubDocument() {
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(docInWorkspace()));
    }

    private void stubTokenCount(long count) {
        when(tokenRepository.countByDocumentId(documentId)).thenReturn(count);
    }

    private void stubTagSet() {
        when(tagDefinitionService.effectiveTagSet(workspaceId))
                .thenReturn(Set.of("PERSON", "ORG", "GPE"));
    }

    @Test
    @DisplayName("create persists valid span and publishes log event")
    void create_validSpan_persists() {
        stubDocument();
        stubTokenCount(10L);
        stubTagSet();
        when(annotationRepository.save(any(NerAnnotationEntity.class)))
                .thenAnswer(inv -> {
                    NerAnnotationEntity e = inv.getArgument(0);
                    e.setId(UUID.randomUUID());
                    return e;
                });

        NerAnnotationDto dto = service.create(req(2, 4, "PERSON"), annotatorId);

        assertNotNull(dto);
        assertEquals(2, dto.getStartTokenIndex());
        assertEquals(4, dto.getEndTokenIndex());
        assertEquals("PERSON", dto.getLabel());
        assertEquals(annotatorId.toString(), dto.getAnnotatorId());
        verify(eventPublisher).publishEvent(any(AnnotationLogEvent.class));
    }

    @Test
    @DisplayName("create rejects end < start")
    void create_endBeforeStart_rejected() {
        stubDocument();
        assertThrows(ValidationException.class,
                () -> service.create(req(5, 2, "PERSON"), annotatorId));
        verify(annotationRepository, never()).save(any());
    }

    @Test
    @DisplayName("create rejects negative start")
    void create_negativeStart_rejected() {
        stubDocument();
        assertThrows(ValidationException.class,
                () -> service.create(req(-1, 2, "PERSON"), annotatorId));
    }

    @Test
    @DisplayName("create rejects end beyond document token count")
    void create_endOutOfBounds_rejected() {
        stubDocument();
        stubTokenCount(10L);
        assertThrows(ValidationException.class,
                () -> service.create(req(0, 10, "PERSON"), annotatorId));
    }

    @Test
    @DisplayName("create rejects label not in effective set")
    void create_unknownLabel_rejected() {
        stubDocument();
        stubTokenCount(10L);
        stubTagSet();
        assertThrows(ValidationException.class,
                () -> service.create(req(2, 4, "NOT_A_TAG"), annotatorId));
    }

    @Test
    @DisplayName("create allows overlapping spans (nesting)")
    void create_overlap_allowed() {
        stubDocument();
        stubTokenCount(10L);
        stubTagSet();
        when(annotationRepository.save(any(NerAnnotationEntity.class)))
                .thenAnswer(inv -> {
                    NerAnnotationEntity e = inv.getArgument(0);
                    e.setId(UUID.randomUUID());
                    return e;
                });

        NerAnnotationDto outer = service.create(req(0, 5, "ORG"), annotatorId);
        NerAnnotationDto inner = service.create(req(2, 3, "PERSON"), annotatorId);
        assertNotNull(outer);
        assertNotNull(inner);
        verify(annotationRepository, times(2)).save(any(NerAnnotationEntity.class));
    }

    @Test
    @DisplayName("create rejects when document missing")
    void create_documentMissing_rejected() {
        when(documentRepository.findById(documentId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.create(req(0, 1, "PERSON"), annotatorId));
    }

    @Test
    @DisplayName("update by non-annotator rejected")
    void update_byOtherUser_rejected() {
        NerAnnotationEntity existing = new NerAnnotationEntity();
        existing.setId(UUID.randomUUID());
        existing.setDocumentId(documentId);
        existing.setAnnotatorId(annotatorId.toString());
        existing.setStartTokenIndex(0);
        existing.setEndTokenIndex(2);
        existing.setLabel("PERSON");
        when(annotationRepository.findById(existing.getId())).thenReturn(Optional.of(existing));

        UpdateNerAnnotationRequest u = new UpdateNerAnnotationRequest();
        u.setLabel("ORG");

        assertThrows(UnauthorizedException.class,
                () -> service.update(existing.getId(), u, UUID.randomUUID()));
    }

    @Test
    @DisplayName("update by owner persists patched fields")
    void update_byOwner_persists() {
        NerAnnotationEntity existing = new NerAnnotationEntity();
        existing.setId(UUID.randomUUID());
        existing.setDocumentId(documentId);
        existing.setAnnotatorId(annotatorId.toString());
        existing.setStartTokenIndex(0);
        existing.setEndTokenIndex(2);
        existing.setLabel("PERSON");
        when(annotationRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        stubDocument();
        stubTokenCount(10L);
        stubTagSet();
        when(annotationRepository.save(any(NerAnnotationEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        UpdateNerAnnotationRequest u = new UpdateNerAnnotationRequest();
        u.setLabel("ORG");

        NerAnnotationDto dto = service.update(existing.getId(), u, annotatorId);
        assertEquals("ORG", dto.getLabel());
        assertEquals(0, dto.getStartTokenIndex());
        assertEquals(2, dto.getEndTokenIndex());
    }

    @Test
    @DisplayName("delete by non-annotator rejected")
    void delete_byOtherUser_rejected() {
        NerAnnotationEntity existing = new NerAnnotationEntity();
        existing.setId(UUID.randomUUID());
        existing.setDocumentId(documentId);
        existing.setAnnotatorId(annotatorId.toString());
        existing.setStartTokenIndex(0);
        existing.setEndTokenIndex(2);
        existing.setLabel("PERSON");
        when(annotationRepository.findById(existing.getId())).thenReturn(Optional.of(existing));

        assertThrows(UnauthorizedException.class,
                () -> service.delete(existing.getId(), UUID.randomUUID()));
        verify(annotationRepository, never()).delete(any());
    }

    @Test
    @DisplayName("delete by owner publishes NER_DELETED event")
    void delete_byOwner_publishesEvent() {
        NerAnnotationEntity existing = new NerAnnotationEntity();
        existing.setId(UUID.randomUUID());
        existing.setDocumentId(documentId);
        existing.setAnnotatorId(annotatorId.toString());
        existing.setStartTokenIndex(0);
        existing.setEndTokenIndex(2);
        existing.setLabel("PERSON");
        when(annotationRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(docInWorkspace()));

        service.delete(existing.getId(), annotatorId);

        verify(annotationRepository).delete(existing);
        verify(eventPublisher).publishEvent(any(AnnotationLogEvent.class));
    }

    @Test
    @DisplayName("delete missing throws")
    void delete_missing_throws() {
        UUID id = UUID.randomUUID();
        when(annotationRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.delete(id, annotatorId));
    }
}
