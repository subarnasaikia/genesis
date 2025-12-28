package com.genesis.notification.listener;

import com.genesis.importexport.event.ExportGeneratedEvent;
import com.genesis.notification.entity.NotificationType;
import com.genesis.notification.service.NotificationService;
import com.genesis.workspace.event.DocumentAnnotationCompletedEvent;
import com.genesis.workspace.event.DocumentDeletedEvent;
import com.genesis.workspace.event.DocumentTokenizedEvent;
import com.genesis.workspace.event.DocumentUploadedEvent;
import com.genesis.workspace.event.MemberAddedEvent;
import com.genesis.workspace.event.MemberRemovedEvent;
import com.genesis.workspace.event.WorkspaceCreatedEvent;
import com.genesis.workspace.event.WorkspaceDeletedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final com.genesis.workspace.repository.WorkspaceMemberRepository workspaceMemberRepository;
    private final com.genesis.user.repository.UserRepository userRepository;

    public NotificationEventListener(NotificationService notificationService, 
                                     com.genesis.workspace.repository.WorkspaceMemberRepository workspaceMemberRepository, 
                                     com.genesis.user.repository.UserRepository userRepository) {
        this.notificationService = notificationService;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.userRepository = userRepository;
    }

    @EventListener
    @Async
    public void handleDocumentUploaded(DocumentUploadedEvent event) {
        String actorName = getUserName(event.getUserId());
        String message = String.format("Document '%s' uploaded by %s", event.getFileName(), actorName);
        
        // Notify actor
        notificationService.createNotification(
            event.getUserId(),
            "Document Uploaded",
            "Document '" + event.getFileName() + "' uploaded successfully.",
            NotificationType.SUCCESS,
            event.getWorkspaceId(),
            event.getUserId(),
            "/workspace/" + event.getWorkspaceId()
        );

        // Notify others
        notifyWorkspaceMembersExcept(
            event.getWorkspaceId(), 
            event.getUserId(), 
            "Document Uploaded", 
            message, 
            NotificationType.INFO, 
            event.getUserId(),
            "/workspace/" + event.getWorkspaceId()
        );
    }

    @EventListener
    @Async
    public void handleDocumentTokenized(DocumentTokenizedEvent event) {
        notifyWorkspaceMembers(
            event.getWorkspaceId(),
            "Document Processing Complete",
            String.format("Document '%s' has been tokenized and is ready.", event.getDocumentName()),
            NotificationType.SUCCESS,
            null, // System notification
            "/workspace/" + event.getWorkspaceId() + "/editor"
        );
    }

    @EventListener
    @Async
    public void handleDocumentAnnotationCompleted(DocumentAnnotationCompletedEvent event) {
        notifyWorkspaceMembers(
            event.getWorkspaceId(),
            "Annotation Complete",
            String.format("Document '%s' has been marked as annotation complete.", event.getDocumentName()),
            NotificationType.SUCCESS,
            event.getCompletedByUserId(),
            "/workspace/" + event.getWorkspaceId()
        );
    }

    @EventListener
    @Async
    public void handleDocumentDeleted(DocumentDeletedEvent event) {
        String actorName = getUserName(event.getActorId());
        String message = String.format("Document '%s' deleted by %s", event.getDocumentName(), actorName);

        // Notify actor
        notificationService.createNotification(
            event.getActorId(),
            "Document Deleted",
            "Document '" + event.getDocumentName() + "' deleted successfully.",
            NotificationType.SUCCESS,
            event.getWorkspaceId(),
            event.getActorId(),
            "/workspace/" + event.getWorkspaceId()
        );

        // Notify others
        notifyWorkspaceMembersExcept(
            event.getWorkspaceId(),
            event.getActorId(),
            "Document Deleted",
            message,
            NotificationType.WARNING,
            event.getActorId(),
            "/workspace/" + event.getWorkspaceId()
        );
    }

    @EventListener
    @Async
    public void handleWorkspaceCreated(WorkspaceCreatedEvent event) {
        notificationService.createNotification(
            event.getOwnerId(),
            "Workspace Created",
            String.format("Workspace '%s' created successfully.", event.getWorkspaceName()),
            NotificationType.SUCCESS,
            event.getWorkspaceId(),
            event.getOwnerId(),
            "/workspace/" + event.getWorkspaceId()
        );
    }

    @EventListener
    @Async
    public void handleWorkspaceDeleted(WorkspaceDeletedEvent event) {
        // Notify all members including the one who deleted it (as warning/info)
        // Since workspace is gone, link is just /home
        for (java.util.UUID memberId : event.getMemberIds()) {
            notificationService.createNotification(
                memberId,
                "Workspace Deleted",
                String.format("Workspace '%s' has been deleted.", event.getWorkspaceName()),
                NotificationType.WARNING,
                null, // Workspace gone
                null, 
                "/home"
            );
        }
    }

    @EventListener
    @Async
    public void handleMemberAdded(MemberAddedEvent event) {
        String actorName = getUserName(event.getAddedByMemberId());
        String newMemberName = getUserName(event.getAddedMemberId());

        // Notify added member
        notificationService.createNotification(
            event.getAddedMemberId(),
            "Added to Workspace",
            String.format("You have been added to workspace '%s' by %s.", event.getWorkspaceName(), actorName),
            NotificationType.SUCCESS,
            event.getWorkspaceId(),
            event.getAddedByMemberId(),
            "/workspace/" + event.getWorkspaceId()
        );

        // Notify actor
        notificationService.createNotification(
            event.getAddedByMemberId(),
            "Member Added",
            String.format("User %s added to workspace successfully.", newMemberName),
            NotificationType.SUCCESS,
            event.getWorkspaceId(),
            event.getAddedByMemberId(),
            "/workspace/" + event.getWorkspaceId()
        );

        // Notify others
        notifyWorkspaceMembersExcept(
            event.getWorkspaceId(),
            java.util.List.of(event.getAddedMemberId(), event.getAddedByMemberId()),
            "Member Added",
            String.format("%s added %s to the workspace.", actorName, newMemberName),
            NotificationType.INFO,
            event.getAddedByMemberId(),
            "/workspace/" + event.getWorkspaceId()
        );
    }

    @EventListener
    @Async
    public void handleExportGenerated(ExportGeneratedEvent event) {
        notificationService.createNotification(
            event.getUserId(),
            "Export Ready",
            String.format("Export '%s' is ready for download.", event.getFileName()),
            NotificationType.SUCCESS,
            null,
            null,
            event.getDownloadUrl() // Assuming this is a download link
        );
    }

    @EventListener
    @Async
    public void handleMemberRemoved(MemberRemovedEvent event) {
        // Notify the removed member
        notificationService.createNotification(
            event.getRemovedMemberId(),
            "Removed from Workspace",
            String.format("You have been removed from workspace '%s'.", event.getWorkspaceName()),
            NotificationType.WARNING,
            null, // Workspace ID not relevant anymore for the removed user
            event.getRemovedByMemberId(),
            "/home"
        );
    }

    // Helpers

    private String getUserName(java.util.UUID userId) {
        return userRepository.findById(userId)
                .map(u -> u.getFirstName() + " " + u.getLastName())
                .orElse("Unknown User");
    }

    private void notifyWorkspaceMembers(java.util.UUID workspaceId, String title, String message, NotificationType type, java.util.UUID actorId, String link) {
        workspaceMemberRepository.findByWorkspaceId(workspaceId).forEach(member -> {
            notificationService.createNotification(
                member.getUser().getId(),
                title,
                message,
                type,
                workspaceId,
                actorId,
                link
            );
        });
    }

    private void notifyWorkspaceMembersExcept(java.util.UUID workspaceId, java.util.UUID excludedUserId, String title, String message, NotificationType type, java.util.UUID actorId, String link) {
        notifyWorkspaceMembersExcept(workspaceId, java.util.List.of(excludedUserId), title, message, type, actorId, link);
    }

    private void notifyWorkspaceMembersExcept(java.util.UUID workspaceId, java.util.List<java.util.UUID> excludedUserIds, String title, String message, NotificationType type, java.util.UUID actorId, String link) {
        workspaceMemberRepository.findByWorkspaceId(workspaceId).stream()
            .filter(member -> !excludedUserIds.contains(member.getUser().getId()))
            .forEach(member -> {
                notificationService.createNotification(
                    member.getUser().getId(),
                    title,
                    message,
                    type,
                    workspaceId,
                    actorId,
                    link
                );
            });
    }
}
