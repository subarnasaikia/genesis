package com.genesis.recommend.util;

import com.genesis.recommend.dto.RecommendationType;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Spec for the {@code recommendation_hash} used as the dismissal key
 * (eng-review D7).
 *
 * <pre>
 *   SHA-256( type + ":" + documentId + ":" + entityId )
 * </pre>
 *
 * <p>{@code entityId} is per-rule:
 * <ul>
 *   <li>Rule 1 UNFINISHED_MENTION — mentionId</li>
 *   <li>Rule 2 DENSITY_GAP — documentId (entityId = documentId)</li>
 *   <li>Rule 3 STRING_MATCH — tokenForm (entityId = the surface form)</li>
 *   <li>Rule 4 COREF_CHAIN_GAP — clusterId</li>
 * </ul>
 *
 * <p>Stable across re-imports because UUIDs and surface forms survive
 * tokenize/import cycles.
 */
public final class RecommendationHash {

    private RecommendationHash() {}

    public static String of(RecommendationType type, Object documentId, Object entityId) {
        String input = type.name()
                + ":" + (documentId == null ? "" : documentId.toString())
                + ":" + (entityId == null ? "" : entityId.toString());
        return sha256Hex(input);
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is mandatory in every JDK.
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
