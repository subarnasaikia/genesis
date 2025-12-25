package com.genesis.importexport.service;

import com.genesis.coref.entity.Cluster;
import com.genesis.coref.entity.Mention;
import com.genesis.coref.entity.Token;
import com.genesis.coref.repository.ClusterRepository;
import com.genesis.coref.repository.MentionRepository;
import com.genesis.coref.repository.TokenRepository;
import com.genesis.workspace.entity.Document;
import com.genesis.workspace.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for exporting annotations to CoNLL-2012 format.
 *
 * <p>
 * CoNLL-2012 format output structure:
 * document_id part_num word_num word ... coref_annotation
 */
@Service
public class CoNLLExporter {

    private static final Logger logger = LoggerFactory.getLogger(CoNLLExporter.class);

    private final TokenRepository tokenRepository;
    private final ClusterRepository clusterRepository;
    private final MentionRepository mentionRepository;
    private final DocumentRepository documentRepository;

    public CoNLLExporter(
            TokenRepository tokenRepository,
            ClusterRepository clusterRepository,
            MentionRepository mentionRepository,
            DocumentRepository documentRepository) {
        this.tokenRepository = tokenRepository;
        this.clusterRepository = clusterRepository;
        this.mentionRepository = mentionRepository;
        this.documentRepository = documentRepository;
    }

    /**
     * Export a document with its annotations to CoNLL-2012 format.
     *
     * @param documentId the document ID
     * @return CoNLL-2012 formatted string
     */
    public String exportDocument(UUID documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));

        // Get all tokens for the document
        List<Token> tokens = tokenRepository.findByDocumentIdOrderByTokenIndexAsc(documentId);

        if (tokens.isEmpty()) {
            return ""; // No tokens to export
        }

        // Get all clusters for the workspace
        List<Cluster> clusters = clusterRepository.findByWorkspaceId(
                document.getWorkspace().getId());

        // Build mention index: token_index -> list of cluster indices
        Map<Integer, List<MentionAnnotation>> tokenAnnotations = buildMentionIndex(
                clusters, tokens);

        // Build CoNLL output
        StringBuilder output = new StringBuilder();
        String docId = document.getName().replaceAll("\\..*$", ""); // Remove extension
        int partNum = 0;

        output.append("#begin document (").append(docId).append("); part ").append(partNum).append("\n");

        for (Token token : tokens) {
            int localWordNum = token.getTokenIndex() - tokens.get(0).getTokenIndex();

            // Build line: doc_id part_num word_num word ... coref
            output.append(docId).append("\t")
                    .append(partNum).append("\t")
                    .append(localWordNum).append("\t")
                    .append(token.getText()).append("\t");

            // Add placeholder columns (POS, parse, etc.)
            output.append("-\t-\t-\t-\t-\t-\t");

            // Add coreference annotation
            String coref = buildCorefAnnotation(token.getTokenIndex(), tokenAnnotations);
            output.append(coref);

            output.append("\n");
        }

        output.append("#end document\n");

        logger.info("Exported document {} to CoNLL format ({} tokens)",
                documentId, tokens.size());

        return output.toString();
    }

    /**
     * Export all documents in a workspace to CoNLL-2012 format.
     *
     * @param workspaceId the workspace ID
     * @return CoNLL-2012 formatted string with all documents
     */
    public String exportWorkspace(UUID workspaceId) {
        List<Document> documents = documentRepository.findByWorkspaceIdOrderByOrderIndexAsc(workspaceId);

        StringBuilder output = new StringBuilder();
        for (Document document : documents) {
            output.append(exportDocument(document.getId()));
            output.append("\n");
        }

        logger.info("Exported workspace {} to CoNLL format ({} documents)",
                workspaceId, documents.size());

        return output.toString();
    }

    private Map<Integer, List<MentionAnnotation>> buildMentionIndex(
            List<Cluster> clusters,
            List<Token> tokens) {

        Map<Integer, List<MentionAnnotation>> index = new HashMap<>();

        for (Cluster cluster : clusters) {
            List<Mention> mentions = mentionRepository.findByClusterId(cluster.getId());

            for (Mention mention : mentions) {
                int start = mention.getTokenStartIndex();
                int end = mention.getTokenEndIndex();

                if (start == end) {
                    // Single-token mention
                    MentionAnnotation annotation = new MentionAnnotation();
                    annotation.clusterId = cluster.getClusterIndex();
                    annotation.type = MentionType.SINGLE;
                    index.computeIfAbsent(start, k -> new ArrayList<>()).add(annotation);
                } else {
                    // Multi-token mention
                    for (int i = start; i <= end; i++) {
                        MentionAnnotation annotation = new MentionAnnotation();
                        annotation.clusterId = cluster.getClusterIndex();

                        if (i == start) {
                            annotation.type = MentionType.START;
                        } else if (i == end) {
                            annotation.type = MentionType.END;
                        } else {
                            annotation.type = MentionType.MIDDLE;
                        }

                        index.computeIfAbsent(i, k -> new ArrayList<>()).add(annotation);
                    }
                }
            }
        }

        return index;
    }

    private String buildCorefAnnotation(
            int tokenIndex,
            Map<Integer, List<MentionAnnotation>> tokenAnnotations) {

        List<MentionAnnotation> annotations = tokenAnnotations.get(tokenIndex);

        if (annotations == null || annotations.isEmpty()) {
            return "-";
        }

        // Build annotation string
        List<String> parts = new ArrayList<>();

        for (MentionAnnotation annotation : annotations) {
            switch (annotation.type) {
                case SINGLE:
                    parts.add("(" + annotation.clusterId + ")");
                    break;
                case START:
                    parts.add("(" + annotation.clusterId);
                    break;
                case END:
                    parts.add(annotation.clusterId + ")");
                    break;
                case MIDDLE:
                    parts.add(String.valueOf(annotation.clusterId));
                    break;
            }
        }

        return String.join("|", parts);
    }

    private static class MentionAnnotation {
        int clusterId;
        MentionType type;
    }

    private enum MentionType {
        SINGLE,
        START,
        MIDDLE,
        END
    }
}
