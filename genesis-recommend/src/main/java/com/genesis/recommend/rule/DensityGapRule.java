package com.genesis.recommend.rule;

import com.genesis.coref.repository.MentionRepository;
import com.genesis.recommend.dto.RecommendationDto;
import com.genesis.recommend.dto.RecommendationPriority;
import com.genesis.recommend.dto.RecommendationType;
import com.genesis.recommend.util.RecommendationHash;
import com.genesis.workspace.entity.Document;
import com.genesis.workspace.repository.DocumentRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Rule 2 — annotation density imbalance.
 *
 * <p>One MEDIUM-priority card per document in the workspace that has
 * zero mentions. Hash entityId = documentId.
 *
 * <p>Uses {@link MentionRepository#countByDocumentId(UUID)} per document
 * — acceptable at typical workspace sizes. If it becomes hot, swap to a
 * single LEFT JOIN aggregate query.
 */
@Component
public class DensityGapRule implements RecommendationRule {

    private final DocumentRepository documentRepository;
    private final MentionRepository mentionRepository;

    public DensityGapRule(DocumentRepository documentRepository,
            MentionRepository mentionRepository) {
        this.documentRepository = documentRepository;
        this.mentionRepository = mentionRepository;
    }

    @Override
    public String name() {
        return "DENSITY_GAP";
    }

    @Override
    public List<RecommendationDto> produce(UUID workspaceId) {
        List<Document> documents = documentRepository.findByWorkspaceIdOrderByOrderIndexAsc(workspaceId);
        List<RecommendationDto> out = new ArrayList<>();
        for (Document doc : documents) {
            long mentionCount = mentionRepository.countByDocumentId(doc.getId());
            if (mentionCount == 0L) {
                String hash = RecommendationHash.of(
                        RecommendationType.DENSITY_GAP,
                        doc.getId(),
                        doc.getId());
                out.add(new RecommendationDto(
                        hash,
                        RecommendationType.DENSITY_GAP,
                        RecommendationPriority.MEDIUM,
                        doc.getId(),
                        doc.getId(),
                        null,
                        null,
                        "Document \"" + safeName(doc) + "\" has no annotations yet."));
            }
        }
        return out;
    }

    private static String safeName(Document doc) {
        String name = doc.getName();
        return name == null ? "(untitled)" : name;
    }
}
