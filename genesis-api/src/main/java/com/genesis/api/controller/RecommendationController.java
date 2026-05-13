package com.genesis.api.controller;

import com.genesis.common.exception.UnauthorizedException;
import com.genesis.common.response.ApiResponse;
import com.genesis.recommend.dto.DismissRecommendationRequest;
import com.genesis.recommend.dto.RecommendationDto;
import com.genesis.recommend.service.RecommendationService;
import com.genesis.user.entity.User;
import com.genesis.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final UserRepository userRepository;

    public RecommendationController(RecommendationService recommendationService,
            UserRepository userRepository) {
        this.recommendationService = recommendationService;
        this.userRepository = userRepository;
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
            @RequestBody DismissRecommendationRequest request) {
        recommendationService.recordDismissal(
                workspaceId,
                currentUserId(),
                request == null ? null : request.getHash(),
                request != null && request.isAccepted());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new UnauthorizedException("Authentication required");
        }
        String name = auth.getName();
        User user = userRepository.findByUsernameOrEmail(name, name)
                .orElseThrow(() -> new UnauthorizedException("User not found: " + name));
        return user.getId();
    }
}
