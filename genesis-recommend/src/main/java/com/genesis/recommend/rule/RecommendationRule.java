package com.genesis.recommend.rule;

import com.genesis.recommend.dto.RecommendationDto;
import java.util.List;
import java.util.UUID;

/**
 * One rule that generates a list of recommendation cards for a workspace.
 *
 * <p>The orchestrator runs rules in priority order with per-rule try-catch
 * so a single failing rule never blocks the rest.
 */
public interface RecommendationRule {

    /** Short stable name for logging/error attribution. */
    String name();

    /**
     * Produce the recommendation cards for the given workspace.
     * Implementations should return an empty list on no-match rather
     * than throwing. The orchestrator catches anything else as WARN.
     */
    List<RecommendationDto> produce(UUID workspaceId);
}
