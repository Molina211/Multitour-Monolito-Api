package com.corhuila.errorcapa8.travesia_natural.reservations.application;

import com.corhuila.errorcapa8.travesia_natural.common.audit.AuditRecord;
import com.corhuila.errorcapa8.travesia_natural.common.audit.AuditRecorder;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.ReservationNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.Reservation;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in.ApplyDiscountCommand;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in.ApplyDiscountUseCase;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.out.ReservationRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantInactiveException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Tenant;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.TenantStatus;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.TenantRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class ApplyDiscountReservationService implements ApplyDiscountUseCase {

    private final TenantRepositoryPort tenantRepositoryPort;
    private final ReservationRepositoryPort reservationRepositoryPort;
    private final AuditRecorder auditRecorder;

    public ApplyDiscountReservationService(TenantRepositoryPort tenantRepositoryPort,
                                            ReservationRepositoryPort reservationRepositoryPort,
                                            AuditRecorder auditRecorder) {
        this.tenantRepositoryPort = tenantRepositoryPort;
        this.reservationRepositoryPort = reservationRepositoryPort;
        this.auditRecorder = auditRecorder;
    }

    @Override
    public Reservation applyDiscount(ApplyDiscountCommand command) {
        requireActiveTenant(command.tenantId());

        Reservation reservation = reservationRepositoryPort
                .findByTenantIdAndReservationId(command.tenantId(), command.reservationId())
                .orElseThrow(() -> new ReservationNotFoundException(command.reservationId().toString()));

        String previousFinalValue = reservation.finalValue().toPlainString();

        Reservation discounted = reservation.applyDiscount(command.percentage(), command.reason(),
                command.actorId());

        Reservation saved = reservationRepositoryPort.save(discounted);

        auditRecorder.record(AuditRecord.of(
                command.tenantId(), command.actorId(), "RESERVATION_DISCOUNT_APPLIED",
                saved.reservationId().toString(), command.reason(), previousFinalValue,
                saved.finalValue().toPlainString(), "reservations",
                "Aplicación de descuento adicional: " + command.percentage() + "%"));

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
