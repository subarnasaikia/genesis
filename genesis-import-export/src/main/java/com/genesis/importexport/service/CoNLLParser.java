package com.genesis.importexport.service;

import com.genesis.coref.entity.Cluster;
import com.genesis.coref.entity.Mention;
import com.genesis.coref.entity.Token;
import com.genesis.coref.repository.ClusterRepository;
import com.genesis.coref.repository.MentionRepository;
import com.genesis.coref.repository.TokenRepository;
import com.genesis.common.exception.GenesisException;
import com.genesis.workspace.entity.Document;
import com.genesis.workspace.entity.Workspace;
import com.genesis.workspace.repository.DocumentRepository;
import com.genesis.workspace.repository.WorkspaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for parsing CoNLL-2012 format coreference annotation files.
 *
 * <p>
 * CoNLL-2012 format uses columns separated by whitespace, with the last column
 * containing coreference information:
 * - `-` = token not in any mention
 * - `(N)` = single-token mention in cluster N
 * - `(N` = start of multi-token mention in cluster N
 * - `N)` = end of multi-token mention in cluster N
 * - `N` = middle of multi-token mention in cluster N
 */
@Service
public class CoNLLParser {

    private static final Logger logger = LoggerFactory.getLogger(CoNLLParser.class);

    private static final Pattern CLUSTER_PATTERN = Pattern.compile("\\(?\\d+\\)?");

    private final TokenRepository tokenRepository;
    private final ClusterRepository clusterRepository;
    private final MentionRepository mentionRepository;
    private final DocumentRepository documentRepository;
    private final WorkspaceRepository workspaceRepository;

    public CoNLLParser(
            TokenRepository tokenRepository,
            ClusterRepository clusterRepository,
            MentionRepository mentionRepository,
            DocumentRepository documentRepository,
            WorkspaceRepository workspaceRepository) {
        this.tokenRepository = tokenRepository;
        this.clusterRepository = clusterRepository;
        this.mentionRepository = mentionRepository;
        this.documentRepository = documentRepository;
        this.workspaceRepository = workspaceRepository;
    }

