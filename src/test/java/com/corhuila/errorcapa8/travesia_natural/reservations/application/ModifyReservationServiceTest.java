package com.corhuila.errorcapa8.travesia_natural.reservations.application;

import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.ReservationNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.PaymentStatus;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.Reservation;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.ReservationStatus;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.ReservedService;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in.ModifyReservationCommand;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.out.ReservationRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantInactiveException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Tenant;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.TenantRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModifyReservationServiceTest {

    private static final String TENANT_ID = "travesia-natural";

    @Mock
    private TenantRepositoryPort tenantRepositoryPort;
    @Mock
    private ReservationRepositoryPort reservationRepositoryPort;

    private ModifyReservationService modifyReservationService;

    @BeforeEach
    void setUp() {
        modifyReservationService = new ModifyReservationService(tenantRepositoryPort, reservationRepositoryPort);
    }

    private Reservation aConfirmedReservation(UUID reservationId) {
        return Reservation.reconstitute(reservationId, TENANT_ID, "customer-1",
                List.of(new ReservedService("tour-laguna", 2, LocalDate.now().plusDays(5), null, null)),
                BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.ZERO,
                ReservationStatus.CONFIRMADA, PaymentStatus.SIN_PAGO, null, Instant.now(), null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, List.of());
    }

    private ModifyReservationCommand aCommand(UUID reservationId) {
        return new ModifyReservationCommand(TENANT_ID, reservationId,
                List.of(new ReservedService("tour-laguna", 3, LocalDate.now().plusDays(6), null, null)),
                BigDecimal.valueOf(150), BigDecimal.valueOf(150), "cambio de fecha", "actor-1");
    }

    @Test
    void modifiesAReservation() {
        UUID reservationId = UUID.randomUUID();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(Tenant.create(TENANT_ID, "Travesia")));
        when(reservationRepositoryPort.findByTenantIdAndReservationId(TENANT_ID, reservationId))
                .thenReturn(Optional.of(aConfirmedReservation(reservationId)));
        when(reservationRepositoryPort.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Reservation result = modifyReservationService.modifyReservation(aCommand(reservationId));

        assertThat(result.finalValue()).isEqualByComparingTo(BigDecimal.valueOf(150));
    }

    @Test
    void rejectsWhenTenantDoesNotExist() {
        UUID reservationId = UUID.randomUUID();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> modifyReservationService.modifyReservation(aCommand(reservationId)))
                .isInstanceOf(TenantNotFoundException.class);
    }

    @Test
    void rejectsWhenTenantIsInactive() {
        UUID reservationId = UUID.randomUUID();
        Tenant inactiveTenant = Tenant.create(TENANT_ID, "Travesia").deactivate();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(inactiveTenant));

        assertThatThrownBy(() -> modifyReservationService.modifyReservation(aCommand(reservationId)))
                .isInstanceOf(TenantInactiveException.class);
    }

    @Test
    void rejectsWhenReservationDoesNotExist() {
        UUID reservationId = UUID.randomUUID();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(Tenant.create(TENANT_ID, "Travesia")));
        when(reservationRepositoryPort.findByTenantIdAndReservationId(TENANT_ID, reservationId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> modifyReservationService.modifyReservation(aCommand(reservationId)))
                .isInstanceOf(ReservationNotFoundException.class);
    }
}
