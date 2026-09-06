package com.corhuila.errorcapa8.travesia_natural.cash.application;

import com.corhuila.errorcapa8.travesia_natural.cash.domain.exception.CashRegisterNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.model.CashRegister;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.model.CashRegisterStatus;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.port.in.CloseCashRegisterCommand;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.port.out.CashRegisterRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.out.ReservationRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Tenant;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.TenantRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CloseCashRegisterServiceTest {

    private static final String TENANT_ID = "travesia-natural";
    private static final LocalDate BUSINESS_DATE = LocalDate.of(2026, 9, 5);

    @Mock
    private TenantRepositoryPort tenantRepositoryPort;
    @Mock
    private CashRegisterRepositoryPort cashRegisterRepositoryPort;
    @Mock
    private ReservationRepositoryPort reservationRepositoryPort;

    private CloseCashRegisterService closeCashRegisterService;

    @BeforeEach
    void setUp() {
        TenantGuard tenantGuard = new TenantGuard(tenantRepositoryPort);
        RefundsTotalCalculator refundsTotalCalculator = new RefundsTotalCalculator(reservationRepositoryPort);
        closeCashRegisterService = new CloseCashRegisterService(tenantGuard, cashRegisterRepositoryPort,
                refundsTotalCalculator);
    }

    @Test
    void closesAnOpenCashRegister() {
        UUID cashRegisterId = UUID.randomUUID();
        CashRegister cashRegister = CashRegister.open(cashRegisterId, TENANT_ID, BUSINESS_DATE,
                BigDecimal.valueOf(50000));
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(Tenant.create(TENANT_ID, "Travesia")));
        when(cashRegisterRepositoryPort.findByTenantIdAndCashRegisterId(TENANT_ID, cashRegisterId))
                .thenReturn(Optional.of(cashRegister));
        when(reservationRepositoryPort.findAllByTenantId(TENANT_ID)).thenReturn(List.of());
        when(cashRegisterRepositoryPort.save(any(CashRegister.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CashRegister result = closeCashRegisterService.closeCashRegister(
                new CloseCashRegisterCommand(TENANT_ID, cashRegisterId, "actor-1"));

        assertThat(result.status()).isEqualTo(CashRegisterStatus.CERRADA);
    }

    @Test
    void rejectsWhenCashRegisterDoesNotExist() {
        UUID cashRegisterId = UUID.randomUUID();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(Tenant.create(TENANT_ID, "Travesia")));
        when(cashRegisterRepositoryPort.findByTenantIdAndCashRegisterId(TENANT_ID, cashRegisterId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> closeCashRegisterService.closeCashRegister(
                new CloseCashRegisterCommand(TENANT_ID, cashRegisterId, "actor-1")))
                .isInstanceOf(CashRegisterNotFoundException.class);
    }
}
