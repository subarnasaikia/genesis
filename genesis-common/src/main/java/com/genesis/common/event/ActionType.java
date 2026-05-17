package com.genesis.common.event;

/**
 * Audit action types for annotation_log. Each annotation-mutating service
 * publishes an AnnotationLogEvent with one of these types so the audit
 * listener can persist a row for IAA and admin observability.
 *
 * <p>WSD_ANNOTATED is reserved for P2 — wired when genesis-wsd lands.
 */
public enum ActionType {
    MENTION_CREATED,
    MENTION_DELETED,
    MENTION_ASSIGNED,
    CLUSTER_CREATED,
    CLUSTER_MERGED,
    POS_TAGGED,
    WSD_ANNOTATED,
    NER_ANNOTATED,
    NER_DELETED
}
