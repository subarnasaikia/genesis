package com.genesis.wsd.service;

import com.genesis.common.exception.ResourceNotFoundException;
import com.genesis.common.exception.UnauthorizedException;
import com.genesis.common.exception.ValidationException;
import com.genesis.workspace.entity.MemberRole;
import com.genesis.workspace.entity.WorkspaceMember;
import com.genesis.workspace.repository.WorkspaceMemberRepository;
import com.genesis.wsd.dto.CreateSenseRequest;
import com.genesis.wsd.dto.WsdSenseDto;
import com.genesis.wsd.entity.WsdSenseEntity;
import com.genesis.wsd.repository.WsdAnnotationRepository;
import com.genesis.wsd.repository.WsdSenseRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Admin-managed word-sense inventory per workspace.
 *
 * <p>Reads are open to any workspace member; create/update/delete require
 * the caller to be a workspace {@code ADMIN}. Delete is blocked with a
 * 409 CONFLICT when annotations reference the sense.
 */
@Service
@Transactional
public class WsdSenseService {

    private final WsdSenseRepository senseRepository;
    private final WsdAnnotationRepository annotationRepository;
    private final WorkspaceMemberRepository memberRepository;

    public WsdSenseService(WsdSenseRepository senseRepository,
            WsdAnnotationRepository annotationRepository,
            WorkspaceMemberRepository memberRepository) {
        this.senseRepository = senseRepository;
        this.annotationRepository = annotationRepository;
        this.memberRepository = memberRepository;
    }

    @Transactional(readOnly = true)
    public List<WsdSenseDto> listSenses(UUID workspaceId, UUID callerUserId) {
        requireMember(workspaceId, callerUserId);
        return senseRepository.findByWorkspaceIdOrderByWordAscSenseLabelAsc(workspaceId).stream()
                .map(WsdSenseDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WsdSenseDto> listSensesForWord(UUID workspaceId, String word, UUID callerUserId) {
        requireMember(workspaceId, callerUserId);
        return senseRepository.findByWorkspaceIdAndWordOrderBySenseLabelAsc(workspaceId, word).stream()
                .map(WsdSenseDto::from)
                .collect(Collectors.toList());
    }

    public WsdSenseDto createSense(UUID workspaceId, UUID callerUserId, CreateSenseRequest request) {
        requireAdmin(workspaceId, callerUserId);
        validate(request);

        WsdSenseEntity entity = new WsdSenseEntity();
        entity.setWorkspaceId(workspaceId);
        entity.setWord(request.getWord().trim());
        entity.setSenseLabel(request.getSenseLabel().trim());
        entity.setDescription(request.getDescription());
        return WsdSenseDto.from(senseRepository.save(entity));
    }

    public WsdSenseDto updateSense(UUID workspaceId, UUID senseId, UUID callerUserId, CreateSenseRequest request) {
        requireAdmin(workspaceId, callerUserId);
        validate(request);

        WsdSenseEntity entity = senseRepository.findById(senseId)
                .orElseThrow(() -> new ResourceNotFoundException("Sense not found: " + senseId));
        if (!entity.getWorkspaceId().equals(workspaceId)) {
            throw new ValidationException("senseId does not belong to workspace " + workspaceId);
        }
        entity.setWord(request.getWord().trim());
        entity.setSenseLabel(request.getSenseLabel().trim());
        entity.setDescription(request.getDescription());
        return WsdSenseDto.from(senseRepository.save(entity));
    }

    public void deleteSense(UUID workspaceId, UUID senseId, UUID callerUserId) {
        requireAdmin(workspaceId, callerUserId);

        WsdSenseEntity entity = senseRepository.findById(senseId)
                .orElseThrow(() -> new ResourceNotFoundException("Sense not found: " + senseId));
        if (!entity.getWorkspaceId().equals(workspaceId)) {
            throw new ValidationException("senseId does not belong to workspace " + workspaceId);
        }

        long inUse = annotationRepository.countBySenseId(senseId);
        if (inUse > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This sense is used by " + inUse + " annotations");
        }
        senseRepository.delete(entity);
    }

    private void validate(CreateSenseRequest request) {
        if (request == null) {
            throw new ValidationException("request body required");
        }
        if (request.getWord() == null || request.getWord().isBlank()) {
            throw new ValidationException("word", "must not be blank");
        }
        if (request.getSenseLabel() == null || request.getSenseLabel().isBlank()) {
            throw new ValidationException("senseLabel", "must not be blank");
        }
    }

    private void requireMember(UUID workspaceId, UUID userId) {
        if (!memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new UnauthorizedException("Not a member of this workspace");
        }
    }

    private void requireAdmin(UUID workspaceId, UUID userId) {
        WorkspaceMember member = memberRepository
                .findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new UnauthorizedException("Not a member of this workspace"));
        if (member.getRole() != MemberRole.ADMIN) {
            throw new UnauthorizedException("Admin role required to manage senses");
        }
    }
}
