package com.corhuila.errorcapa8.travesia_natural.common.audit.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_records")
public class AuditRecordEntity {

    @Id
    @Column(name = "audit_record_id")
    private UUID auditRecordId;

    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "actor_id", nullable = false)
    private String actorId;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "affected_record_id", nullable = false)
    private String affectedRecordId;

    @Column(name = "reason")
    private String reason;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "previous_value")
    private String previousValue;

    @Column(name = "new_value")
    private String newValue;

    @Column(name = "channel_or_module")
    private String channelOrModule;

    @Column(name = "functional_process_reference")
    private String functionalProcessReference;

    protected AuditRecordEntity() {
        // JPA
    }

    public AuditRecordEntity(UUID auditRecordId, String tenantId, String actorId, String action,
                              String affectedRecordId, String reason, Instant recordedAt,
                              String previousValue, String newValue, String channelOrModule,
                              String functionalProcessReference) {
        this.auditRecordId = auditRecordId;
        this.tenantId = tenantId;
        this.actorId = actorId;
        this.action = action;
        this.affectedRecordId = affectedRecordId;
        this.reason = reason;
        this.recordedAt = recordedAt;
        this.previousValue = previousValue;
        this.newValue = newValue;
        this.channelOrModule = channelOrModule;
        this.functionalProcessReference = functionalProcessReference;
    }

    public UUID getAuditRecordId() {
        return auditRecordId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getActorId() {
        return actorId;
    }

    public String getAction() {
        return action;
    }

    public String getAffectedRecordId() {
        return affectedRecordId;
    }

    public String getReason() {
        return reason;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public String getPreviousValue() {
        return previousValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public String getChannelOrModule() {
        return channelOrModule;
    }

    public String getFunctionalProcessReference() {
        return functionalProcessReference;
    }
}
