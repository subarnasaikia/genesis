package com.genesis.wsd.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.genesis.common.exception.UnauthorizedException;
import com.genesis.workspace.service.WorkspaceAccessControl;
import com.genesis.wsd.dto.CreateSenseRequest;
import com.genesis.wsd.entity.WsdSenseEntity;
import com.genesis.wsd.repository.WsdAnnotationRepository;
import com.genesis.wsd.repository.WsdSenseRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class WsdSenseServiceTest {

    @Mock
    private WsdSenseRepository senseRepository;
    @Mock
    private WsdAnnotationRepository annotationRepository;
    @Mock
    private WorkspaceAccessControl accessControl;

    private WsdSenseService service;

    private UUID workspaceId;
    private UUID userId;
    private UUID senseId;

    @BeforeEach
    void setUp() {
        service = new WsdSenseService(senseRepository, annotationRepository, accessControl);
        workspaceId = UUID.randomUUID();
        userId = UUID.randomUUID();
        senseId = UUID.randomUUID();
    }

    private WsdSenseEntity senseInWorkspace() {
        WsdSenseEntity e = new WsdSenseEntity();
        e.setId(senseId);
        e.setWorkspaceId(workspaceId);
        e.setWord("bank");
        e.setSenseLabel("financial");
        return e;
    }

    @Test
    @DisplayName("deleteSense with active annotations → 409 with count in message (eng-review D5)")
    void deleteSenseInUse_returns409WithCount() {
        when(senseRepository.findById(senseId)).thenReturn(Optional.of(senseInWorkspace()));
        when(annotationRepository.countBySenseId(senseId)).thenReturn(7L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.deleteSense(workspaceId, senseId, userId));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertNotNull(ex.getReason());
        assertTrue(ex.getReason().contains("7"), "count must appear in reason: " + ex.getReason());

        verify(senseRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteSense with no annotations succeeds")
    void deleteSenseUnused_succeeds() {
        WsdSenseEntity e = senseInWorkspace();
        when(senseRepository.findById(senseId)).thenReturn(Optional.of(e));
        when(annotationRepository.countBySenseId(senseId)).thenReturn(0L);

        service.deleteSense(workspaceId, senseId, userId);

        verify(senseRepository, times(1)).delete(e);
    }

    @Test
    @DisplayName("Non-admin cannot create senses → UnauthorizedException (requireAdmin throws)")
    void createSense_nonAdmin_unauthorized() {
        doThrow(new UnauthorizedException("Admin role required", true))
                .when(accessControl).requireAdmin(workspaceId, userId);
        CreateSenseRequest req = new CreateSenseRequest();
        req.setWord("bank");
        req.setSenseLabel("financial");

        assertThrows(UnauthorizedException.class,
                () -> service.createSense(workspaceId, userId, req));

        verify(senseRepository, never()).save(any());
    }
}
