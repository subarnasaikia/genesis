package com.genesis.api.controller;

import com.genesis.common.event.ActionType;
import com.genesis.common.exception.UnauthorizedException;
import com.genesis.common.response.ApiResponse;
import com.genesis.logging.dto.AnnotationLogResponse;
import com.genesis.logging.service.AnnotationLogService;
import com.genesis.user.entity.User;
import com.genesis.user.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin read API for the annotation_log audit table.
 *
 * <p>Pagination + filters are applied by {@link AnnotationLogService}; the
 * controller is a thin shim that resolves the caller's UUID from the JWT.
 */
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/logs")
public class LogController {

    private final AnnotationLogService logService;
    private final UserRepository userRepository;

    public LogController(AnnotationLogService logService, UserRepository userRepository) {
        this.logService = logService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AnnotationLogResponse>>> getLogs(
            @PathVariable UUID workspaceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(name = "action_type", required = false) ActionType actionType,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        UUID callerId = currentUserId();
        Pageable pageable = PageRequest.of(page, Math.min(size, 200));
        Page<AnnotationLogResponse> result = logService.findLogs(
                workspaceId, callerId, actionType, from, to, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
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
