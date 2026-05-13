package com.genesis.recommend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import com.genesis.common.exception.UnauthorizedException;
import com.genesis.recommend.dto.RecommendationDto;
import com.genesis.recommend.dto.RecommendationPriority;
import com.genesis.recommend.dto.RecommendationType;
import com.genesis.recommend.entity.DismissedRecommendationEntity;
import com.genesis.recommend.repository.DismissedRecommendationRepository;
import com.genesis.recommend.rule.RecommendationRule;
import com.genesis.workspace.repository.WorkspaceMemberRepository;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private DismissedRecommendationRepository dismissalRepository;
    @Mock
    private WorkspaceMemberRepository memberRepository;

    private UUID workspaceId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        workspaceId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    private RecommendationDto card(String hash, RecommendationType type,
            RecommendationPriority priority, UUID docId) {
        return new RecommendationDto(hash, type, priority, docId, docId, null, null, "test");
    }

    private RecommendationRule stubRule(String name, List<RecommendationDto> cards) {
        return new RecommendationRule() {
            @Override public String name() { return name; }
            @Override public List<RecommendationDto> produce(UUID workspaceId) { return cards; }
        };
    }

    private RecommendationRule throwingRule(String name) {
        return new RecommendationRule() {
            @Override public String name() { return name; }
            @Override public List<RecommendationDto> produce(UUID workspaceId) {
                throw new RuntimeException("boom");
            }
        };
    }

    @Test
    @DisplayName("Non-member → 403 (forbidden)")
    void nonMember_isForbidden() {
        when(memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)).thenReturn(false);
        RecommendationService service = new RecommendationService(
                Collections.emptyList(), dismissalRepository, memberRepository);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> service.getRecommendations(workspaceId, userId));
        assertTrue(ex.isForbidden());
    }

    @Test
    @DisplayName("Failing rule is isolated — other rules still run")
    void failingRuleIsolated() {
        when(memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)).thenReturn(true);
        when(dismissalRepository.findByUserIdAndWorkspaceId(userId, workspaceId)).thenReturn(Collections.emptyList());

        UUID docId = UUID.randomUUID();
        RecommendationDto good = card("hash-good", RecommendationType.DENSITY_GAP,
                RecommendationPriority.MEDIUM, docId);

        RecommendationService service = new RecommendationService(
                List.of(throwingRule("R-fail"), stubRule("R-ok", List.of(good))),
                dismissalRepository, memberRepository);

        List<RecommendationDto> out = service.getRecommendations(workspaceId, userId);
        assertEquals(1, out.size());
        assertEquals("hash-good", out.get(0).getHash());
    }

    @Test
    @DisplayName("Dismissed hashes are filtered out")
    void dismissedHashesFiltered() {
        when(memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)).thenReturn(true);

        UUID docA = UUID.randomUUID();
        UUID docB = UUID.randomUUID();
        RecommendationDto keepCard = card("hash-keep", RecommendationType.DENSITY_GAP,
                RecommendationPriority.MEDIUM, docA);
        RecommendationDto dismissedCard = card("hash-dismiss", RecommendationType.DENSITY_GAP,
                RecommendationPriority.MEDIUM, docB);

        DismissedRecommendationEntity row = new DismissedRecommendationEntity();
        row.setRecommendationHash("hash-dismiss");
        when(dismissalRepository.findByUserIdAndWorkspaceId(userId, workspaceId)).thenReturn(List.of(row));

        RecommendationService service = new RecommendationService(
                List.of(stubRule("R", List.of(keepCard, dismissedCard))),
                dismissalRepository, memberRepository);

        List<RecommendationDto> out = service.getRecommendations(workspaceId, userId);
        assertEquals(1, out.size());
        assertEquals("hash-keep", out.get(0).getHash());
    }

    @Test
    @DisplayName("Same documentId → highest priority wins")
    void dedupKeepsHighestPriority() {
        when(memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)).thenReturn(true);
        when(dismissalRepository.findByUserIdAndWorkspaceId(userId, workspaceId)).thenReturn(Collections.emptyList());

        UUID docId = UUID.randomUUID();
        RecommendationDto medium = card("hash-mid", RecommendationType.DENSITY_GAP,
                RecommendationPriority.MEDIUM, docId);
        RecommendationDto high = card("hash-high", RecommendationType.UNFINISHED_MENTION,
                RecommendationPriority.HIGH, docId);

        RecommendationService service = new RecommendationService(
                List.of(stubRule("R", List.of(medium, high))),
                dismissalRepository, memberRepository);

        List<RecommendationDto> out = service.getRecommendations(workspaceId, userId);
        assertEquals(1, out.size());
        assertEquals("hash-high", out.get(0).getHash(),
                "HIGH-priority card must beat MEDIUM-priority card for the same document");
    }
}
