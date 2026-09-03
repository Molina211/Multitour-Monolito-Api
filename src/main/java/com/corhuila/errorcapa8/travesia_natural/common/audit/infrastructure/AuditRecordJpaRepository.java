package com.corhuila.errorcapa8.travesia_natural.common.audit.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditRecordJpaRepository extends JpaRepository<AuditRecordEntity, UUID> {
}
