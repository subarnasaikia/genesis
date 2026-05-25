package com.genesis.pos.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.genesis.common.exception.ResourceNotFoundException;
import com.genesis.common.exception.UnauthorizedException;
import com.genesis.common.exception.ValidationException;
import com.genesis.pos.dto.CreatePosTagRequest;
import com.genesis.pos.dto.PosTagDefinitionDto;
import com.genesis.pos.entity.PosTagDefinitionEntity;
import com.genesis.pos.entity.PosTagScope;
import com.genesis.pos.repository.PosTagDefinitionRepository;
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
class PosTagDefinitionServiceTest {

    @Mock
    private PosTagDefinitionRepository definitionRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceMemberRepository memberRepository;

    @Mock
    private com.genesis.workspace.service.WorkspaceAccessControl accessControl;

    private PosTagDefinitionService service;

    private UUID workspaceId;
    private UUID ownerId;
    private UUID memberAdminId;
    private UUID memberAnnotatorId;

    @BeforeEach
    void setUp() {
        service = new PosTagDefinitionService(definitionRepository, workspaceRepository, memberRepository, accessControl);
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

    private CreatePosTagRequest req(String tag, PosTagScope scope, UUID ws) {
        CreatePosTagRequest r = new CreatePosTagRequest();
        r.setTag(tag);
        r.setScope(scope);
        r.setWorkspaceId(ws);
        return r;
    }

    @Test
    @DisplayName("create rejects tag colliding with universal set")
    void create_collidesWithUniversal_rejected() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.create(req("NOUN", PosTagScope.WORKSPACE, workspaceId), ownerId));
        assertTrue(ex.getMessage().contains("built-in"));
        verify(definitionRepository, never()).save(any());
    }

    @Test
    @DisplayName("create rejects malformed tag (lowercase)")
    void create_badPattern_rejected() {
        assertThrows(ValidationException.class,
                () -> service.create(req("noun_x", PosTagScope.WORKSPACE, workspaceId), ownerId));
    }

    @Test
    @DisplayName("create requires workspaceId when scope=WORKSPACE")
    void create_workspaceScopeMissingId_rejected() {
        assertThrows(ValidationException.class,
                () -> service.create(req("NEG", PosTagScope.WORKSPACE, null), ownerId));
    }

    @Test
    @DisplayName("create rejects non-admin member")
    void create_workspaceScope_annotator_rejected() {
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace()));
        stubWorkspaceMember(memberAnnotatorId, MemberRole.ANNOTATOR);

        assertThrows(UnauthorizedException.class,
                () -> service.create(req("NEG", PosTagScope.WORKSPACE, workspaceId), memberAnnotatorId));
    }

    @Test
    @DisplayName("create rejects non-member")
    void create_workspaceScope_nonMember_rejected() {
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace()));
        when(memberRepository.findByWorkspaceIdAndUserId(workspaceId, memberAnnotatorId))
                .thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class,
                () -> service.create(req("NEG", PosTagScope.WORKSPACE, workspaceId), memberAnnotatorId));
    }

    @Test
    @DisplayName("create allows workspace owner")
    void create_workspaceScope_owner_persists() {
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace()));
        when(definitionRepository.findByTagAndScopeAndWorkspaceId("NEG", PosTagScope.WORKSPACE, workspaceId))
                .thenReturn(Optional.empty());
        when(definitionRepository.save(any(PosTagDefinitionEntity.class)))
                .thenAnswer(inv -> {
                    PosTagDefinitionEntity e = inv.getArgument(0);
                    e.setId(UUID.randomUUID());
                    return e;
                });

        PosTagDefinitionDto dto = service.create(req("NEG", PosTagScope.WORKSPACE, workspaceId), ownerId);

        assertNotNull(dto);
        assertEquals("NEG", dto.getTag());
        assertEquals(PosTagScope.WORKSPACE, dto.getScope());
        assertEquals(workspaceId, dto.getWorkspaceId());
    }

    @Test
    @DisplayName("create allows ADMIN member")
    void create_workspaceScope_admin_persists() {
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace()));
        stubWorkspaceMember(memberAdminId, MemberRole.ADMIN);
        when(definitionRepository.findByTagAndScopeAndWorkspaceId("NEG", PosTagScope.WORKSPACE, workspaceId))
                .thenReturn(Optional.empty());
        when(definitionRepository.save(any(PosTagDefinitionEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PosTagDefinitionDto dto = service.create(req("NEG", PosTagScope.WORKSPACE, workspaceId), memberAdminId);

        assertEquals("NEG", dto.getTag());
    }

    @Test
    @DisplayName("create rejects duplicate within same workspace")
    void create_workspaceScope_duplicate_rejected() {
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace()));
        PosTagDefinitionEntity existing = new PosTagDefinitionEntity();
        existing.setTag("NEG");
        when(definitionRepository.findByTagAndScopeAndWorkspaceId("NEG", PosTagScope.WORKSPACE, workspaceId))
                .thenReturn(Optional.of(existing));

        assertThrows(ValidationException.class,
                () -> service.create(req("NEG", PosTagScope.WORKSPACE, workspaceId), ownerId));
    }

    @Test
    @DisplayName("create global tag persists")
    void create_globalScope_persists() {
        when(definitionRepository.findByTagAndScopeAndWorkspaceIdIsNull("NEG", PosTagScope.GLOBAL))
                .thenReturn(Optional.empty());
        when(definitionRepository.save(any(PosTagDefinitionEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PosTagDefinitionDto dto = service.create(req("NEG", PosTagScope.GLOBAL, null), ownerId);

        assertEquals(PosTagScope.GLOBAL, dto.getScope());
        assertNull(dto.getWorkspaceId());
    }

    @Test
    @DisplayName("effectiveTagSet composes universal + global + workspace")
    void effectiveTagSet_composes() {
        PosTagDefinitionEntity g = new PosTagDefinitionEntity();
        g.setTag("GLOBAL_CUSTOM");
        g.setScope(PosTagScope.GLOBAL);
        PosTagDefinitionEntity w = new PosTagDefinitionEntity();
        w.setTag("WS_CUSTOM");
        w.setScope(PosTagScope.WORKSPACE);
        w.setWorkspaceId(workspaceId);

        when(definitionRepository.findByScope(PosTagScope.GLOBAL)).thenReturn(List.of(g));
        when(definitionRepository.findByWorkspaceId(workspaceId)).thenReturn(List.of(w));

        Set<String> tags = service.effectiveTagSet(workspaceId, memberAnnotatorId);
        assertTrue(tags.contains("NOUN"));
        assertTrue(tags.contains("VERB"));
        assertTrue(tags.contains("GLOBAL_CUSTOM"));
        assertTrue(tags.contains("WS_CUSTOM"));
    }

    @Test
    @DisplayName("delete workspace tag requires owner/admin")
    void delete_workspaceTag_nonAdmin_rejected() {
        PosTagDefinitionEntity entity = new PosTagDefinitionEntity();
        entity.setId(UUID.randomUUID());
        entity.setScope(PosTagScope.WORKSPACE);
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
        PosTagDefinitionEntity entity = new PosTagDefinitionEntity();
        entity.setId(UUID.randomUUID());
        entity.setScope(PosTagScope.GLOBAL);
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
