package com.genesis.recommend.rule;

import com.genesis.recommend.dto.RecommendationDto;
import com.genesis.recommend.dto.RecommendationPriority;
import com.genesis.recommend.dto.RecommendationType;
import com.genesis.recommend.repository.TokenFormProjectionRepository;
import com.genesis.recommend.util.RecommendationHash;
import com.genesis.recommend.util.StopWordFilter;
import jakarta.persistence.QueryTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Rule 3 — STRING_MATCH.
 *
 * <p>Surfaces token surface forms that repeat across the workspace and
 * are likely worth annotating (proper nouns, key entities). Filters out
 * function words via {@link StopWordFilter}. Cap surfaced cards at
 * {@code MAX_CARDS} so we don't drown the sidebar in low-signal entries.
 *
 * <p>If the underlying JPQL exceeds its query timeout we return an empty
 * list; the orchestrator's per-rule try-catch still preserves results
 * from other rules.
 */
@Component
public class StringMatchRule implements RecommendationRule {

    private static final Logger log = LoggerFactory.getLogger(StringMatchRule.class);

    /** A form must appear at least this many times to be a candidate. */
    private static final long MIN_OCCURRENCES = 3L;

    /** Hard cap on cards emitted by this rule. */
    private static final int MAX_CARDS = 50;

    private final TokenFormProjectionRepository tokenFormProjection;

    public StringMatchRule(TokenFormProjectionRepository tokenFormProjection) {
        this.tokenFormProjection = tokenFormProjection;
    }

    @Override
    public String name() {
        return "STRING_MATCH";
    }

    @Override
    public List<RecommendationDto> produce(UUID workspaceId) {
        List<Object[]> rows;
        try {
            rows = tokenFormProjection.findRepeatedTokenForms(workspaceId, MIN_OCCURRENCES);
        } catch (QueryTimeoutException ex) {
            log.warn("STRING_MATCH query timed out for workspace {} — returning empty", workspaceId);
            return List.of();
        }

        List<RecommendationDto> out = new ArrayList<>();
        for (Object[] row : rows) {
            if (out.size() >= MAX_CARDS) break;
            String form = (String) row[0];
            long count = ((Number) row[1]).longValue();
            if (StopWordFilter.isStopWord(form)) {
                continue;
            }
            String hash = RecommendationHash.of(
                    RecommendationType.STRING_MATCH,
                    null,
                    form);
            out.add(new RecommendationDto(
                    hash,
                    RecommendationType.STRING_MATCH,
                    RecommendationPriority.LOW,
                    null,
                    null,
                    null,
                    null,
                    "\"" + form + "\" appears " + count + " times — consider annotating."));
        }
        return out;
    }
}
