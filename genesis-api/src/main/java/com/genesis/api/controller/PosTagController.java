package com.genesis.api.controller;

import com.genesis.api.security.AuthenticatedUserResolver;
import com.genesis.common.response.ApiResponse;
import com.genesis.pos.dto.CreatePosTagRequest;
import com.genesis.pos.dto.PosTagDefinitionDto;
import com.genesis.pos.service.PosTagDefinitionService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * CRUD for custom POS tag definitions. The 17 Universal Dependencies tags are
 * always-available built-ins; this controller manages additive workspace-local
 * and global customs.
 */
@RestController
@RequestMapping("/api/pos-tags")
public class PosTagController {

    private final PosTagDefinitionService definitionService;
    private final AuthenticatedUserResolver userResolver;

    public PosTagController(PosTagDefinitionService definitionService,
            AuthenticatedUserResolver userResolver) {
        this.definitionService = definitionService;
        this.userResolver = userResolver;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PosTagDefinitionDto>> create(
            @Valid @RequestBody CreatePosTagRequest request) {
        PosTagDefinitionDto created = definitionService.create(request, currentUserId());
        return ResponseEntity.ok(ApiResponse.success(created));
    }

    /**
     * Effective POS tag list for the caller. With {@code workspaceId} the
     * response includes built-ins + global customs + that workspace's customs.
     * Without it, only built-ins + global customs.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<PosTagDefinitionDto>>> list(
            @RequestParam(value = "workspaceId", required = false) UUID workspaceId) {
        return ResponseEntity.ok(
                ApiResponse.success(definitionService.listForWorkspace(workspaceId, currentUserId())));
    }

    @DeleteMapping("/{definitionId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID definitionId) {
        definitionService.delete(definitionId, currentUserId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private UUID currentUserId() {
        return userResolver.currentUserId();
    }
}
