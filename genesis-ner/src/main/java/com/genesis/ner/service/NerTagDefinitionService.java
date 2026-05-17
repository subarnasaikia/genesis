package com.genesis.ner.service;

import com.genesis.common.exception.ResourceNotFoundException;
import com.genesis.common.exception.UnauthorizedException;
import com.genesis.common.exception.ValidationException;
import com.genesis.ner.dto.CreateNerTagRequest;
import com.genesis.ner.dto.NerTagDefinitionDto;
import com.genesis.ner.entity.NerTagDefinitionEntity;
import com.genesis.ner.entity.NerTagScope;
import com.genesis.ner.repository.NerTagDefinitionRepository;
import com.genesis.workspace.entity.MemberRole;
import com.genesis.workspace.entity.Workspace;
import com.genesis.workspace.entity.WorkspaceMember;
import com.genesis.workspace.repository.WorkspaceMemberRepository;
import com.genesis.workspace.repository.WorkspaceRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NerTagDefinitionService {

    private static final Pattern TAG_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]{0,19}$");

    /**
     * OntoNotes 5 entity types — the 18 always-available NER labels. Custom
     * tags created by workspace owners/admins are additive on top of this set.
     */
    public static final Map<String, String> UNIVERSAL_NER_TAGS;

    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("PERSON", "People, including fictional");
        m.put("NORP", "Nationalities or religious or political groups");
        m.put("FAC", "Buildings, airports, highways, bridges");
        m.put("ORG", "Companies, agencies, institutions");
        m.put("GPE", "Countries, cities, states");
        m.put("LOC", "Non-GPE locations: mountain ranges, bodies of water");
        m.put("PRODUCT", "Vehicles, weapons, foods (not services)");
        m.put("EVENT", "Named hurricanes, battles, wars, sports events");
        m.put("WORK_OF_ART", "Titles of books, songs, etc.");
        m.put("LAW", "Named documents made into laws");
        m.put("LANGUAGE", "Any named language");
        m.put("DATE", "Absolute or relative dates or periods");
        m.put("TIME", "Times smaller than a day");
        m.put("PERCENT", "Percentage, including %");
        m.put("MONEY", "Monetary values, including unit");
        m.put("QUANTITY", "Measurements, as of weight or distance");
        m.put("ORDINAL", "\"first\", \"second\"");
        m.put("CARDINAL", "Numerals that do not fall under another type");
        UNIVERSAL_NER_TAGS = Collections.unmodifiableMap(m);
    }

    private final NerTagDefinitionRepository definitionRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;

    public NerTagDefinitionService(NerTagDefinitionRepository definitionRepository,
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository memberRepository) {
        this.definitionRepository = definitionRepository;
        this.workspaceRepository = workspaceRepository;
        this.memberRepository = memberRepository;
    }

    public NerTagDefinitionDto create(CreateNerTagRequest request, UUID callerUserId) {
        if (request == null) {
            throw new ValidationException("body", "Request body required");
        }
        if (callerUserId == null) {
            throw new UnauthorizedException("Authentication required");
        }
        String tag = request.getTag() == null ? null : request.getTag().trim();
        if (tag == null || tag.isEmpty()) {
            throw new ValidationException("tag", "Tag is required");
        }
        if (!TAG_PATTERN.matcher(tag).matches()) {
            throw new ValidationException("tag",
                    "Tag must start with uppercase letter and contain only A-Z, 0-9, underscore (max 20 chars)");
        }
        if (UNIVERSAL_NER_TAGS.containsKey(tag)) {
            throw new ValidationException("tag", "Tag conflicts with a built-in OntoNotes tag: " + tag);
        }
        NerTagScope scope = request.getScope();
        if (scope == null) {
            throw new ValidationException("scope", "Scope is required (GLOBAL or WORKSPACE)");
        }

        UUID workspaceId = null;
        if (scope == NerTagScope.WORKSPACE) {
            workspaceId = request.getWorkspaceId();
            if (workspaceId == null) {
                throw new ValidationException("workspaceId",
                        "workspaceId is required when scope=WORKSPACE");
            }
            requireWorkspaceAdmin(workspaceId, callerUserId);
            definitionRepository.findByTagAndScopeAndWorkspaceId(tag, scope, workspaceId)
                    .ifPresent(existing -> {
                        throw new ValidationException("tag",
                                "Tag already exists in this workspace: " + tag);
                    });
        } else {
            definitionRepository.findByTagAndScopeAndWorkspaceIdIsNull(tag, scope)
                    .ifPresent(existing -> {
                        throw new ValidationException("tag",
                                "Global tag already exists: " + tag);
                    });
        }

        NerTagDefinitionEntity entity = new NerTagDefinitionEntity();
        entity.setTag(tag);
        entity.setDescription(request.getDescription());
        entity.setScope(scope);
        entity.setWorkspaceId(workspaceId);
        entity.setCreatedByUserId(callerUserId.toString());

        return NerTagDefinitionDto.from(definitionRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<NerTagDefinitionDto> listForWorkspace(UUID workspaceId) {
        List<NerTagDefinitionDto> result = new ArrayList<>();
        UNIVERSAL_NER_TAGS.forEach((tag, desc) -> result.add(NerTagDefinitionDto.builtin(tag, desc)));
        definitionRepository.findByScope(NerTagScope.GLOBAL)
                .forEach(e -> result.add(NerTagDefinitionDto.from(e)));
        if (workspaceId != null) {
            definitionRepository.findByWorkspaceId(workspaceId)
                    .forEach(e -> result.add(NerTagDefinitionDto.from(e)));
        }
        return result;
    }

    /**
     * Effective tag set used by {@code NerAnnotationService} to validate NER
     * spans. Includes OntoNotes 18 + global customs + workspace customs.
     */
    @Transactional(readOnly = true)
    public Set<String> effectiveTagSet(UUID workspaceId) {
        Set<String> tags = new HashSet<>(UNIVERSAL_NER_TAGS.keySet());
        definitionRepository.findByScope(NerTagScope.GLOBAL)
                .forEach(e -> tags.add(e.getTag()));
        if (workspaceId != null) {
            definitionRepository.findByWorkspaceId(workspaceId)
                    .forEach(e -> tags.add(e.getTag()));
        }
        return tags;
    }

    public void delete(UUID definitionId, UUID callerUserId) {
        NerTagDefinitionEntity entity = definitionRepository.findById(definitionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "NER tag definition not found: " + definitionId));

        if (entity.getScope() == NerTagScope.WORKSPACE) {
            requireWorkspaceAdmin(entity.getWorkspaceId(), callerUserId);
        } else {
            if (!entity.getCreatedByUserId().equals(callerUserId.toString())) {
                throw new UnauthorizedException(
                        "Global NER tags can only be deleted by their creator", true);
            }
        }
        definitionRepository.delete(entity);
    }

    private void requireWorkspaceAdmin(UUID workspaceId, UUID callerUserId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Workspace not found: " + workspaceId));
        if (workspace.getOwner() != null && callerUserId.equals(workspace.getOwner().getId())) {
            return;
        }
        WorkspaceMember member = memberRepository
                .findByWorkspaceIdAndUserId(workspaceId, callerUserId)
                .orElseThrow(() -> new UnauthorizedException(
                        "Not a member of this workspace", true));
        if (member.getRole() != MemberRole.ADMIN) {
            throw new UnauthorizedException(
                    "Only workspace owner or admins can manage NER tags", true);
        }
    }
}
