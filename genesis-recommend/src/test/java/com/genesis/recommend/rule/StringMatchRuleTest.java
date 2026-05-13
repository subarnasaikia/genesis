package com.genesis.recommend.rule;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import com.genesis.recommend.dto.RecommendationDto;
import com.genesis.recommend.dto.RecommendationType;
import com.genesis.recommend.repository.TokenFormProjectionRepository;
import jakarta.persistence.QueryTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StringMatchRuleTest {

    @Mock
    private TokenFormProjectionRepository projection;

    private StringMatchRule rule;
    private UUID workspaceId;

    @BeforeEach
    void setUp() {
        rule = new StringMatchRule(projection);
        workspaceId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Assamese particles are filtered out by StopWordFilter")
    void stopWordsExcluded() {
        when(projection.findRepeatedTokenForms(any(), anyLong())).thenReturn(List.of(
                new Object[]{"এই", 50L},   // stop word — must be filtered
                new Object[]{"আৰু", 30L},  // stop word — must be filtered
                new Object[]{"ৰাম", 12L}   // content word — keep
        ));

        List<RecommendationDto> cards = rule.produce(workspaceId);

        assertEquals(1, cards.size());
        assertEquals(RecommendationType.STRING_MATCH, cards.get(0).getType());
        assertTrue(cards.get(0).getReason().contains("ৰাম"));
    }

    @Test
    @DisplayName("QueryTimeoutException → empty list (rule does not crash orchestrator)")
    void queryTimeout_returnsEmpty() {
        when(projection.findRepeatedTokenForms(any(), anyLong()))
                .thenThrow(new QueryTimeoutException("simulated"));

        List<RecommendationDto> cards = rule.produce(workspaceId);
        assertTrue(cards.isEmpty(), "must return empty on timeout, not propagate");
    }

    @Test
    @DisplayName("Cards are capped at MAX_CARDS")
    void cardsAreCapped() {
        List<Object[]> rows = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            rows.add(new Object[]{"word" + i, 5L});
        }
        when(projection.findRepeatedTokenForms(any(), anyLong())).thenReturn(rows);

        List<RecommendationDto> cards = rule.produce(workspaceId);
        assertEquals(50, cards.size());
    }
}
