package com.corhuila.errorcapa8.travesia_natural.reservations.application;

import com.corhuila.errorcapa8.travesia_natural.common.audit.AuditRecord;
import com.corhuila.errorcapa8.travesia_natural.common.audit.AuditRecorder;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.ReservationNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in.PaymentFollowupQueryUseCase;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.out.ReservationRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantInactiveException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Tenant;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.TenantStatus;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.TenantRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentFollowupQueryService implements PaymentFollowupQueryUseCase {

    static final String ACTION = "SEGUIMIENTO_PAGO";

    private final TenantRepositoryPort tenantRepositoryPort;
    private final ReservationRepositoryPort reservationRepositoryPort;
    private final AuditRecorder auditRecorder;

    public PaymentFollowupQueryService(TenantRepositoryPort tenantRepositoryPort,
                                        ReservationRepositoryPort reservationRepositoryPort,
                                        AuditRecorder auditRecorder) {
        this.tenantRepositoryPort = tenantRepositoryPort;
        this.reservationRepositoryPort = reservationRepositoryPort;
        this.auditRecorder = auditRecorder;
    }

    @Override
    public List<AuditRecord> listFollowups(String tenantId, UUID reservationId) {
        requireActiveTenant(tenantId);

        reservationRepositoryPort.findByTenantIdAndReservationId(tenantId, reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId.toString()));

        return auditRecorder.findAll().stream()
                .filter(record -> ACTION.equals(record.action()))
                .filter(record -> tenantId.equals(record.tenantId()))
                .filter(record -> reservationId.toString().equals(record.affectedRecordId()))
                .sorted(Comparator.comparing(AuditRecord::recordedAt))
                .toList();
    }

    private void requireActiveTenant(String tenantId) {
        Tenant tenant = tenantRepositoryPort.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));

        if (tenant.tenantStatus() == TenantStatus.INACTIVO) {
            throw new TenantInactiveException(tenant.tenantId());
        }
    }
}
