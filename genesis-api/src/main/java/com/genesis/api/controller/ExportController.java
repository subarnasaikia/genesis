package com.genesis.api.controller;

import com.genesis.coref.service.CoreferenceService;
import com.genesis.importexport.dto.ExportOptions;
import com.genesis.importexport.service.ExportService;
import com.genesis.importexport.service.ExportService.DocumentInfo;
import com.genesis.importexport.service.ExportService.ExportResult;
import com.genesis.pos.service.PosTaggingService;
import com.genesis.workspace.dto.DocumentResponse;
import com.genesis.workspace.service.DocumentService;
import com.genesis.workspace.service.WorkspaceService;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for export operations.
 */
@RestController
@RequestMapping("/api/export")
public class ExportController {

    private final ExportService exportService;
    private final WorkspaceService workspaceService;
    private final DocumentService documentService;
    private final CoreferenceService coreferenceService;
    private final PosTaggingService posTaggingService;

    public ExportController(ExportService exportService,
            WorkspaceService workspaceService,
            DocumentService documentService,
            CoreferenceService coreferenceService,
            PosTaggingService posTaggingService) {
        this.exportService = exportService;
        this.workspaceService = workspaceService;
        this.documentService = documentService;
        this.coreferenceService = coreferenceService;
        this.posTaggingService = posTaggingService;
    }

    /**
     * Export a single document.
     */
    @PostMapping("/documents/{documentId}")
    public ResponseEntity<byte[]> exportDocument(
            @PathVariable UUID documentId,
            @RequestBody(required = false) ExportOptions options) {
        if (options == null) {
            options = new ExportOptions();
        }

        @SuppressWarnings("null")
        DocumentResponse document = documentService.getById(documentId);

        // Fetch coreference annotations
        @SuppressWarnings("null")
        Map<String, String> corefAnnotations = coreferenceService.generateCorefAnnotations(documentId);

        // Fetch majority-vote POS overrides for this document
        Map<UUID, String> posOverrides = posTaggingService.getMajorityPosByDocument(documentId);

        @SuppressWarnings("null")
        ExportResult result = exportService.exportDocument(
                documentId,
                document.getName(),
                corefAnnotations,
                posOverrides,
                options);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.getFilename() + "\"")
                .contentType(MediaType.parseMediaType(result.getContentType()))
                .body(result.getContent());
    }

    /**
     * Export an entire workspace.
     */
    @PostMapping("/workspaces/{workspaceId}")
    public ResponseEntity<byte[]> exportWorkspace(
            @PathVariable UUID workspaceId,
            @RequestBody(required = false) ExportOptions options) throws IOException {
        if (options == null) {
            options = new ExportOptions();
        }

        @SuppressWarnings("null")
        var workspace = workspaceService.getById(workspaceId);
        @SuppressWarnings("null")
        List<DocumentResponse> documents = documentService.getByWorkspaceId(workspaceId);

        @SuppressWarnings("null")
        List<DocumentInfo> docInfos = documents.stream()
                .map(d -> new DocumentInfo(d.getId(), d.getName()))
                .collect(Collectors.toList());

        @SuppressWarnings("null")
        Map<UUID, Map<String, String>> corefAnnotationsPerDoc = coreferenceService
                .generateWorkspaceCorefAnnotations(workspaceId);

        // Fetch majority-vote POS overrides per document
        Map<UUID, Map<UUID, String>> posOverridesPerDoc = new HashMap<>();
        for (DocumentResponse d : documents) {
            posOverridesPerDoc.put(d.getId(), posTaggingService.getMajorityPosByDocument(d.getId()));
        }

        @SuppressWarnings("null")
        ExportResult result = exportService.exportWorkspace(
                docInfos,
                corefAnnotationsPerDoc,
                posOverridesPerDoc,
                options,
                workspace.getName());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.getFilename() + "\"")
                .contentType(MediaType.parseMediaType(result.getContentType()))
                .body(result.getContent());
    }
}
