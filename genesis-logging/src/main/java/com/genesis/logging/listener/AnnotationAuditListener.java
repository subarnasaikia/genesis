package com.genesis.logging.listener;

import com.genesis.common.event.AnnotationLogEvent;
import com.genesis.logging.entity.AnnotationLogEntity;
import com.genesis.logging.repository.AnnotationLogRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Persists {@link AnnotationLogEvent} to the annotation_log table after the
 * source annotation transaction commits.
 *
 * <p><b>Isolation guarantee:</b> the listener fires at {@code AFTER_COMMIT},
 * so the source tx is already committed before this method runs — a failed
 * audit write cannot roll back the annotation. The save also runs in
 * {@code REQUIRES_NEW} so it has its own transaction boundary, and any
 * exception is swallowed with a WARN log.
 */
@Component
public class AnnotationAuditListener {

    private static final Logger log = LoggerFactory.getLogger(AnnotationAuditListener.class);

    private final AnnotationLogRepository repository;

    public AnnotationAuditListener(AnnotationLogRepository repository) {
        this.repository = repository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAnnotationLog(AnnotationLogEvent event) {
        try {
            AnnotationLogEntity entity = new AnnotationLogEntity();
            entity.setWorkspaceId(event.getWorkspaceId());
            entity.setUserId(event.getUserId());
            entity.setActionType(event.getActionType());
            entity.setEntityId(event.getEntityId());
            entity.setTimestamp(Instant.ofEpochMilli(event.getTimestamp()));
            entity.setPayloadJson(event.getPayloadJson());
            repository.save(entity);
        } catch (Exception ex) {
            log.warn("Audit log failed for entity {} action {}: {}",
                    event.getEntityId(), event.getActionType(), ex.getMessage(), ex);
        }
    }
}
