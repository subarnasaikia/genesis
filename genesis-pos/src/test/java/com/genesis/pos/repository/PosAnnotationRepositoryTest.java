package com.genesis.pos.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.genesis.pos.config.PosTestConfiguration;
import com.genesis.pos.entity.PosAnnotationEntity;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * Repository tests for PosAnnotationRepository.
 *
 * <p>Verifies majority-vote and annotator-count aggregations against a
 * real database (H2 via @DataJpaTest).
 */
@DataJpaTest
@ContextConfiguration(classes = PosTestConfiguration.class)
class PosAnnotationRepositoryTest {

    @Autowired
    private PosAnnotationRepository repository;

    private UUID documentId;
    private UUID tokenA;
    private UUID tokenB;

    @BeforeEach
    void setUp() {
        documentId = UUID.randomUUID();
        tokenA = UUID.randomUUID();
        tokenB = UUID.randomUUID();
    }

    private void save(UUID tokenId, String annotatorId, String posTag) {
        PosAnnotationEntity e = new PosAnnotationEntity();
        e.setTokenId(tokenId);
        e.setDocumentId(documentId);
        e.setAnnotatorId(annotatorId);
        e.setPosTag(posTag);
        repository.saveAndFlush(e);
    }

    @Test
    @DisplayName("majority winner: 2 NOUN vs 1 VERB → NOUN first")
    void majorityWinnerByVoteCount() {
        save(tokenA, "alice", "NOUN");
        save(tokenA, "bob", "NOUN");
        save(tokenA, "carol", "VERB");

        List<Object[]> rows = repository.findPosCountsByDocumentId(documentId);

        assertFalse(rows.isEmpty());
        Object[] first = rows.get(0);
        assertEquals(tokenA, first[0]);
        assertEquals("NOUN", first[1]);
        assertEquals(2L, ((Number) first[2]).longValue());
    }

    @Test
    @DisplayName("tie broken by most recent timestamp")
    void tieBrokenByMostRecentTimestamp() throws InterruptedException {
        save(tokenA, "alice", "NOUN");
        Thread.sleep(10);
        save(tokenA, "bob", "VERB");

        List<Object[]> rows = repository.findPosCountsByDocumentId(documentId);

        Object[] first = rows.get(0);
        assertEquals(tokenA, first[0]);
        assertEquals("VERB", first[1], "more recent tag wins on a tie");
    }

    @Test
    @DisplayName("distinct annotator count per token")
    void annotatorCountReturnsDistinctAnnotators() {
        save(tokenA, "alice", "NOUN");
        save(tokenA, "bob", "NOUN");
        save(tokenA, "carol", "VERB");
        save(tokenB, "alice", "ADJ");

        List<Object[]> rows = repository.findAnnotatorCountsByDocumentId(documentId);

        assertEquals(2, rows.size());
        for (Object[] row : rows) {
            UUID tokenId = (UUID) row[0];
            long count = ((Number) row[1]).longValue();
            if (tokenId.equals(tokenA)) {
                assertEquals(3L, count);
            } else if (tokenId.equals(tokenB)) {
                assertEquals(1L, count);
            } else {
                fail("unexpected token id: " + tokenId);
            }
        }
    }

    @Test
    @DisplayName("unique (token_id, annotator_id) blocks duplicate inserts")
    void uniqueIndexPreventsDuplicateAnnotatorRow() {
        save(tokenA, "alice", "NOUN");

        PosAnnotationEntity dup = new PosAnnotationEntity();
        dup.setTokenId(tokenA);
        dup.setDocumentId(documentId);
        dup.setAnnotatorId("alice");
        dup.setPosTag("VERB");

        assertThrows(Exception.class, () -> repository.saveAndFlush(dup),
                "unique (token_id, annotator_id) must prevent duplicate inserts");
    }
}
