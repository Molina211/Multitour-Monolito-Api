package com.corhuila.errorcapa8.travesia_natural.reservations.application;

import com.corhuila.errorcapa8.travesia_natural.common.audit.AuditRecord;
import com.corhuila.errorcapa8.travesia_natural.common.audit.AuditRecorder;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.InvalidReservationException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.ReservationNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.Reservation;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in.DecidePaymentSupportCommand;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in.DecidePaymentSupportUseCase;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.out.ReservationRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantInactiveException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Tenant;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.TenantStatus;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.TenantRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class DecidePaymentSupportService implements DecidePaymentSupportUseCase {

    private final TenantRepositoryPort tenantRepositoryPort;
    private final ReservationRepositoryPort reservationRepositoryPort;
    private final AuditRecorder auditRecorder;

    public DecidePaymentSupportService(TenantRepositoryPort tenantRepositoryPort,
                                        ReservationRepositoryPort reservationRepositoryPort,
                                        AuditRecorder auditRecorder) {
        this.tenantRepositoryPort = tenantRepositoryPort;
        this.reservationRepositoryPort = reservationRepositoryPort;
        this.auditRecorder = auditRecorder;
    }

    @Override
    public Reservation decidePaymentSupport(DecidePaymentSupportCommand command) {
        if (command.reason() == null || command.reason().isBlank()) {
            throw new InvalidReservationException("reason is required to decide a payment support");
        }

        requireActiveTenant(command.tenantId());

        Reservation reservation = reservationRepositoryPort
                .findByTenantIdAndReservationId(command.tenantId(), command.reservationId())
                .orElseThrow(() -> new ReservationNotFoundException(command.reservationId().toString()));

        Reservation updated;
        String action;
        if ("APPROVE".equals(command.decision())) {
            updated = reservation.approveTransferPayment();
            action = "APROBAR_SOPORTE_PAGO";
        } else if ("REJECT".equals(command.decision())) {
            updated = reservation.rejectTransferPayment();
            action = "RECHAZAR_SOPORTE_PAGO";
        } else {
            throw new InvalidReservationException("unknown payment support decision: " + command.decision());
        }

        Reservation saved = reservationRepositoryPort.save(updated);

        auditRecorder.record(AuditRecord.of(
                command.tenantId(), command.actorId(), action, command.reservationId().toString(),
                command.reason()));

        return saved;
    }

    private void requireActiveTenant(String tenantId) {
        Tenant tenant = tenantRepositoryPort.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));

        if (tenant.tenantStatus() == TenantStatus.INACTIVO) {
            throw new TenantInactiveException(tenant.tenantId());
        }
    }
}
