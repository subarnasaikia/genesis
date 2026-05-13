package com.genesis.logging.listener;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.genesis.common.event.ActionType;
import com.genesis.common.event.AnnotationLogEvent;
import com.genesis.logging.entity.AnnotationLogEntity;
import com.genesis.logging.repository.AnnotationLogRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Failure isolation tests for {@link AnnotationAuditListener} (eng-review D5).
 *
 * <p>The listener fires at AFTER_COMMIT — by that point the source annotation
 * transaction is already committed. An exception inside the listener must NOT
 * propagate to the caller and MUST be swallowed with a WARN log.
 */
@ExtendWith(MockitoExtension.class)
class AnnotationAuditListenerTest {

    @Mock
    private AnnotationLogRepository repository;

    private AnnotationAuditListener listener;

    @BeforeEach
    void setUp() {
        listener = new AnnotationAuditListener(repository);
    }

    private AnnotationLogEvent event() {
        return new AnnotationLogEvent(
                this,
                UUID.randomUUID(),
                "alice",
                ActionType.MENTION_CREATED,
                UUID.randomUUID(),
                "{\"k\":\"v\"}");
    }

    @Test
    @DisplayName("save failure is swallowed: source tx must not be affected")
    void saveFailureIsSwallowed() {
        when(repository.save(any(AnnotationLogEntity.class)))
                .thenThrow(new RuntimeException("simulated DB outage"));

        assertDoesNotThrow(() -> listener.onAnnotationLog(event()),
                "listener must not propagate exceptions — annotation tx already committed");
    }

    @Test
    @DisplayName("successful save persists all event fields")
    void successfulSavePersistsFields() {
        when(repository.save(any(AnnotationLogEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        AnnotationLogEvent e = event();
        listener.onAnnotationLog(e);

        ArgumentCaptor<AnnotationLogEntity> captor = ArgumentCaptor.forClass(AnnotationLogEntity.class);
        verify(repository, times(1)).save(captor.capture());
        AnnotationLogEntity saved = captor.getValue();
        assertEquals(e.getWorkspaceId(), saved.getWorkspaceId());
        assertEquals(e.getUserId(), saved.getUserId());
        assertEquals(e.getActionType(), saved.getActionType());
        assertEquals(e.getEntityId(), saved.getEntityId());
        assertEquals(e.getPayloadJson(), saved.getPayloadJson());
        assertNotNull(saved.getTimestamp());
    }
}
