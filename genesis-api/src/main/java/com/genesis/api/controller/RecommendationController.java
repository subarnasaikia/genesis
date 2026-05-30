package com.genesis.api.controller;

import com.genesis.api.security.AuthenticatedUserResolver;
import com.genesis.common.response.ApiResponse;
import com.genesis.recommend.dto.DismissRecommendationRequest;
import com.genesis.recommend.dto.RecommendationDto;
import com.genesis.recommend.service.RecommendationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Recommendation API.
 *
 * <p>The caller UUID is always read from the JWT — never from the request
 * body — so users cannot dismiss recommendations for someone else.
 */
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final AuthenticatedUserResolver userResolver;

    public RecommendationController(RecommendationService recommendationService,
            AuthenticatedUserResolver userResolver) {
        this.recommendationService = recommendationService;
        this.userResolver = userResolver;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RecommendationDto>>> list(
            @PathVariable UUID workspaceId) {
        List<RecommendationDto> cards = recommendationService.getRecommendations(workspaceId, currentUserId());
        return ResponseEntity.ok(ApiResponse.success(cards));
    }

    @PostMapping("/dismissals")
    public ResponseEntity<ApiResponse<Void>> dismiss(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody DismissRecommendationRequest request) {
        recommendationService.recordDismissal(
                workspaceId,
                currentUserId(),
                request == null ? null : request.getHash(),
                request != null && request.isAccepted());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private UUID currentUserId() {
        return userResolver.currentUserId();
    }
}
