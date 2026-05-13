package com.genesis.wsd.service;

import com.genesis.common.exception.UnauthorizedException;
import com.genesis.workspace.repository.WorkspaceMemberRepository;
import com.genesis.wsd.repository.WsdAnnotationRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Renders WSD annotation tables to TSV.
 *
 * <p>Two formats:
 * <ul>
 *   <li>Per-annotator — one row per annotation (token_id, word, sense_label, annotator_id).</li>
 *   <li>Consensus — majority sense per token (token_id, word, sense_label, votes).
 *       Ties broken by most recent timestamp via the repository's ORDER BY.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class WsdExportService {

    private final WsdAnnotationRepository annotationRepository;
    private final WorkspaceMemberRepository memberRepository;

    public WsdExportService(WsdAnnotationRepository annotationRepository,
            WorkspaceMemberRepository memberRepository) {
        this.annotationRepository = annotationRepository;
        this.memberRepository = memberRepository;
    }

    public String exportPerAnnotator(UUID workspaceId, UUID callerUserId) {
        requireMember(workspaceId, callerUserId);
        List<Object[]> rows = annotationRepository.findPerAnnotatorExportRows(workspaceId);
        StringBuilder sb = new StringBuilder();
        sb.append("token_id\tword\tsense_label\tannotator_id\n");
        for (Object[] row : rows) {
            sb.append(row[0]).append('\t')
              .append(tsv(row[1])).append('\t')
              .append(tsv(row[2])).append('\t')
              .append(tsv(row[3])).append('\n');
        }
        return sb.toString();
    }

    public String exportConsensus(UUID workspaceId, UUID callerUserId) {
        requireMember(workspaceId, callerUserId);
        List<Object[]> rows = annotationRepository.findConsensusExportRows(workspaceId);
        StringBuilder sb = new StringBuilder();
        sb.append("token_id\tword\tsense_label\tvotes\n");

        // First row per tokenId is the majority winner because the repository
        // orders by (tokenId ASC, COUNT DESC, MAX(timestamp) DESC).
        Set<UUID> seen = new HashSet<>();
        for (Object[] row : rows) {
            UUID tokenId = (UUID) row[0];
            if (!seen.add(tokenId)) {
                continue;
            }
            long votes = ((Number) row[3]).longValue();
            sb.append(tokenId).append('\t')
              .append(tsv(row[1])).append('\t')
              .append(tsv(row[2])).append('\t')
              .append(votes).append('\n');
        }
        return sb.toString();
    }

    private void requireMember(UUID workspaceId, UUID userId) {
        if (!memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new UnauthorizedException("Not a member of this workspace", true);
        }
    }

    /** Replace tab/newline in cell values so TSV stays parseable. */
    private static String tsv(Object value) {
        if (value == null) return "";
        String s = value.toString();
        return s.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
    }
}
