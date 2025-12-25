package com.genesis.importexport.controller;

import com.genesis.common.response.ApiResponse;
import com.genesis.importexport.service.CoNLLExporter;
import com.genesis.importexport.service.CoNLLParser;
import com.genesis.importexport.service.DocumentTextService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for import/export operations.
 */
@RestController
@RequestMapping("/api/import-export")
public class ImportExportController {

    private final CoNLLParser conllParser;
    private final CoNLLExporter conllExporter;
    private final DocumentTextService documentTextService;

    public ImportExportController(
            CoNLLParser conllParser,
            CoNLLExporter conllExporter,
            DocumentTextService documentTextService) {
        this.conllParser = conllParser;
        this.conllExporter = conllExporter;
        this.documentTextService = documentTextService;
    }

    /**
     * Import CoNLL-2012 format file.
     *
     * @param workspaceId the workspace ID
     * @param documentId  the document ID
     * @param file        the CoNLL file to import
     * @return import statistics
     */
    @PostMapping("/workspaces/{workspaceId}/documents/{documentId}/conll")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> importCoNLL(
            @PathVariable UUID workspaceId,
            @PathVariable UUID documentId,
            @RequestParam("file") MultipartFile file) {

        // Extract text from file
        String conllText = documentTextService.extractText(file);

        // Parse and import
        Map<String, Integer> stats = conllParser.parseCoNLL(workspaceId, documentId, conllText);

        return ResponseEntity.ok(
                ApiResponse.success(stats, "CoNLL file imported successfully"));
    }

    /**
     * Export document to CoNLL-2012 format.
     *
     * @param documentId the document ID
     * @return CoNLL formatted text file
     */
    @GetMapping("/documents/{documentId}/conll")
    public ResponseEntity<String> exportDocumentToCoNLL(@PathVariable UUID documentId) {
        String conllText = conllExporter.exportDocument(documentId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"document.conll\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(conllText);
    }

    /**
     * Export workspace to CoNLL-2012 format.
     *
     * @param workspaceId the workspace ID
     * @return CoNLL formatted text file with all documents
     */
    @GetMapping("/workspaces/{workspaceId}/conll")
    public ResponseEntity<String> exportWorkspaceToCoNLL(@PathVariable UUID workspaceId) {
        String conllText = conllExporter.exportWorkspace(workspaceId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"workspace.conll\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(conllText);
    }
}
