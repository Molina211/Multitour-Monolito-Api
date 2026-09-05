package com.corhuila.errorcapa8.travesia_natural.reservations.application;

import com.corhuila.errorcapa8.travesia_natural.common.audit.AuditRecord;
import com.corhuila.errorcapa8.travesia_natural.common.audit.AuditRecorder;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.InvalidReservationException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.ReservationNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in.RegisterPaymentFollowupCommand;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in.RegisterPaymentFollowupUseCase;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.out.ReservationRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantInactiveException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Tenant;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.TenantStatus;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.TenantRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class RegisterPaymentFollowupService implements RegisterPaymentFollowupUseCase {

    static final String ACTION = "SEGUIMIENTO_PAGO";

    private final TenantRepositoryPort tenantRepositoryPort;
    private final ReservationRepositoryPort reservationRepositoryPort;
    private final AuditRecorder auditRecorder;

    public RegisterPaymentFollowupService(TenantRepositoryPort tenantRepositoryPort,
                                           ReservationRepositoryPort reservationRepositoryPort,
                                           AuditRecorder auditRecorder) {
        this.tenantRepositoryPort = tenantRepositoryPort;
        this.reservationRepositoryPort = reservationRepositoryPort;
        this.auditRecorder = auditRecorder;
    }

    @Override
    public AuditRecord registerFollowup(RegisterPaymentFollowupCommand command) {
        if (command.note() == null || command.note().isBlank()) {
            throw new InvalidReservationException("note is required to register a payment followup");
        }

        requireActiveTenant(command.tenantId());

        reservationRepositoryPort.findByTenantIdAndReservationId(command.tenantId(), command.reservationId())
                .orElseThrow(() -> new ReservationNotFoundException(command.reservationId().toString()));

        return auditRecorder.record(AuditRecord.of(
                command.tenantId(), command.actorId(), ACTION, command.reservationId().toString(), command.note()));
    }

    private void requireActiveTenant(String tenantId) {
        Tenant tenant = tenantRepositoryPort.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));

        if (tenant.tenantStatus() == TenantStatus.INACTIVO) {
            throw new TenantInactiveException(tenant.tenantId());
        }
    }
}
