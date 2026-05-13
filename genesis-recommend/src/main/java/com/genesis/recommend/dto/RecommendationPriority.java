package com.genesis.recommend.dto;

/**
 * Priority for ordering recommendation cards in the sidebar.
 * Higher priority wins during per-document deduplication.
 */
public enum RecommendationPriority {
    HIGH,
    MEDIUM,
    LOW
}
