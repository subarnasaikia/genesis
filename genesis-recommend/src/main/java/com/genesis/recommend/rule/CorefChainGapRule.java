package com.genesis.recommend.rule;

import com.genesis.recommend.dto.RecommendationDto;
import com.genesis.recommend.dto.RecommendationPriority;
import com.genesis.recommend.dto.RecommendationType;
import com.genesis.recommend.repository.ClusterChainProjectionRepository;
import com.genesis.recommend.util.RecommendationHash;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Rule 4 — coreference chain gaps.
 *
 * <p>A cluster has mentions in document order indices, e.g. {1, 2, 4}.
 * If {@code max - min + 1 > distinctDocs} then at least one document in
 * the chain was skipped — flag the cluster for a sanity check.
 *
 * <p>Workspace-wide card (documentId null). Hash entityId = clusterId.
 */
@Component
public class CorefChainGapRule implements RecommendationRule {

    private final ClusterChainProjectionRepository projection;

    public CorefChainGapRule(ClusterChainProjectionRepository projection) {
        this.projection = projection;
    }

    @Override
    public String name() {
        return "COREF_CHAIN_GAP";
    }

    @Override
    public List<RecommendationDto> produce(UUID workspaceId) {
        List<Object[]> rows = projection.findClustersWithChainGaps(workspaceId);
        List<RecommendationDto> out = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            UUID clusterId = (UUID) row[0];
            int minIdx = ((Number) row[1]).intValue();
            int maxIdx = ((Number) row[2]).intValue();
            long distinctDocs = ((Number) row[3]).longValue();
            long missing = (long) (maxIdx - minIdx + 1) - distinctDocs;
            if (missing <= 0) continue; // defensive — the query already filters this

            String hash = RecommendationHash.of(
                    RecommendationType.COREF_CHAIN_GAP,
                    null,
                    clusterId);
            out.add(new RecommendationDto(
                    hash,
                    RecommendationType.COREF_CHAIN_GAP,
                    RecommendationPriority.MEDIUM,
                    null,
                    clusterId,
                    null,
                    null,
                    "Cluster skips " + missing + " document(s) in the chain — check for missed mentions."));
        }
        return out;
    }
}
