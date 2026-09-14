package com.corhuila.errorcapa8.travesia_natural.reservations.application;

import com.corhuila.errorcapa8.travesia_natural.common.audit.AuditRecord;
import com.corhuila.errorcapa8.travesia_natural.common.audit.AuditRecorder;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.InvalidReservationException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.ReservationNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.SupportValidationNotAllowedException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.PaymentStatus;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.Reservation;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.ReservationStatus;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in.DecidePaymentSupportCommand;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.out.ReservationRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantInactiveException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Membership;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Tenant;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.TenantStatus;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.MembershipRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.TenantRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DecidePaymentSupportServiceTest {

    private static final String TENANT_ID = "travesia-natural";

    @Mock
    private TenantRepositoryPort tenantRepositoryPort;
    @Mock
    private ReservationRepositoryPort reservationRepositoryPort;
    @Mock
    private MembershipRepositoryPort membershipRepositoryPort;
    @Mock
    private AuditRecorder auditRecorder;

    private DecidePaymentSupportService decidePaymentSupportService;

    @BeforeEach
    void setUp() {
        decidePaymentSupportService = new DecidePaymentSupportService(tenantRepositoryPort,
                reservationRepositoryPort, membershipRepositoryPort, auditRecorder);
    }

    private Reservation aReservationPendingTransferSupport(UUID reservationId) {
        return Reservation.reconstitute(reservationId, TENANT_ID, "customer-1", List.of(), BigDecimal.valueOf(100),
                BigDecimal.valueOf(100), BigDecimal.valueOf(50), BigDecimal.ZERO, ReservationStatus.CONFIRMADA,
                PaymentStatus.EN_VALIDACION, "TRANSFERENCIA", Instant.now(), BigDecimal.valueOf(50), "ref-1",
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, List.of());
    }

    private Membership anAdministrator() {
        return Membership.createAdministrator(TENANT_ID, "admin@travesia.com", "hashed-password");
    }

    @Test
    void approvesAPaymentSupport() {
        UUID reservationId = UUID.randomUUID();
        Membership administrator = anAdministrator();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(Tenant.create(TENANT_ID, "Travesia")));
        when(membershipRepositoryPort.findByTenantIdAndMembershipId(TENANT_ID, administrator.membershipId()))
                .thenReturn(Optional.of(administrator));
        when(reservationRepositoryPort.findByTenantIdAndReservationId(TENANT_ID, reservationId))
                .thenReturn(Optional.of(aReservationPendingTransferSupport(reservationId)));
        when(reservationRepositoryPort.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Reservation result = decidePaymentSupportService.decidePaymentSupport(new DecidePaymentSupportCommand(
                TENANT_ID, reservationId, "APPROVE", "soporte valido", administrator.membershipId().toString()));

        assertThat(result.paymentStatus()).isEqualTo(PaymentStatus.PAGADO);
    }

    @Test
    void rejectsAPaymentSupport() {
        UUID reservationId = UUID.randomUUID();
        Membership administrator = anAdministrator();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(Tenant.create(TENANT_ID, "Travesia")));
        when(membershipRepositoryPort.findByTenantIdAndMembershipId(TENANT_ID, administrator.membershipId()))
                .thenReturn(Optional.of(administrator));
        when(reservationRepositoryPort.findByTenantIdAndReservationId(TENANT_ID, reservationId))
                .thenReturn(Optional.of(aReservationPendingTransferSupport(reservationId)));
        when(reservationRepositoryPort.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Reservation result = decidePaymentSupportService.decidePaymentSupport(new DecidePaymentSupportCommand(
                TENANT_ID, reservationId, "REJECT", "soporte invalido", administrator.membershipId().toString()));

        assertThat(result.paymentStatus()).isEqualTo(PaymentStatus.RECHAZADO);
    }

    @Test
    void rejectsWhenReasonIsBlank() {
        UUID reservationId = UUID.randomUUID();

        assertThatThrownBy(() -> decidePaymentSupportService.decidePaymentSupport(
                new DecidePaymentSupportCommand(TENANT_ID, reservationId, "APPROVE", " ", "actor-1")))
                .isInstanceOf(InvalidReservationException.class);
    }

    @Test
    void rejectsWhenTenantDoesNotExist() {
        UUID reservationId = UUID.randomUUID();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> decidePaymentSupportService.decidePaymentSupport(
                new DecidePaymentSupportCommand(TENANT_ID, reservationId, "APPROVE", "soporte valido", "actor-1")))
                .isInstanceOf(TenantNotFoundException.class);
    }

    @Test
    void rejectsWhenTenantIsInactive() {
        UUID reservationId = UUID.randomUUID();
        Tenant inactiveTenant = Tenant.create(TENANT_ID, "Travesia").deactivate();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(inactiveTenant));

        assertThatThrownBy(() -> decidePaymentSupportService.decidePaymentSupport(
                new DecidePaymentSupportCommand(TENANT_ID, reservationId, "APPROVE", "soporte valido", "actor-1")))
                .isInstanceOf(TenantInactiveException.class);
    }

    @Test
    void rejectsWhenActorIsNotAllowedToValidateSupport() {
        UUID reservationId = UUID.randomUUID();
        Membership collaborator = Membership.createOperationalCollaborator(TENANT_ID, "Colaborador",
                "colaborador@travesia.com", "hashed-password");
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(Tenant.create(TENANT_ID, "Travesia")));
        when(membershipRepositoryPort.findByTenantIdAndMembershipId(TENANT_ID, collaborator.membershipId()))
                .thenReturn(Optional.of(collaborator));

        assertThatThrownBy(() -> decidePaymentSupportService.decidePaymentSupport(new DecidePaymentSupportCommand(
                TENANT_ID, reservationId, "APPROVE", "soporte valido", collaborator.membershipId().toString())))
                .isInstanceOf(SupportValidationNotAllowedException.class);
    }

    @Test
    void rejectsWhenReservationDoesNotExist() {
        UUID reservationId = UUID.randomUUID();
        Membership administrator = anAdministrator();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(Tenant.create(TENANT_ID, "Travesia")));
        when(membershipRepositoryPort.findByTenantIdAndMembershipId(TENANT_ID, administrator.membershipId()))
                .thenReturn(Optional.of(administrator));
        when(reservationRepositoryPort.findByTenantIdAndReservationId(TENANT_ID, reservationId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> decidePaymentSupportService.decidePaymentSupport(new DecidePaymentSupportCommand(
                TENANT_ID, reservationId, "APPROVE", "soporte valido", administrator.membershipId().toString())))
                .isInstanceOf(ReservationNotFoundException.class);
    }
}
