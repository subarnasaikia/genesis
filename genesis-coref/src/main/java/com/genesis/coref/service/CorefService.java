package com.genesis.coref.service;

import com.genesis.coref.entity.Cluster;
import com.genesis.coref.entity.Mention;
import com.genesis.coref.entity.Token;
import com.genesis.coref.repository.ClusterRepository;
import com.genesis.coref.repository.MentionRepository;
import com.genesis.coref.repository.TokenRepository;
import com.genesis.common.exception.ResourceNotFoundException;
import com.genesis.workspace.entity.Document;
import com.genesis.workspace.entity.Workspace;
import com.genesis.workspace.repository.DocumentRepository;
import com.genesis.workspace.repository.WorkspaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for coreference annotation operations.
 *
 * <p>
 * Handles creation and management of clusters and mentions for coreference resolution.
 */
@Service
public class CorefService {

    private static final Logger logger = LoggerFactory.getLogger(CorefService.class);

    private final ClusterRepository clusterRepository;
    private final MentionRepository mentionRepository;
    private final TokenRepository tokenRepository;
    private final WorkspaceRepository workspaceRepository;
    private final DocumentRepository documentRepository;

    public CorefService(
            ClusterRepository clusterRepository,
            MentionRepository mentionRepository,
            TokenRepository tokenRepository,
            WorkspaceRepository workspaceRepository,
            DocumentRepository documentRepository) {
        this.clusterRepository = clusterRepository;
        this.mentionRepository = mentionRepository;
        this.tokenRepository = tokenRepository;
        this.workspaceRepository = workspaceRepository;
        this.documentRepository = documentRepository;
    }

    /**
     * Create a new coreference cluster in a workspace.
     *
     * @param workspaceId the workspace ID
     * @return the created cluster
     */
    @Transactional
    public Cluster createCluster(UUID workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", workspaceId));

        // Get next cluster index
        long count = clusterRepository.countByWorkspaceId(workspaceId);
        int nextIndex = (int) (count + 1);

        Cluster cluster = new Cluster();
        cluster.setWorkspace(workspace);
        cluster.setClusterIndex(nextIndex);

        Cluster saved = clusterRepository.save(cluster);
        logger.info("Created cluster {} in workspace {}", saved.getId(), workspaceId);
        return saved;
    }

    /**
     * Add a mention to a cluster.
     *
     * @param clusterId       the cluster ID
     * @param tokenStartIndex start token index (inclusive)
     * @param tokenEndIndex   end token index (inclusive)
     * @return the created mention
     */
    @Transactional
    public Mention addMentionToCluster(UUID clusterId, int tokenStartIndex, int tokenEndIndex) {
        Cluster cluster = clusterRepository.findById(clusterId)
                .orElseThrow(() -> new ResourceNotFoundException("Cluster", clusterId));

        // Get tokens to construct mention text
        String mentionText = constructMentionText(tokenStartIndex, tokenEndIndex);

        Mention mention = new Mention();
        mention.setCluster(cluster);
        mention.setTokenStartIndex(tokenStartIndex);
        mention.setTokenEndIndex(tokenEndIndex);
        mention.setText(mentionText);

        Mention saved = mentionRepository.save(mention);
        logger.info("Added mention to cluster {}: [{}, {}] = '{}'",
                clusterId, tokenStartIndex, tokenEndIndex, mentionText);
        return saved;
    }

    /**
     * Get all clusters for a workspace.
     *
     * @param workspaceId the workspace ID
     * @return list of clusters
     */
    public List<Cluster> getClustersForWorkspace(UUID workspaceId) {
        if (!workspaceRepository.existsById(workspaceId)) {
            throw new ResourceNotFoundException("Workspace", workspaceId);
        }
        return clusterRepository.findByWorkspaceId(workspaceId);
    }

    /**
     * Get all mentions for a cluster.
     *
     * @param clusterId the cluster ID
     * @return list of mentions ordered by start index
     */
    public List<Mention> getMentionsForCluster(UUID clusterId) {
        if (!clusterRepository.existsById(clusterId)) {
            throw new ResourceNotFoundException("Cluster", clusterId);
        }
        return mentionRepository.findByClusterIdOrderByTokenStartIndexAsc(clusterId);
    }

    /**
     * Delete a mention.
     *
     * @param mentionId the mention ID
     */
    @Transactional
    public void deleteMention(UUID mentionId) {
        if (!mentionRepository.existsById(mentionId)) {
            throw new ResourceNotFoundException("Mention", mentionId);
        }
        mentionRepository.deleteById(mentionId);
        logger.info("Deleted mention {}", mentionId);
    }

    /**
     * Delete a cluster and all its mentions.
     *
     * @param clusterId the cluster ID
     */
    @Transactional
    public void deleteCluster(UUID clusterId) {
        if (!clusterRepository.existsById(clusterId)) {
            throw new ResourceNotFoundException("Cluster", clusterId);
        }

        // Delete all mentions first
        mentionRepository.deleteByClusterId(clusterId);

        // Delete the cluster
        clusterRepository.deleteById(clusterId);
        logger.info("Deleted cluster {} and its mentions", clusterId);
    }

    /**
     * Delete all annotations (clusters and mentions) for a workspace.
     *
     * @param workspaceId the workspace ID
     */
    @Transactional
    public void deleteAllAnnotations(UUID workspaceId) {
        if (!workspaceRepository.existsById(workspaceId)) {
            throw new ResourceNotFoundException("Workspace", workspaceId);
        }

        List<Cluster> clusters = clusterRepository.findByWorkspaceId(workspaceId);
        for (Cluster cluster : clusters) {
            mentionRepository.deleteByClusterId(cluster.getId());
        }
        clusterRepository.deleteByWorkspaceId(workspaceId);

        logger.info("Deleted all annotations for workspace {}", workspaceId);
    }

    /**
     * Construct mention text from token indices.
     * This method finds all tokens in the range and concatenates their text.
     *
     * @param tokenStartIndex start token index
     * @param tokenEndIndex   end token index
     * @return the mention text
     */
    private String constructMentionText(int tokenStartIndex, int tokenEndIndex) {
        // For now, we'll need to find tokens by their global index
        // This is a simplified implementation - in production, you'd optimize this
        // by finding the document that contains these token indices

        // This is a placeholder - we'll improve this when we have a better way
        // to query tokens by global index
        return String.format("Token[%d-%d]", tokenStartIndex, tokenEndIndex);
    }

    /**
     * Get all tokens for a document.
     *
     * @param documentId the document ID
     * @return list of tokens
     */
    public List<Token> getTokensForDocument(UUID documentId) {
        if (!documentRepository.existsById(documentId)) {
            throw new ResourceNotFoundException("Document", documentId);
        }
        return tokenRepository.findByDocumentIdOrderByTokenIndexAsc(documentId);
    }
}
