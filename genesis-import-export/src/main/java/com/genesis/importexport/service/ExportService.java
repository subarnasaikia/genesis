package com.genesis.importexport.service;

import com.genesis.importexport.dto.ExportOptions;
import com.genesis.importexport.dto.ExportOptions.ExportFormat;
import com.genesis.importexport.entity.SentenceEntity;
import com.genesis.importexport.entity.TokenEntity;
import com.genesis.importexport.format.Conll2012Exporter;
import com.genesis.importexport.format.Conll2012Exporter.DocumentExportData;
import com.genesis.importexport.repository.SentenceRepository;
import com.genesis.importexport.repository.TokenRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for exporting documents to CoNLL-2012 format.
 */
@Service
@Transactional(readOnly = true)
public class ExportService {

    private final TokenRepository tokenRepository;
    private final SentenceRepository sentenceRepository;
    private final Conll2012Exporter exporter;

    public ExportService(TokenRepository tokenRepository,
            SentenceRepository sentenceRepository) {
        this.tokenRepository = tokenRepository;
        this.sentenceRepository = sentenceRepository;
        this.exporter = new Conll2012Exporter();
    }

    /**
     * Export result containing content and metadata.
     */
    public static class ExportResult {
        private final byte[] content;
        private final String contentType;
        private final String filename;

        public ExportResult(byte[] content, String contentType, String filename) {
            this.content = content;
            this.contentType = contentType;
            this.filename = filename;
        }

        public byte[] getContent() {
            return content;
        }

        public String getContentType() {
            return contentType;
        }

        public String getFilename() {
            return filename;
        }
    }

    /**
     * Export a single document to CoNLL-2012 format.
     *
     * @param documentId       the document UUID
     * @param documentName     the document name
     * @param corefAnnotations coreference annotations (sentenceIndex-tokenIndex ->
     *                         coref string)
     * @param options          export options
     * @return export result
     */
    public ExportResult exportDocument(UUID documentId, String documentName,
            Map<String, String> corefAnnotations,
            ExportOptions options) {
        return exportDocument(documentId, documentName, corefAnnotations, null, options);
    }

    /**
     * Export a single document, applying per-token POS overrides (e.g. majority
     * vote across annotators). Pass null to fall back to TokenEntity.pos.
     */
    public ExportResult exportDocument(UUID documentId, String documentName,
            Map<String, String> corefAnnotations,
            Map<UUID, String> posOverrides,
            ExportOptions options) {
        List<SentenceEntity> sentences = sentenceRepository.findByDocumentIdOrderBySentenceIndexAsc(documentId);
        Map<Integer, List<TokenEntity>> tokensBySentence = getTokensBySentence(documentId);

        String content = exporter.export(
                documentName,
                sentences,
                tokensBySentence,
                corefAnnotations,
                posOverrides,
                options,
                0 // No offset for single document
        );

        String baseFilename = sanitizeFilename(documentName);
        return new ExportResult(
                content.getBytes(StandardCharsets.UTF_8),
                "text/plain; charset=UTF-8",
                baseFilename + ".conll");
    }

    /**
     * Export multiple documents (workspace) to CoNLL-2012 format.
     *
     * @param documents              list of (documentId, documentName) pairs
     * @param corefAnnotationsPerDoc map of documentId -> coref annotations
     * @param options                export options
     * @param workspaceName          name for the export
     * @return export result (single file or ZIP depending on options)
     */
    public ExportResult exportWorkspace(List<DocumentInfo> documents,
            Map<UUID, Map<String, String>> corefAnnotationsPerDoc,
            ExportOptions options,
            String workspaceName) throws IOException {
        return exportWorkspace(documents, corefAnnotationsPerDoc, null, options, workspaceName);
    }

    /**
     * Export a workspace, applying per-document POS overrides.
     */
    public ExportResult exportWorkspace(List<DocumentInfo> documents,
            Map<UUID, Map<String, String>> corefAnnotationsPerDoc,
            Map<UUID, Map<UUID, String>> posOverridesPerDoc,
            ExportOptions options,
            String workspaceName) throws IOException {
        return exportWorkspace(documents, corefAnnotationsPerDoc, posOverridesPerDoc, null, options, workspaceName);
    }

