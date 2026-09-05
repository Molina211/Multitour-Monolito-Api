package com.corhuila.errorcapa8.travesia_natural.common.audit.infrastructure;

import com.corhuila.errorcapa8.travesia_natural.common.audit.AuditRecord;
import com.corhuila.errorcapa8.travesia_natural.common.audit.AuditRecorder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AuditRecorderAdapter implements AuditRecorder {

    private final AuditRecordJpaRepository jpaRepository;

    public AuditRecorderAdapter(AuditRecordJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public AuditRecord record(AuditRecord auditRecord) {
        jpaRepository.save(new AuditRecordEntity(
                auditRecord.auditRecordId(),
                auditRecord.tenantId(),
                auditRecord.actorId(),
                auditRecord.action(),
                auditRecord.affectedRecordId(),
                auditRecord.reason(),
                auditRecord.recordedAt(),
                auditRecord.previousValue(),
                auditRecord.newValue(),
                auditRecord.channelOrModule(),
                auditRecord.functionalProcessReference()));

        return auditRecord;
    }

    @Override
    public List<AuditRecord> findAll() {
        return jpaRepository.findAll().stream().map(AuditRecorderAdapter::toDomain).toList();
    }

    private static AuditRecord toDomain(AuditRecordEntity entity) {
        return new AuditRecord(
                entity.getAuditRecordId(),
                entity.getTenantId(),
                entity.getActorId(),
                entity.getAction(),
                entity.getAffectedRecordId(),
                entity.getReason(),
                entity.getRecordedAt(),
                entity.getPreviousValue(),
                entity.getNewValue(),
                entity.getChannelOrModule(),
                entity.getFunctionalProcessReference());
    }
}
