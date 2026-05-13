package com.genesis.recommend.service;

import com.genesis.common.exception.UnauthorizedException;
import com.genesis.recommend.dto.RecommendationDto;
import com.genesis.recommend.dto.RecommendationPriority;
import com.genesis.recommend.entity.DismissedRecommendationEntity;
import com.genesis.recommend.repository.DismissedRecommendationRepository;
import com.genesis.recommend.rule.RecommendationRule;
import com.genesis.workspace.repository.WorkspaceMemberRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates recommendation rules.
 *
 * <ol>
 *   <li>Runs each {@link RecommendationRule} bean in registration order.</li>
 *   <li>Per-rule try-catch: a failing rule logs WARN and is skipped; the
 *       caller still gets whatever the other rules produced.</li>
 *   <li>Removes cards whose hash the user has already dismissed.</li>
 *   <li>Deduplicates per documentId — the highest-priority card wins.</li>
 * </ol>
 */
@Service
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

    private final List<RecommendationRule> rules;
    private final DismissedRecommendationRepository dismissalRepository;
    private final WorkspaceMemberRepository memberRepository;

    public RecommendationService(List<RecommendationRule> rules,
            DismissedRecommendationRepository dismissalRepository,
            WorkspaceMemberRepository memberRepository) {
        this.rules = rules;
        this.dismissalRepository = dismissalRepository;
        this.memberRepository = memberRepository;
    }

    @Transactional(readOnly = true)
    public List<RecommendationDto> getRecommendations(UUID workspaceId, UUID callerUserId) {
        requireMember(workspaceId, callerUserId);

        List<RecommendationDto> all = new ArrayList<>();
        for (RecommendationRule rule : rules) {
            try {
                List<RecommendationDto> produced = rule.produce(workspaceId);
                if (produced != null) {
                    all.addAll(produced);
                }
            } catch (Exception ex) {
                log.warn("Recommendation rule {} failed for workspace {}: {}",
                        rule.name(), workspaceId, ex.getMessage(), ex);
            }
        }

        Set<String> dismissedHashes = dismissalRepository
                .findByUserIdAndWorkspaceId(callerUserId, workspaceId).stream()
                .map(DismissedRecommendationEntity::getRecommendationHash)
                .collect(Collectors.toSet());
        List<RecommendationDto> active = new ArrayList<>(all.size());
        for (RecommendationDto card : all) {
            if (!dismissedHashes.contains(card.getHash())) {
                active.add(card);
            }
        }

        return dedupePerDocument(active);
    }

    /**
     * Persist (or update) a dismissal/acceptance for the (user, hash) pair.
     * Idempotent: re-dismissing updates the existing row.
     */
    @Transactional
    public void recordDismissal(UUID workspaceId,
            UUID callerUserId,
            String recommendationHash,
            boolean accepted) {
        requireMember(workspaceId, callerUserId);
        if (recommendationHash == null || recommendationHash.isBlank()) {
            return;
        }

        DismissedRecommendationEntity entity = dismissalRepository
                .findByUserIdAndRecommendationHash(callerUserId, recommendationHash)
                .orElseGet(DismissedRecommendationEntity::new);
        entity.setUserId(callerUserId);
        entity.setWorkspaceId(workspaceId);
        entity.setRecommendationHash(recommendationHash);
        Instant now = Instant.now();
        if (entity.getDismissedAt() == null) {
            entity.setDismissedAt(now);
        }
        entity.setAccepted(accepted);
        if (accepted && entity.getAcceptedAt() == null) {
            entity.setAcceptedAt(now);
        }
        dismissalRepository.save(entity);
    }

    private void requireMember(UUID workspaceId, UUID userId) {
        if (!memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new UnauthorizedException("Not a member of this workspace", true);
        }
    }

    /**
     * If multiple cards point at the same documentId, keep the
     * highest-priority one. UNFINISHED_MENTION (HIGH) beats DENSITY_GAP
     * (MEDIUM) for the same doc — annotators see the more actionable one.
     * Cards with null documentId pass through.
     */
    private List<RecommendationDto> dedupePerDocument(List<RecommendationDto> cards) {
        Map<UUID, RecommendationDto> winnerByDoc = new HashMap<>();
        List<RecommendationDto> unscoped = new ArrayList<>();
        for (RecommendationDto card : cards) {
            UUID docId = card.getDocumentId();
            if (docId == null) {
                unscoped.add(card);
                continue;
            }
            RecommendationDto existing = winnerByDoc.get(docId);
            if (existing == null || priorityRank(card) < priorityRank(existing)) {
                winnerByDoc.put(docId, card);
            }
        }

        List<RecommendationDto> out = new ArrayList<>(winnerByDoc.size() + unscoped.size());
        out.addAll(winnerByDoc.values());
        Set<String> alreadyAdded = new HashSet<>();
        for (RecommendationDto card : out) {
            alreadyAdded.add(card.getHash());
        }
        for (RecommendationDto card : unscoped) {
            if (alreadyAdded.add(card.getHash())) {
                out.add(card);
            }
        }
        return out;
    }

    private static int priorityRank(RecommendationDto card) {
        RecommendationPriority p = card.getPriority();
        if (p == null) return 99;
        return switch (p) {
            case HIGH -> 0;
            case MEDIUM -> 1;
            case LOW -> 2;
        };
    }
}
