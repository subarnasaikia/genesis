package com.genesis.coref.service;

import com.genesis.coref.entity.ClusterEntity;
import com.genesis.coref.entity.MentionEntity;
import com.genesis.coref.repository.ClusterRepository;
import com.genesis.coref.repository.MentionRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

/**
 * Service for coreference annotation operations.
 * Provides methods for CoNLL export integration.
 */
@Service
public class CoreferenceService {

    private final MentionRepository mentionRepository;
    private final ClusterRepository clusterRepository;

    public CoreferenceService(MentionRepository mentionRepository,
            ClusterRepository clusterRepository) {
        this.mentionRepository = mentionRepository;
        this.clusterRepository = clusterRepository;
    }

    /**
     * Generate coreference annotations for CoNLL export.
     * Returns a map of "sentenceIndex-tokenIndex" -> coref annotation string.
     *
     * @param documentId the document ID
     * @return map of token positions to coref annotations
     */
    public Map<String, String> generateCorefAnnotations(@NonNull UUID documentId) {
        List<MentionEntity> mentions = mentionRepository.findByDocumentIdOrdered(documentId);
        Map<String, String> annotations = new HashMap<>();

        for (MentionEntity mention : mentions) {
            if (mention.getClusterId() == null) {
                continue; // Skip unassigned mentions
            }

            // Get cluster number
            ClusterEntity cluster = clusterRepository.findById(mention.getClusterId()).orElse(null);
            if (cluster == null) {
                continue;
            }

            int clusterNum = cluster.getClusterNumber();
            int sentIdx = mention.getSentenceIndex();
            int startToken = mention.getStartTokenIndex();
            int endToken = mention.getEndTokenIndex();

            if (startToken == endToken) {
                // Single-token mention: (N)
                String key = sentIdx + "-" + startToken;
                String existing = annotations.get(key);
                String newAnnotation = "(" + clusterNum + ")";
                annotations.put(key, mergeAnnotations(existing, newAnnotation));
            } else {
                // Multi-token mention: (N at start, N) at end
                String startKey = sentIdx + "-" + startToken;
                String endKey = sentIdx + "-" + endToken;

                String startAnnotation = "(" + clusterNum;
                String endAnnotation = clusterNum + ")";

                annotations.put(startKey, mergeAnnotations(annotations.get(startKey), startAnnotation));
                annotations.put(endKey, mergeAnnotations(annotations.get(endKey), endAnnotation));
            }
        }

        return annotations;
    }

    /**
     * Generate coreference annotations for all documents in a workspace.
     * Returns a map of documentId -> (tokenPosition -> coref annotation).
     *
     * @param workspaceId the workspace ID
     * @return nested map of document and token positions to coref annotations
     */
    public Map<UUID, Map<String, String>> generateWorkspaceCorefAnnotations(@NonNull UUID workspaceId) {
        Map<UUID, Map<String, String>> result = new HashMap<>();
        List<MentionEntity> allMentions = mentionRepository.findByWorkspaceId(workspaceId);

        for (MentionEntity mention : allMentions) {
            if (mention.getClusterId() == null) {
                continue;
            }

            UUID docId = mention.getDocumentId();
            result.computeIfAbsent(docId, k -> new HashMap<>());

            ClusterEntity cluster = clusterRepository.findById(mention.getClusterId()).orElse(null);
            if (cluster == null) {
                continue;
            }

            int clusterNum = cluster.getClusterNumber();
            int sentIdx = mention.getSentenceIndex();
            int startToken = mention.getStartTokenIndex();
            int endToken = mention.getEndTokenIndex();

            Map<String, String> docAnnotations = result.get(docId);

            if (startToken == endToken) {
                String key = sentIdx + "-" + startToken;
                String existing = docAnnotations.get(key);
                String newAnnotation = "(" + clusterNum + ")";
                docAnnotations.put(key, mergeAnnotations(existing, newAnnotation));
            } else {
                String startKey = sentIdx + "-" + startToken;
                String endKey = sentIdx + "-" + endToken;

                String startAnnotation = "(" + clusterNum;
                String endAnnotation = clusterNum + ")";

                docAnnotations.put(startKey, mergeAnnotations(docAnnotations.get(startKey), startAnnotation));
                docAnnotations.put(endKey, mergeAnnotations(docAnnotations.get(endKey), endAnnotation));
            }
        }

        return result;
    }

    /**
     * Get annotation statistics for a workspace.
     */
    public AnnotationStats getStats(@NonNull UUID workspaceId) {
        long mentionCount = mentionRepository.countByWorkspaceId(workspaceId);
        long clusterCount = clusterRepository.countByWorkspaceId(workspaceId);
        long unassignedCount = mentionRepository.findByWorkspaceIdAndClusterIdIsNull(workspaceId).size();

        return new AnnotationStats(mentionCount, clusterCount, unassignedCount);
    }

    /**
     * Merge multiple annotations on the same token using pipe separator.
     */
    private String mergeAnnotations(String existing, String newAnnotation) {
        if (existing == null || existing.isEmpty()) {
            return newAnnotation;
        }
        return existing + "|" + newAnnotation;
    }

    /**
     * Annotation statistics.
     */
    public static class AnnotationStats {
        private final long mentionCount;
        private final long clusterCount;
        private final long unassignedCount;

        public AnnotationStats(long mentionCount, long clusterCount, long unassignedCount) {
            this.mentionCount = mentionCount;
            this.clusterCount = clusterCount;
            this.unassignedCount = unassignedCount;
        }

        public long getMentionCount() {
            return mentionCount;
        }

        public long getClusterCount() {
            return clusterCount;
        }

        public long getUnassignedCount() {
            return unassignedCount;
        }
    }
}
