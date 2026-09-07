package com.corhuila.errorcapa8.travesia_natural.operations.application;

import com.corhuila.errorcapa8.travesia_natural.operations.domain.model.Execution;
import com.corhuila.errorcapa8.travesia_natural.operations.domain.port.in.RegisterExecutionCommand;
import com.corhuila.errorcapa8.travesia_natural.operations.domain.port.out.ExecutionRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.ReservationNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.PaymentStatus;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.Reservation;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.ReservationStatus;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.out.ReservationRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantInactiveException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Tenant;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.TenantRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterExecutionServiceTest {

    private static final String TENANT_ID = "travesia-natural";

    @Mock
    private TenantRepositoryPort tenantRepositoryPort;
    @Mock
    private ReservationRepositoryPort reservationRepositoryPort;
    @Mock
    private ExecutionRepositoryPort executionRepositoryPort;

    private RegisterExecutionService registerExecutionService;

    @BeforeEach
    void setUp() {
        registerExecutionService = new RegisterExecutionService(tenantRepositoryPort, reservationRepositoryPort,
                executionRepositoryPort);
    }

    private Reservation aConfirmedReservation(UUID reservationId) {
        return Reservation.reconstitute(reservationId, TENANT_ID, "customer-1", List.of(), BigDecimal.TEN,
                BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, ReservationStatus.CONFIRMADA,
                PaymentStatus.PAGADO, "EFECTIVO", Instant.now(), null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                List.of());
    }

    @Test
    void registersAnExecution() {
        UUID reservationId = UUID.randomUUID();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(Tenant.create(TENANT_ID, "Travesia")));
        when(reservationRepositoryPort.findByTenantIdAndReservationId(TENANT_ID, reservationId))
                .thenReturn(Optional.of(aConfirmedReservation(reservationId)));
        when(executionRepositoryPort.save(any(Execution.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Execution result = registerExecutionService.registerExecution(
                new RegisterExecutionCommand(TENANT_ID, reservationId, true, 10, null, "actor-1"));

        assertThat(result.served()).isTrue();

        ArgumentCaptor<Reservation> reservationCaptor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepositoryPort).save(reservationCaptor.capture());
        assertThat(reservationCaptor.getValue().reservationStatus()).isEqualTo(ReservationStatus.EN_EJECUCION);
    }

    @Test
    void rejectsWhenTenantDoesNotExist() {
        UUID reservationId = UUID.randomUUID();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registerExecutionService.registerExecution(
                new RegisterExecutionCommand(TENANT_ID, reservationId, true, 10, null, "actor-1")))
                .isInstanceOf(TenantNotFoundException.class);
    }

    @Test
    void rejectsWhenTenantIsInactive() {
        UUID reservationId = UUID.randomUUID();
        Tenant inactiveTenant = Tenant.create(TENANT_ID, "Travesia").deactivate();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(inactiveTenant));

        assertThatThrownBy(() -> registerExecutionService.registerExecution(
                new RegisterExecutionCommand(TENANT_ID, reservationId, true, 10, null, "actor-1")))
                .isInstanceOf(TenantInactiveException.class);
    }

    @Test
    void rejectsWhenReservationDoesNotExist() {
        UUID reservationId = UUID.randomUUID();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(Tenant.create(TENANT_ID, "Travesia")));
        when(reservationRepositoryPort.findByTenantIdAndReservationId(TENANT_ID, reservationId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> registerExecutionService.registerExecution(
                new RegisterExecutionCommand(TENANT_ID, reservationId, true, 10, null, "actor-1")))
                .isInstanceOf(ReservationNotFoundException.class);
    }
}