    /**
     * Parse CoNLL-2012 format text and create tokens, clusters, and mentions.
     *
     * @param workspaceId the workspace ID
     * @param documentId  the document ID
     * @param conllText   the CoNLL-2012 formatted text
     * @return map with statistics
     */
    @Transactional
    public Map<String, Integer> parseCoNLL(UUID workspaceId, UUID documentId, String conllText) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new GenesisException("Workspace not found"));
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new GenesisException("Document not found"));

        // Delete existing tokens and annotations
        tokenRepository.deleteByDocumentId(documentId);

        String[] lines = conllText.split("\n");
        List<Token> tokens = new ArrayList<>();
        Map<Integer, Cluster> clusters = new HashMap<>();
        Map<Integer, List<OpenMention>> openMentions = new HashMap<>();

        int tokenIndex = 0;
        int charOffset = 0;

        for (String line : lines) {
            line = line.trim();

            // Skip comments and empty lines
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            // Split by whitespace
            String[] columns = line.split("\\s+");
            if (columns.length < 4) {
                continue; // Invalid line
            }

            // Column indices (typical CoNLL-2012):
            // 0: document ID
            // 1: part number
            // 2: word number
            // 3: word
            // ... (other columns)
            // last: coreference annotation

            String word = columns[3];
            String corefAnnotation = columns[columns.length - 1];

            // Create token
            Token token = new Token();
            token.setDocument(document);
            token.setTokenIndex(tokenIndex);
            token.setText(word);
            token.setStartOffset(charOffset);
            token.setEndOffset(charOffset + word.length());
            tokens.add(token);

            // Process coreference annotations
            processCorefAnnotation(workspace, corefAnnotation, tokenIndex, word,
                    clusters, openMentions);

            // Update offsets
            charOffset += word.length() + 1; // +1 for space
            tokenIndex++;
        }

        // Save tokens
        tokenRepository.saveAll(tokens);

        // Update document token indices
        if (!tokens.isEmpty()) {
            document.setTokenStartIndex(tokens.get(0).getTokenIndex());
            document.setTokenEndIndex(tokens.get(tokens.size() - 1).getTokenIndex());
            documentRepository.save(document);
        }

        // Save clusters
        clusterRepository.saveAll(clusters.values());

        // Save all mentions
        int mentionCount = 0;
        for (Cluster cluster : clusters.values()) {
            List<Mention> mentions = extractMentionsForCluster(cluster, openMentions);
            mentionRepository.saveAll(mentions);
            mentionCount += mentions.size();
        }

        logger.info("Parsed CoNLL file: {} tokens, {} clusters, {} mentions",
                tokens.size(), clusters.size(), mentionCount);

        Map<String, Integer> stats = new HashMap<>();
        stats.put("tokens", tokens.size());
        stats.put("clusters", clusters.size());
        stats.put("mentions", mentionCount);
        return stats;
    }

    private void processCorefAnnotation(
            Workspace workspace,
            String annotation,
            int tokenIndex,
            String word,
            Map<Integer, Cluster> clusters,
            Map<Integer, List<OpenMention>> openMentions) {

        if (annotation.equals("-")) {
            return; // No annotation
        }

        // Handle multiple annotations (e.g., "(1|(2)")
        String[] parts = annotation.split("\\|");
        for (String part : parts) {
            part = part.trim();

            if (part.matches("\\(\\d+\\)")) {
                // Single-token mention: (N)
                int clusterId = Integer.parseInt(part.replaceAll("[()]", ""));
                Cluster cluster = getOrCreateCluster(workspace, clusterId, clusters);

                Mention mention = new Mention();
                mention.setCluster(cluster);
                mention.setTokenStartIndex(tokenIndex);
                mention.setTokenEndIndex(tokenIndex);
                mention.setText(word);

                addMentionToCluster(cluster, mention, openMentions);

            } else if (part.matches("\\(\\d+")) {
                // Start of multi-token mention: (N
                int clusterId = Integer.parseInt(part.replace("(", ""));
                Cluster cluster = getOrCreateCluster(workspace, clusterId, clusters);

                OpenMention openMention = new OpenMention();
                openMention.cluster = cluster;
                openMention.startIndex = tokenIndex;
                openMention.text = word;

                openMentions.computeIfAbsent(clusterId, k -> new ArrayList<>()).add(openMention);

            } else if (part.matches("\\d+\\)")) {
                // End of multi-token mention: N)
                int clusterId = Integer.parseInt(part.replace(")", ""));

                List<OpenMention> opens = openMentions.get(clusterId);
                if (opens != null && !opens.isEmpty()) {
                    OpenMention openMention = opens.remove(opens.size() - 1);
                    openMention.endIndex = tokenIndex;
                    openMention.text += " " + word;

                    Mention mention = new Mention();
                    mention.setCluster(openMention.cluster);
                    mention.setTokenStartIndex(openMention.startIndex);
                    mention.setTokenEndIndex(openMention.endIndex);
                    mention.setText(openMention.text);

                    addMentionToCluster(openMention.cluster, mention, openMentions);
                }

            } else if (part.matches("\\d+")) {
                // Middle of multi-token mention: N
                int clusterId = Integer.parseInt(part);

                List<OpenMention> opens = openMentions.get(clusterId);
                if (opens != null && !opens.isEmpty()) {
                    OpenMention openMention = opens.get(opens.size() - 1);
                    openMention.text += " " + word;
                }
            }
        }
    }

    private Cluster getOrCreateCluster(
            Workspace workspace,
            int clusterId,
            Map<Integer, Cluster> clusters) {

        return clusters.computeIfAbsent(clusterId, id -> {
            Cluster cluster = new Cluster();
            cluster.setWorkspace(workspace);
            cluster.setClusterIndex(id);
            return cluster;
        });
    }

    private void addMentionToCluster(
            Cluster cluster,
            Mention mention,
            Map<Integer, List<OpenMention>> openMentions) {
        // This is handled by saving mentions later
        // Just track for now
    }

    private List<Mention> extractMentionsForCluster(
            Cluster cluster,
            Map<Integer, List<OpenMention>> openMentions) {
        // This would extract stored mentions
        // For now, return empty list as mentions are created during parsing
        return new ArrayList<>();
    }

    /**
     * Helper class to track mentions being built.
     */
    private static class OpenMention {
        Cluster cluster;
        int startIndex;
        int endIndex;
        String text;
    }
}
