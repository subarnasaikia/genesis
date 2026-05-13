package com.genesis.recommend.util;

import java.util.Set;

/**
 * Hardcoded Assamese stop-word filter for Rule 3 (STRING_MATCH).
 *
 * <p>The repeated-token-form rule would otherwise surface every common
 * particle ("আৰু", "এই", etc.) as a "candidate" mention. The filter
 * removes the most frequent function words / pronouns / demonstratives
 * so the rule highlights content tokens worth annotating.
 *
 * <p>Order of magnitude: ~20 entries. Conservative on purpose — we'd
 * rather miss-include a borderline word than silently swallow a useful
 * content noun.
 */
public final class StopWordFilter {

    private StopWordFilter() {}

    private static final Set<String> ASSAMESE_STOP_WORDS = Set.of(
            // Demonstratives
            "এই", "সেই", "ই",
            // Locative particles
            "ত", "ৰ", "ক", "লৈ",
            // Coordinating conjunctions
            "আৰু", "কিন্তু", "অথবা",
            // Subordinating conjunctions / interrogatives
            "যদি", "যেতিয়া", "যিহেতু", "যি",
            // Common pronouns
            "তেওঁ", "মই", "আমি", "তুমি",
            // Common auxiliaries / verbs
            "হয়", "নাই", "আছিল"
    );

    public static boolean isStopWord(String word) {
        if (word == null) return false;
        return ASSAMESE_STOP_WORDS.contains(word.trim());
    }

    /** Exposed for tests + telemetry. */
    public static Set<String> stopWords() {
        return ASSAMESE_STOP_WORDS;
    }
}
