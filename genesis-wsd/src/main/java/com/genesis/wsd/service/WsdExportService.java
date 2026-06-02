package com.genesis.wsd.service;

import com.genesis.common.port.TokenQueryPort;
import com.genesis.workspace.service.WorkspaceAccessControl;
import com.genesis.wsd.repository.WsdAnnotationRepository;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
 *
 * <p>The repository queries return rows without the surface form ("word") so they
 * do not depend on {@code com.genesis.importexport}'s entity package; this service
 * fills it in via {@link TokenQueryPort}, batching the lookups so the export
 * stays one round trip per distinct token rather than one per annotation row.
 */
@Service
@Transactional(readOnly = true)
public class WsdExportService {

    private final WsdAnnotationRepository annotationRepository;
    private final TokenQueryPort tokenQuery;
    private final WorkspaceAccessControl accessControl;

    public WsdExportService(WsdAnnotationRepository annotationRepository,
            TokenQueryPort tokenQuery,
            WorkspaceAccessControl accessControl) {
        this.annotationRepository = annotationRepository;
        this.tokenQuery = tokenQuery;
        this.accessControl = accessControl;
    }

    public String exportPerAnnotator(UUID workspaceId, UUID callerUserId) {
        accessControl.requireMember(workspaceId, callerUserId);
        List<Object[]> rows = annotationRepository.findPerAnnotatorExportRows(workspaceId);
        Map<UUID, String> formByToken = resolveForms(rows);
        StringBuilder sb = new StringBuilder();
        sb.append("token_id\tword\tsense_label\tannotator_id\n");
        for (Object[] row : rows) {
            UUID tokenId = (UUID) row[0];
            sb.append(tokenId).append('\t')
              .append(tsv(formByToken.get(tokenId))).append('\t')
              .append(tsv(row[1])).append('\t')
              .append(tsv(row[2])).append('\n');
        }
        return sb.toString();
    }

    public String exportConsensus(UUID workspaceId, UUID callerUserId) {
        accessControl.requireMember(workspaceId, callerUserId);
        List<Object[]> rows = annotationRepository.findConsensusExportRows(workspaceId);
        Map<UUID, String> formByToken = resolveForms(rows);
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
            long votes = ((Number) row[2]).longValue();
            sb.append(tokenId).append('\t')
              .append(tsv(formByToken.get(tokenId))).append('\t')
              .append(tsv(row[1])).append('\t')
              .append(votes).append('\n');
        }
        return sb.toString();
    }

    private Map<UUID, String> resolveForms(List<Object[]> rows) {
        Map<UUID, String> out = new HashMap<>();
        for (Object[] row : rows) {
            UUID tokenId = (UUID) row[0];
            if (out.containsKey(tokenId)) continue;
            out.put(tokenId, tokenQuery.formForToken(tokenId));
        }
        return out;
    }

    /** Replace tab/newline in cell values so TSV stays parseable. */
    private static String tsv(Object value) {
        if (value == null) return "";
        String s = value.toString();
        return s.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
    }
}
