package com.genesis.recommend.util;

import static org.junit.jupiter.api.Assertions.*;

import com.genesis.recommend.dto.RecommendationType;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RecommendationHashTest {

    @Test
    @DisplayName("Hash is deterministic for the same inputs")
    void hashIsDeterministic() {
        UUID doc = UUID.randomUUID();
        UUID entity = UUID.randomUUID();
        String a = RecommendationHash.of(RecommendationType.UNFINISHED_MENTION, doc, entity);
        String b = RecommendationHash.of(RecommendationType.UNFINISHED_MENTION, doc, entity);
        assertEquals(a, b);
    }

    @Test
    @DisplayName("Hash output is 64-char lowercase hex")
    void hashShapeIs64HexChars() {
        String hash = RecommendationHash.of(
                RecommendationType.DENSITY_GAP, UUID.randomUUID(), UUID.randomUUID());
        assertEquals(64, hash.length());
        assertTrue(hash.matches("[0-9a-f]+"));
    }

    @Test
    @DisplayName("Different types → different hashes for same ids")
    void differentTypeChangesHash() {
        UUID doc = UUID.randomUUID();
        UUID entity = UUID.randomUUID();
        String a = RecommendationHash.of(RecommendationType.UNFINISHED_MENTION, doc, entity);
        String b = RecommendationHash.of(RecommendationType.DENSITY_GAP, doc, entity);
        assertNotEquals(a, b);
    }

    @Test
    @DisplayName("Different entity ids → different hashes")
    void differentEntityIdChangesHash() {
        UUID doc = UUID.randomUUID();
        String a = RecommendationHash.of(RecommendationType.UNFINISHED_MENTION, doc, UUID.randomUUID());
        String b = RecommendationHash.of(RecommendationType.UNFINISHED_MENTION, doc, UUID.randomUUID());
        assertNotEquals(a, b);
    }
}
