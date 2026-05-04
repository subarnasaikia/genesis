package com.genesis.coref.listener;

import com.genesis.coref.entity.ClusterEntity;
import com.genesis.coref.entity.MentionEntity;
import com.genesis.coref.repository.ClusterRepository;
import com.genesis.coref.repository.MentionRepository;
import com.genesis.importexport.entity.TokenEntity;
import com.genesis.importexport.event.ConllImportedEvent;
import com.genesis.importexport.format.Conll2012Parser.MentionSpan;
import com.genesis.importexport.repository.TokenRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists CoNLL-imported mention spans + clusters into the coref tables.
 *
 * <p>For each unique CoNLL cluster id encountered, allocates a workspace-scoped
 * {@link ClusterEntity} (preserving the original CoNLL id as the cluster
 * number when free, otherwise falling back to the next free number and storing
 * the original in {@code label}). Then persists one {@link MentionEntity} per
 * span, linked to that cluster.
 */
@Component
public class ConllMentionImportListener {

    private static final Logger log = LoggerFactory.getLogger(ConllMentionImportListener.class);

    private static final String[] CLUSTER_COLORS = {
            "#3b82f6", "#ef4444", "#10b981", "#f59e0b", "#8b5cf6",
            "#ec4899", "#14b8a6", "#f97316", "#06b6d4", "#84cc16"
    };

    private final ClusterRepository clusterRepository;
    private final MentionRepository mentionRepository;
    private final TokenRepository tokenRepository;

    public ConllMentionImportListener(ClusterRepository clusterRepository,
            MentionRepository mentionRepository,
            TokenRepository tokenRepository) {
        this.clusterRepository = clusterRepository;
        this.mentionRepository = mentionRepository;
        this.tokenRepository = tokenRepository;
    }

    @EventListener
    @Transactional
    public void onConllImported(ConllImportedEvent event) {
        UUID documentId = event.getDocumentId();
        UUID workspaceId = event.getWorkspaceId();
        List<MentionSpan> spans = event.getMentionSpans();

        if (spans == null || spans.isEmpty()) {
            log.debug("CoNLL import for doc {} carried no mentions", documentId);
            return;
        }

        // Wipe any prior mentions on this document (re-import path)
        mentionRepository.deleteByDocumentId(documentId);

        Map<Integer, ClusterEntity> clusterByConllId = new HashMap<>();
        Map<Integer, Integer> mentionCountByConllId = new HashMap<>();

        for (MentionSpan span : spans) {
            int conllId = span.getClusterId();
            mentionCountByConllId.merge(conllId, 1, Integer::sum);
            clusterByConllId.computeIfAbsent(conllId, id -> resolveOrCreateCluster(workspaceId, id));
        }

        // Cache tokens per sentence to compute mention text
        Map<Integer, List<TokenEntity>> tokensBySentence = new HashMap<>();

        List<MentionEntity> mentions = new ArrayList<>(spans.size());
        for (MentionSpan span : spans) {
            ClusterEntity cluster = clusterByConllId.get(span.getClusterId());
            if (cluster == null) continue;

            MentionEntity m = new MentionEntity();
            m.setWorkspaceId(workspaceId);
            m.setDocumentId(documentId);
            m.setClusterId(cluster.getId());
            m.setSentenceIndex(span.getSentenceIndex());
            m.setStartTokenIndex(span.getStartTokenIndex());
            m.setEndTokenIndex(span.getEndTokenIndex());
            m.setText(extractMentionText(documentId, span, tokensBySentence));
            mentions.add(m);
        }
        mentionRepository.saveAll(mentions);

        // Update mention counts on the touched clusters
        clusterByConllId.forEach((conllId, cluster) -> {
            int delta = mentionCountByConllId.getOrDefault(conllId, 0);
            int prev = cluster.getMentionCount() == null ? 0 : cluster.getMentionCount();
            cluster.setMentionCount(prev + delta);
        });
        clusterRepository.saveAll(clusterByConllId.values());

        log.info("CoNLL import: persisted {} mentions across {} clusters for doc {}",
                mentions.size(), clusterByConllId.size(), documentId);
    }

    private ClusterEntity resolveOrCreateCluster(UUID workspaceId, int conllId) {
        return clusterRepository.findByWorkspaceIdAndClusterNumber(workspaceId, conllId)
                .orElseGet(() -> createCluster(workspaceId, conllId));
    }

    private ClusterEntity createCluster(UUID workspaceId, int conllId) {
        int targetNumber = conllId;
        if (clusterRepository.existsByWorkspaceIdAndClusterNumber(workspaceId, targetNumber)) {
            targetNumber = clusterRepository.getNextClusterNumber(workspaceId);
        }
        ClusterEntity cluster = new ClusterEntity();
        cluster.setWorkspaceId(workspaceId);
        cluster.setClusterNumber(targetNumber);
        cluster.setLabel("CoNLL #" + conllId);
        cluster.setColor(CLUSTER_COLORS[Math.floorMod(conllId, CLUSTER_COLORS.length)]);
        cluster.setMentionCount(0);
        return clusterRepository.save(cluster);
    }

    private String extractMentionText(UUID documentId, MentionSpan span,
            Map<Integer, List<TokenEntity>> cache) {
        List<TokenEntity> sentTokens = cache.computeIfAbsent(span.getSentenceIndex(),
                idx -> tokenRepository.findByDocumentIdAndSentenceIndexOrderByTokenIndexAsc(documentId, idx));
        StringBuilder sb = new StringBuilder();
        for (TokenEntity t : sentTokens) {
            int idx = t.getTokenIndex() == null ? -1 : t.getTokenIndex();
            if (idx >= span.getStartTokenIndex() && idx <= span.getEndTokenIndex()) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(t.getForm());
            }
        }
        String text = sb.toString();
        return text.length() > 2000 ? text.substring(0, 2000) : text;
    }
}
