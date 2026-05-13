package com.genesis.recommend.rule;

import com.genesis.coref.entity.MentionEntity;
import com.genesis.coref.repository.MentionRepository;
import com.genesis.recommend.dto.RecommendationDto;
import com.genesis.recommend.dto.RecommendationPriority;
import com.genesis.recommend.dto.RecommendationType;
import com.genesis.recommend.util.RecommendationHash;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Rule 1 — surface mentions without a cluster assignment.
 *
 * <p>Hash entityId = mentionId. One card per unassigned mention,
 * pointing at its document and token range.
 */
@Component
public class UnfinishedMentionsRule implements RecommendationRule {

    private final MentionRepository mentionRepository;

    public UnfinishedMentionsRule(MentionRepository mentionRepository) {
        this.mentionRepository = mentionRepository;
    }

    @Override
    public String name() {
        return "UNFINISHED_MENTION";
    }

    @Override
    public List<RecommendationDto> produce(UUID workspaceId) {
        List<MentionEntity> unassigned = mentionRepository.findByWorkspaceIdAndClusterIdIsNull(workspaceId);
        List<RecommendationDto> out = new ArrayList<>(unassigned.size());
        for (MentionEntity m : unassigned) {
            String hash = RecommendationHash.of(
                    RecommendationType.UNFINISHED_MENTION,
                    m.getDocumentId(),
                    m.getId());
            String label = m.getText() == null ? "Mention" : "\"" + m.getText() + "\"";
            out.add(new RecommendationDto(
                    hash,
                    RecommendationType.UNFINISHED_MENTION,
                    RecommendationPriority.HIGH,
                    m.getDocumentId(),
                    m.getId(),
                    m.getStartTokenIndex(),
                    m.getEndTokenIndex(),
                    label + " has no cluster assignment."));
        }
        return out;
    }
}
