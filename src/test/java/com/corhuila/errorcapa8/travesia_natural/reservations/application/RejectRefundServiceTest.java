package com.corhuila.errorcapa8.travesia_natural.reservations.application;

import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.RefundActionNotAllowedException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.ReservationNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.PaymentStatus;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.RefundDecisionStatus;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.Reservation;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.ReservationStatus;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in.RejectRefundCommand;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.out.ReservationRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantInactiveException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Membership;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Tenant;
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
class RejectRefundServiceTest {

    private static final String TENANT_ID = "travesia-natural";

    @Mock
    private TenantRepositoryPort tenantRepositoryPort;
    @Mock
    private MembershipRepositoryPort membershipRepositoryPort;
    @Mock
    private ReservationRepositoryPort reservationRepositoryPort;

    private RejectRefundService rejectRefundService;

    @BeforeEach
    void setUp() {
        rejectRefundService = new RejectRefundService(tenantRepositoryPort, membershipRepositoryPort,
                reservationRepositoryPort);
    }

    private Reservation aReservationPendingAuthorization(UUID reservationId) {
        return Reservation.reconstitute(reservationId, TENANT_ID, "customer-1", List.of(), BigDecimal.valueOf(100),
                BigDecimal.valueOf(100), BigDecimal.ZERO, BigDecimal.valueOf(100),
                ReservationStatus.CANCELADA, PaymentStatus.SALDO_A_FAVOR_PENDIENTE, null, Instant.now(), null, null,
                "cancelada", "actor-1", Instant.now(), RefundDecisionStatus.PENDIENTE_AUTORIZACION, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, List.of());
    }

    private Membership anAdministrator() {
        return Membership.createAdministrator(TENANT_ID, "admin@travesia.com", "hashed-password");
    }

    @Test
    void rejectsARefund() {
        UUID reservationId = UUID.randomUUID();
        Membership administrator = anAdministrator();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(Tenant.create(TENANT_ID, "Travesia")));
        when(membershipRepositoryPort.findByTenantIdAndMembershipId(TENANT_ID, administrator.membershipId()))
                .thenReturn(Optional.of(administrator));
        when(reservationRepositoryPort.findByTenantIdAndReservationId(TENANT_ID, reservationId))
                .thenReturn(Optional.of(aReservationPendingAuthorization(reservationId)));
        when(reservationRepositoryPort.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Reservation result = rejectRefundService.rejectRefund(new RejectRefundCommand(TENANT_ID,
                reservationId, administrator.membershipId().toString(), "no procede"));

        assertThat(result.refundDecisionStatus()).isEqualTo(RefundDecisionStatus.RECHAZADA);
    }

    @Test
    void rejectsWhenTenantDoesNotExist() {
        UUID reservationId = UUID.randomUUID();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rejectRefundService.rejectRefund(
                new RejectRefundCommand(TENANT_ID, reservationId, "actor-1", "no procede")))
                .isInstanceOf(TenantNotFoundException.class);
    }

    @Test
    void rejectsWhenTenantIsInactive() {
        UUID reservationId = UUID.randomUUID();
        Tenant inactiveTenant = Tenant.create(TENANT_ID, "Travesia").deactivate();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(inactiveTenant));

        assertThatThrownBy(() -> rejectRefundService.rejectRefund(
                new RejectRefundCommand(TENANT_ID, reservationId, "actor-1", "no procede")))
                .isInstanceOf(TenantInactiveException.class);
    }

    @Test
    void rejectsWhenActorIdIsNotAValidMembershipId() {
        UUID reservationId = UUID.randomUUID();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(Tenant.create(TENANT_ID, "Travesia")));

        assertThatThrownBy(() -> rejectRefundService.rejectRefund(
                new RejectRefundCommand(TENANT_ID, reservationId, "not-a-uuid", "no procede")))
                .isInstanceOf(RefundActionNotAllowedException.class);
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

        assertThatThrownBy(() -> rejectRefundService.rejectRefund(new RejectRefundCommand(TENANT_ID,
                reservationId, administrator.membershipId().toString(), "no procede")))
                .isInstanceOf(ReservationNotFoundException.class);
    }
}
