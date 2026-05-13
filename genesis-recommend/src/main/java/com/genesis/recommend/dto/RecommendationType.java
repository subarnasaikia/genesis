package com.genesis.recommend.dto;

/**
 * Catalog of recommendation types surfaced to annotators. Stable string
 * names are part of the SHA-256 hash spec — never rename without a hash
 * migration.
 */
public enum RecommendationType {
    /** Rule 1 — mention exists without a cluster. */
    UNFINISHED_MENTION,
    /** Rule 2 — document in an annotated workspace has zero annotations. */
    DENSITY_GAP,
    /** Rule 3 — repeated token form across the workspace. */
    STRING_MATCH,
    /** Rule 4 — coreference chain skips document order. */
    COREF_CHAIN_GAP
}
