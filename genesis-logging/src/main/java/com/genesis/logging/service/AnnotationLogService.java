package com.genesis.logging.service;

import com.genesis.common.event.ActionType;
import com.genesis.common.exception.UnauthorizedException;
import com.genesis.logging.dto.AnnotationLogResponse;
import com.genesis.logging.repository.AnnotationLogRepository;
import com.genesis.workspace.entity.MemberRole;
import com.genesis.workspace.entity.WorkspaceMember;
import com.genesis.workspace.repository.WorkspaceMemberRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-side service for {@code annotation_log}. Only workspace admins may
 * query. Lookback is capped at 7 days to keep the query path small and to
 * avoid leaking historic activity beyond the audit window.
 */
@Service
@Transactional(readOnly = true)
public class AnnotationLogService {

    public static final Duration MAX_LOOKBACK = Duration.ofDays(7);

    private final AnnotationLogRepository logRepository;
    private final WorkspaceMemberRepository memberRepository;

    public AnnotationLogService(AnnotationLogRepository logRepository,
            WorkspaceMemberRepository memberRepository) {
        this.logRepository = logRepository;
        this.memberRepository = memberRepository;
    }

    public Page<AnnotationLogResponse> findLogs(UUID workspaceId,
            UUID callerUserId,
            ActionType actionType,
            Instant from,
            Instant to,
            Pageable pageable) {
        requireAdmin(workspaceId, callerUserId);

        Instant now = Instant.now();
        Instant earliestAllowed = now.minus(MAX_LOOKBACK);
        Instant effectiveFrom = (from == null || from.isBefore(earliestAllowed)) ? earliestAllowed : from;
        Instant effectiveTo = (to == null) ? now : to;

        return logRepository
                .findFiltered(workspaceId, actionType, effectiveFrom, effectiveTo, pageable)
                .map(AnnotationLogResponse::from);
    }

    private void requireAdmin(UUID workspaceId, UUID userId) {
        WorkspaceMember member = memberRepository
                .findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new UnauthorizedException("Not a member of this workspace"));
        if (member.getRole() != MemberRole.ADMIN) {
            throw new UnauthorizedException("Admin role required to view annotation logs");
        }
    }
}
