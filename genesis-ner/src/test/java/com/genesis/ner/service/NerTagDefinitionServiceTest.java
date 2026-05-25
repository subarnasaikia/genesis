package com.genesis.ner.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.genesis.common.exception.ResourceNotFoundException;
import com.genesis.common.exception.UnauthorizedException;
import com.genesis.common.exception.ValidationException;
import com.genesis.ner.dto.CreateNerTagRequest;
import com.genesis.ner.dto.NerTagDefinitionDto;
import com.genesis.ner.entity.NerTagDefinitionEntity;
import com.genesis.ner.entity.NerTagScope;
import com.genesis.ner.repository.NerTagDefinitionRepository;
import com.genesis.user.entity.User;
import com.genesis.workspace.entity.MemberRole;
import com.genesis.workspace.entity.Workspace;
import com.genesis.workspace.entity.WorkspaceMember;
import com.genesis.workspace.repository.WorkspaceMemberRepository;
import com.genesis.workspace.repository.WorkspaceRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NerTagDefinitionServiceTest {

    @Mock
    private NerTagDefinitionRepository definitionRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceMemberRepository memberRepository;

    @Mock
    private com.genesis.workspace.service.WorkspaceAccessControl accessControl;

    private NerTagDefinitionService service;

    private UUID workspaceId;
    private UUID ownerId;
    private UUID memberAdminId;
    private UUID memberAnnotatorId;

    @BeforeEach
    void setUp() {
        service = new NerTagDefinitionService(definitionRepository, workspaceRepository, memberRepository, accessControl);
        workspaceId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        memberAdminId = UUID.randomUUID();
        memberAnnotatorId = UUID.randomUUID();
    }

    private Workspace workspace() {
        User owner = new User();
        owner.setId(ownerId);
        Workspace w = new Workspace();
        w.setId(workspaceId);
        w.setOwner(owner);
        return w;
    }

    private void stubWorkspaceMember(UUID userId, MemberRole role) {
        WorkspaceMember m = new WorkspaceMember();
        m.setRole(role);
        when(memberRepository.findByWorkspaceIdAndUserId(workspaceId, userId))
                .thenReturn(Optional.of(m));
    }

    private CreateNerTagRequest req(String tag, NerTagScope scope, UUID ws) {
        CreateNerTagRequest r = new CreateNerTagRequest();
        r.setTag(tag);
        r.setScope(scope);
        r.setWorkspaceId(ws);
        return r;
    }

    @Test
    @DisplayName("create rejects tag colliding with OntoNotes universal set")
    void create_collidesWithUniversal_rejected() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.create(req("PERSON", NerTagScope.WORKSPACE, workspaceId), ownerId));
        assertTrue(ex.getMessage().contains("built-in"));
        verify(definitionRepository, never()).save(any());
    }

    @Test
    @DisplayName("create rejects malformed tag (lowercase)")
    void create_badPattern_rejected() {
        assertThrows(ValidationException.class,
                () -> service.create(req("person_x", NerTagScope.WORKSPACE, workspaceId), ownerId));
    }

    @Test
    @DisplayName("create requires workspaceId when scope=WORKSPACE")
    void create_workspaceScopeMissingId_rejected() {
        assertThrows(ValidationException.class,
                () -> service.create(req("WIZARD", NerTagScope.WORKSPACE, null), ownerId));
    }

    @Test
    @DisplayName("create rejects non-admin member")
    void create_workspaceScope_annotator_rejected() {
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace()));
        stubWorkspaceMember(memberAnnotatorId, MemberRole.ANNOTATOR);

        assertThrows(UnauthorizedException.class,
                () -> service.create(req("WIZARD", NerTagScope.WORKSPACE, workspaceId), memberAnnotatorId));
    }

    @Test
    @DisplayName("create rejects non-member")
    void create_workspaceScope_nonMember_rejected() {
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace()));
        when(memberRepository.findByWorkspaceIdAndUserId(workspaceId, memberAnnotatorId))
                .thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class,
                () -> service.create(req("WIZARD", NerTagScope.WORKSPACE, workspaceId), memberAnnotatorId));
    }

    @Test
    @DisplayName("create allows workspace owner")
    void create_workspaceScope_owner_persists() {
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace()));
        when(definitionRepository.findByTagAndScopeAndWorkspaceId("WIZARD", NerTagScope.WORKSPACE, workspaceId))
                .thenReturn(Optional.empty());
        when(definitionRepository.save(any(NerTagDefinitionEntity.class)))
                .thenAnswer(inv -> {
                    NerTagDefinitionEntity e = inv.getArgument(0);
                    e.setId(UUID.randomUUID());
                    return e;
                });

        NerTagDefinitionDto dto = service.create(req("WIZARD", NerTagScope.WORKSPACE, workspaceId), ownerId);

        assertNotNull(dto);
        assertEquals("WIZARD", dto.getTag());
        assertEquals(NerTagScope.WORKSPACE, dto.getScope());
        assertEquals(workspaceId, dto.getWorkspaceId());
    }

    @Test
    @DisplayName("create allows ADMIN member")
    void create_workspaceScope_admin_persists() {
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace()));
        stubWorkspaceMember(memberAdminId, MemberRole.ADMIN);
        when(definitionRepository.findByTagAndScopeAndWorkspaceId("WIZARD", NerTagScope.WORKSPACE, workspaceId))
                .thenReturn(Optional.empty());
        when(definitionRepository.save(any(NerTagDefinitionEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        NerTagDefinitionDto dto = service.create(req("WIZARD", NerTagScope.WORKSPACE, workspaceId), memberAdminId);

        assertEquals("WIZARD", dto.getTag());
    }

    @Test
    @DisplayName("create rejects duplicate within same workspace")
    void create_workspaceScope_duplicate_rejected() {
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace()));
        NerTagDefinitionEntity existing = new NerTagDefinitionEntity();
        existing.setTag("WIZARD");
        when(definitionRepository.findByTagAndScopeAndWorkspaceId("WIZARD", NerTagScope.WORKSPACE, workspaceId))
                .thenReturn(Optional.of(existing));

        assertThrows(ValidationException.class,
                () -> service.create(req("WIZARD", NerTagScope.WORKSPACE, workspaceId), ownerId));
    }

    @Test
    @DisplayName("create global tag persists")
    void create_globalScope_persists() {
        when(definitionRepository.findByTagAndScopeAndWorkspaceIdIsNull("WIZARD", NerTagScope.GLOBAL))
                .thenReturn(Optional.empty());
        when(definitionRepository.save(any(NerTagDefinitionEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        NerTagDefinitionDto dto = service.create(req("WIZARD", NerTagScope.GLOBAL, null), ownerId);

        assertEquals(NerTagScope.GLOBAL, dto.getScope());
        assertNull(dto.getWorkspaceId());
    }

    @Test
    @DisplayName("effectiveTagSet composes OntoNotes + global + workspace")
    void effectiveTagSet_composes() {
        NerTagDefinitionEntity g = new NerTagDefinitionEntity();
        g.setTag("GLOBAL_CUSTOM");
        g.setScope(NerTagScope.GLOBAL);
        NerTagDefinitionEntity w = new NerTagDefinitionEntity();
        w.setTag("WS_CUSTOM");
        w.setScope(NerTagScope.WORKSPACE);
        w.setWorkspaceId(workspaceId);

        when(definitionRepository.findByScope(NerTagScope.GLOBAL)).thenReturn(List.of(g));
        when(definitionRepository.findByWorkspaceId(workspaceId)).thenReturn(List.of(w));

        Set<String> tags = service.effectiveTagSet(workspaceId, memberAnnotatorId);
        assertTrue(tags.contains("PERSON"));
        assertTrue(tags.contains("ORG"));
        assertTrue(tags.contains("WORK_OF_ART"));
        assertTrue(tags.contains("GLOBAL_CUSTOM"));
        assertTrue(tags.contains("WS_CUSTOM"));
        assertEquals(18 + 2, tags.size());
    }

    @Test
    @DisplayName("listForWorkspace includes 18 OntoNotes built-ins")
    void listForWorkspace_includesBuiltins() {
        when(definitionRepository.findByScope(NerTagScope.GLOBAL)).thenReturn(List.of());
        when(definitionRepository.findByWorkspaceId(workspaceId)).thenReturn(List.of());

        List<NerTagDefinitionDto> dtos = service.listForWorkspace(workspaceId, memberAnnotatorId);
        long builtins = dtos.stream().filter(NerTagDefinitionDto::isBuiltin).count();
        assertEquals(18, builtins);
    }

    @Test
    @DisplayName("delete workspace tag requires owner/admin")
    void delete_workspaceTag_nonAdmin_rejected() {
        NerTagDefinitionEntity entity = new NerTagDefinitionEntity();
        entity.setId(UUID.randomUUID());
        entity.setScope(NerTagScope.WORKSPACE);
        entity.setWorkspaceId(workspaceId);

        when(definitionRepository.findById(entity.getId())).thenReturn(Optional.of(entity));
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace()));
        stubWorkspaceMember(memberAnnotatorId, MemberRole.ANNOTATOR);

        assertThrows(UnauthorizedException.class,
                () -> service.delete(entity.getId(), memberAnnotatorId));
    }

    @Test
    @DisplayName("delete global tag only by creator")
    void delete_globalTag_nonCreator_rejected() {
        UUID creatorId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        NerTagDefinitionEntity entity = new NerTagDefinitionEntity();
        entity.setId(UUID.randomUUID());
        entity.setScope(NerTagScope.GLOBAL);
        entity.setCreatedByUserId(creatorId.toString());

        when(definitionRepository.findById(entity.getId())).thenReturn(Optional.of(entity));

        assertThrows(UnauthorizedException.class,
                () -> service.delete(entity.getId(), otherId));
    }

    @Test
    @DisplayName("delete throws when definition missing")
    void delete_missing_throws() {
        UUID id = UUID.randomUUID();
        when(definitionRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.delete(id, ownerId));
    }
}
