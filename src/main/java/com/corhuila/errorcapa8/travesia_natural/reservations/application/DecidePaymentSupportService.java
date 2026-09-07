package com.corhuila.errorcapa8.travesia_natural.reservations.application;

import com.corhuila.errorcapa8.travesia_natural.common.audit.AuditRecord;
import com.corhuila.errorcapa8.travesia_natural.common.audit.AuditRecorder;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.InvalidReservationException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.ReservationNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.SupportValidationNotAllowedException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.Reservation;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in.DecidePaymentSupportCommand;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in.DecidePaymentSupportUseCase;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.out.ReservationRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantInactiveException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Membership;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.MembershipRole;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Tenant;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.TenantStatus;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.MembershipRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.TenantRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DecidePaymentSupportService implements DecidePaymentSupportUseCase {

    private final TenantRepositoryPort tenantRepositoryPort;
    private final ReservationRepositoryPort reservationRepositoryPort;
    private final MembershipRepositoryPort membershipRepositoryPort;
    private final AuditRecorder auditRecorder;

    public DecidePaymentSupportService(TenantRepositoryPort tenantRepositoryPort,
                                        ReservationRepositoryPort reservationRepositoryPort,
                                        MembershipRepositoryPort membershipRepositoryPort,
                                        AuditRecorder auditRecorder) {
        this.tenantRepositoryPort = tenantRepositoryPort;
        this.reservationRepositoryPort = reservationRepositoryPort;
        this.membershipRepositoryPort = membershipRepositoryPort;
        this.auditRecorder = auditRecorder;
    }

    @Override
    public Reservation decidePaymentSupport(DecidePaymentSupportCommand command) {
        if (command.reason() == null || command.reason().isBlank()) {
            throw new InvalidReservationException("reason is required to decide a payment support");
        }

        Tenant tenant = requireActiveTenant(command.tenantId());
        requireSupportValidationAllowed(tenant, command.actorId());

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

    private Tenant requireActiveTenant(String tenantId) {
        Tenant tenant = tenantRepositoryPort.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));

        if (tenant.tenantStatus() == TenantStatus.INACTIVO) {
            throw new TenantInactiveException(tenant.tenantId());
        }

        return tenant;
    }

    private void requireSupportValidationAllowed(Tenant tenant, String actorId) {
        if (actorId == null) {
            throw new SupportValidationNotAllowedException("actorId must be a valid membershipId: null");
        }

        UUID actorMembershipId;
        try {
            actorMembershipId = UUID.fromString(actorId);
        } catch (IllegalArgumentException e) {
            throw new SupportValidationNotAllowedException("actorId must be a valid membershipId: " + actorId);
        }

        Membership actor = membershipRepositoryPort.findByTenantIdAndMembershipId(tenant.tenantId(), actorMembershipId)
                .orElseThrow(() -> new SupportValidationNotAllowedException(
                        "membership not found for actorId: " + actorId + " in tenant " + tenant.tenantId()));

        if (actor.role() == MembershipRole.ADMINISTRATOR) {
            return;
        }

        if (actor.role() == MembershipRole.OPERATIONAL_COLLABORATOR && tenant.allowCollaboratorSupportValidation()) {
            return;
        }

        throw new SupportValidationNotAllowedException(
                "actor role " + actor.role() + " is not allowed to decide a payment support for tenant "
                        + tenant.tenantId());
    }
}