    /**
     * Export a workspace with POS overrides and a sidecar CSV listing
     * annotator counts per token (consensus confidence). Sidecar is only
     * included in ZIP exports — MERGED_SINGLE_FILE returns plain text.
     */
    public ExportResult exportWorkspace(List<DocumentInfo> documents,
            Map<UUID, Map<String, String>> corefAnnotationsPerDoc,
            Map<UUID, Map<UUID, String>> posOverridesPerDoc,
            Map<UUID, Map<UUID, Long>> annotatorCountsPerDoc,
            ExportOptions options,
            String workspaceName) throws IOException {
        if (documents.isEmpty()) {
            return new ExportResult(
                    new byte[0],
                    "text/plain",
                    workspaceName + ".conll");
        }

        if (options.getExportFormat() == ExportFormat.MERGED_SINGLE_FILE) {
            return exportMerged(documents, corefAnnotationsPerDoc, posOverridesPerDoc, options, workspaceName);
        } else {
            return exportAsZip(documents, corefAnnotationsPerDoc, posOverridesPerDoc,
                    annotatorCountsPerDoc, options, workspaceName);
        }
    }

    /**
     * Export all documents merged into a single CoNLL file.
     */
    private ExportResult exportMerged(List<DocumentInfo> documents,
            Map<UUID, Map<String, String>> corefAnnotationsPerDoc,
            Map<UUID, Map<UUID, String>> posOverridesPerDoc,
            ExportOptions options,
            String workspaceName) {
        List<DocumentExportData> docDataList = new ArrayList<>();

        for (DocumentInfo doc : documents) {
            List<SentenceEntity> sentences = sentenceRepository
                    .findByDocumentIdOrderBySentenceIndexAsc(doc.documentId);
            Map<Integer, List<TokenEntity>> tokensBySentence = getTokensBySentence(doc.documentId);
            Map<String, String> corefAnnotations = corefAnnotationsPerDoc != null
                    ? corefAnnotationsPerDoc.getOrDefault(doc.documentId, new HashMap<>())
                    : new HashMap<>();
            Map<UUID, String> posOverrides = posOverridesPerDoc != null
                    ? posOverridesPerDoc.getOrDefault(doc.documentId, new HashMap<>())
                    : null;

            docDataList.add(new DocumentExportData(
                    doc.documentName,
                    sentences,
                    tokensBySentence,
                    corefAnnotations,
                    posOverrides));
        }

        String content = exporter.exportMerged(docDataList, options);
        String filename = sanitizeFilename(workspaceName) + ".conll";

        return new ExportResult(
                content.getBytes(StandardCharsets.UTF_8),
                "text/plain; charset=UTF-8",
                filename);
    }

    /**
     * Export each document as a separate CoNLL file in a ZIP archive.
     */
    private ExportResult exportAsZip(List<DocumentInfo> documents,
            Map<UUID, Map<String, String>> corefAnnotationsPerDoc,
            Map<UUID, Map<UUID, String>> posOverridesPerDoc,
            Map<UUID, Map<UUID, Long>> annotatorCountsPerDoc,
            ExportOptions options,
            String workspaceName) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            int sentenceOffset = 0;
            // Track used filenames to avoid duplicates
            Map<String, Integer> usedFilenames = new HashMap<>();

