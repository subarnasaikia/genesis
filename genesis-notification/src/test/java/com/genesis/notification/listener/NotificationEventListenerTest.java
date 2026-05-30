package com.genesis.notification.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.genesis.notification.entity.NotificationType;
import com.genesis.notification.port.RecipientDirectory;
import com.genesis.notification.service.NotificationService;
import com.genesis.workspace.event.DocumentTokenizedEvent;
import com.genesis.workspace.event.DocumentUploadedEvent;
import com.genesis.workspace.event.WorkspaceDeletedEvent;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link NotificationEventListener}. The listener used to inject
 * workspace/user repositories directly, which made it un-unit-testable; routing
 * its lookups through {@link RecipientDirectory} (ARCHITECTURE_AUDIT A-004) lets
 * us drive it with a mock port.
 */
@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private RecipientDirectory recipientDirectory;

    @InjectMocks
    private NotificationEventListener listener;

    @Test
    @DisplayName("document uploaded - notifies actor plus every other workspace member")
    void documentUploaded_notifiesActorAndOtherMembers() {
        UUID workspaceId = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        when(recipientDirectory.displayName(actor)).thenReturn("Ada Lovelace");
        when(recipientDirectory.workspaceMemberUserIds(workspaceId))
                .thenReturn(List.of(actor, other));

        listener.handleDocumentUploaded(new DocumentUploadedEvent(
                this, UUID.randomUUID(), workspaceId, "https://files/notes.txt", actor, "notes.txt"));

        // Actor gets the personal "uploaded successfully" notification.
        verify(notificationService).createNotification(
                eq(actor), eq("Document Uploaded"), any(), eq(NotificationType.SUCCESS),
                eq(workspaceId), eq(actor), any());
        // The other member gets the fan-out INFO notification; the actor does not (excluded).
        verify(notificationService).createNotification(
                eq(other), eq("Document Uploaded"), any(), eq(NotificationType.INFO),
                eq(workspaceId), eq(actor), any());
        verify(notificationService, never()).createNotification(
                eq(actor), eq("Document Uploaded"), any(), eq(NotificationType.INFO),
                any(), any(), any());
    }

    @Test
    @DisplayName("document tokenized - notifies all workspace members via the port")
    void documentTokenized_fansOutToAllMembers() {
        UUID workspaceId = UUID.randomUUID();
        UUID m1 = UUID.randomUUID();
        UUID m2 = UUID.randomUUID();
        when(recipientDirectory.workspaceMemberUserIds(workspaceId)).thenReturn(List.of(m1, m2));

        listener.handleDocumentTokenized(
                new DocumentTokenizedEvent(this, UUID.randomUUID(), workspaceId, "notes.txt"));

        verify(notificationService).createNotification(
                eq(m1), eq("Document Processing Complete"), any(), eq(NotificationType.SUCCESS),
                eq(workspaceId), eq(null), any());
        verify(notificationService).createNotification(
                eq(m2), eq("Document Processing Complete"), any(), eq(NotificationType.SUCCESS),
                eq(workspaceId), eq(null), any());
    }

    @Test
    @DisplayName("workspace deleted - notifies each member carried in the event payload, no port lookup")
    void workspaceDeleted_notifiesPayloadMembers() {
        UUID m1 = UUID.randomUUID();
        UUID m2 = UUID.randomUUID();
        listener.handleWorkspaceDeleted(
                new WorkspaceDeletedEvent(this, UUID.randomUUID(), "Team WS", List.of(m1, m2)));

        verify(notificationService).createNotification(
                eq(m1), eq("Workspace Deleted"), any(), eq(NotificationType.WARNING), any(), any(), any());
        verify(notificationService).createNotification(
                eq(m2), eq("Workspace Deleted"), any(), eq(NotificationType.WARNING), any(), any(), any());
        // This event already carries member ids, so the listener must not hit the directory.
        verify(recipientDirectory, never()).workspaceMemberUserIds(any());
    }
}