            // 1. Add individual files
            for (DocumentInfo doc : documents) {
                List<SentenceEntity> sentences = sentenceRepository
                        .findByDocumentIdOrderBySentenceIndexAsc(doc.documentId);
                Map<Integer, List<TokenEntity>> tokensBySentence = getTokensBySentence(doc.documentId);
                Map<String, String> corefAnnotations = corefAnnotationsPerDoc != null
                        ? corefAnnotationsPerDoc.getOrDefault(doc.documentId, new HashMap<>())
                        : new HashMap<>();
                Map<UUID, String> posOverrides = posOverridesPerDoc != null
                        ? posOverridesPerDoc.getOrDefault(doc.documentId, new HashMap<>())
                        : null;

                String content = exporter.export(
                        doc.documentName,
                        sentences,
                        tokensBySentence,
                        corefAnnotations,
                        posOverrides,
                        options,
                        options.isContinueSentenceNumbers() ? sentenceOffset : 0);

                // Generate unique filename to avoid duplicates
                String baseFilename = sanitizeFilename(doc.documentName);
                String entryName;
                if (usedFilenames.containsKey(baseFilename)) {
                    int count = usedFilenames.get(baseFilename) + 1;
                    usedFilenames.put(baseFilename, count);
                    entryName = baseFilename + "_" + count + ".conll";
                } else {
                    usedFilenames.put(baseFilename, 1);
                    entryName = baseFilename + ".conll";
                }

                // Add to ZIP
                ZipEntry entry = new ZipEntry(entryName);
                zos.putNextEntry(entry);
                zos.write(content.getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();

                // Update offset for next document
                if (options.isContinueSentenceNumbers()) {
                    sentenceOffset += sentences.size();
                }
            }

            // 2. Add merged file if requested
            if (options.getExportFormat() == ExportFormat.SEPARATE_FILES_ZIP_WITH_MERGED) {
                ExportResult mergedResult = exportMerged(documents, corefAnnotationsPerDoc, posOverridesPerDoc, options, workspaceName);
                ZipEntry entry = new ZipEntry("merged.conll");
                zos.putNextEntry(entry);
                zos.write(mergedResult.getContent());
                zos.closeEntry();
            }

            // 3. Add sidecar CSV with annotator counts (consensus confidence).
            if (annotatorCountsPerDoc != null && !annotatorCountsPerDoc.isEmpty()) {
                String sidecar = buildAnnotatorCountsCsv(documents, annotatorCountsPerDoc);
                if (!sidecar.isEmpty()) {
                    String csvName = sanitizeFilename(workspaceName) + "_annotators.csv";
                    ZipEntry entry = new ZipEntry(csvName);
                    zos.putNextEntry(entry);
                    zos.write(sidecar.getBytes(StandardCharsets.UTF_8));
                    zos.closeEntry();
                }
            }
        }

        String filename = sanitizeFilename(workspaceName) + ".zip";
        return new ExportResult(
                baos.toByteArray(),
                "application/zip",
                filename);
    }

    /**
     * Get tokens grouped by sentence index.
     */
    private Map<Integer, List<TokenEntity>> getTokensBySentence(UUID documentId) {
        List<TokenEntity> allTokens = tokenRepository.findByDocumentIdOrderByGlobalIndexAsc(documentId);
        Map<Integer, List<TokenEntity>> map = new HashMap<>();

        for (TokenEntity token : allTokens) {
            map.computeIfAbsent(token.getSentenceIndex(), k -> new ArrayList<>()).add(token);
        }

        return map;
    }

    private String buildAnnotatorCountsCsv(List<DocumentInfo> documents,
            Map<UUID, Map<UUID, Long>> annotatorCountsPerDoc) {
        StringBuilder sb = new StringBuilder();
        sb.append("document_id,token_id,annotator_count\n");
        boolean anyRow = false;
        for (DocumentInfo doc : documents) {
            Map<UUID, Long> counts = annotatorCountsPerDoc.get(doc.documentId);
            if (counts == null || counts.isEmpty()) {
                continue;
            }
            for (Map.Entry<UUID, Long> entry : counts.entrySet()) {
                sb.append(doc.documentId).append(',')
                        .append(entry.getKey()).append(',')
                        .append(entry.getValue()).append('\n');
                anyRow = true;
            }
        }
        return anyRow ? sb.toString() : "";
    }

    private String sanitizeFilename(String name) {
        if (name == null) {
            return "export";
        }
        return name.replaceAll("[^a-zA-Z0-9._-]", "_")
                .replaceAll("\\.[^.]+$", ""); // Remove extension
    }

    /**
     * Document info for export.
     */
    public static class DocumentInfo {
        public UUID documentId;
        public String documentName;

        public DocumentInfo(UUID documentId, String documentName) {
            this.documentId = documentId;
            this.documentName = documentName;
        }
    }
}
