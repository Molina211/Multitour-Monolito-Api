package com.corhuila.errorcapa8.travesia_natural.cash.application;

import com.corhuila.errorcapa8.travesia_natural.cash.domain.exception.CashRegisterNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.model.CashRegister;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.model.CashMovementType;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CashRegisterQueryServiceTest {

    private static final String TENANT_ID = "travesia-natural";
    private static final LocalDate BUSINESS_DATE = LocalDate.of(2026, 9, 5);

    @Mock
    private TenantRepositoryPort tenantRepositoryPort;
    @Mock
    private CashRegisterRepositoryPort cashRegisterRepositoryPort;
    @Mock
    private ReservationRepositoryPort reservationRepositoryPort;

    private CashRegisterQueryService cashRegisterQueryService;

    @BeforeEach
    void setUp() {
        TenantGuard tenantGuard = new TenantGuard(tenantRepositoryPort);
        RefundsTotalCalculator refundsTotalCalculator = new RefundsTotalCalculator(reservationRepositoryPort);
        cashRegisterQueryService = new CashRegisterQueryService(tenantGuard, cashRegisterRepositoryPort,
                refundsTotalCalculator);
    }

    @Test
    void returnsAnOpenCashRegisterWithLiveTotal() {
        CashRegister open = CashRegister.open(UUID.randomUUID(), TENANT_ID, BUSINESS_DATE, BigDecimal.valueOf(50000))
                .registerMovement(CashMovementType.INGRESO, BigDecimal.valueOf(20000), "venta", "actor-1");
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(Tenant.create(TENANT_ID, "Travesia")));
        when(cashRegisterRepositoryPort.findByTenantIdAndBusinessDate(TENANT_ID, BUSINESS_DATE))
                .thenReturn(Optional.of(open));
        when(reservationRepositoryPort.findAllByTenantId(TENANT_ID)).thenReturn(List.of());

        CashRegister result = cashRegisterQueryService.getByBusinessDate(TENANT_ID, BUSINESS_DATE);

        assertThat(result.totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(70000));
    }

    @Test
    void rejectsWhenNoCashRegisterExistsForTheBusinessDate() {
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(Tenant.create(TENANT_ID, "Travesia")));
        when(cashRegisterRepositoryPort.findByTenantIdAndBusinessDate(TENANT_ID, BUSINESS_DATE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> cashRegisterQueryService.getByBusinessDate(TENANT_ID, BUSINESS_DATE))
                .isInstanceOf(CashRegisterNotFoundException.class);
    }

    @Test
    void listsClosedCashRegisterHistory() {
        CashRegister closed = CashRegister.open(UUID.randomUUID(), TENANT_ID, BUSINESS_DATE,
                BigDecimal.valueOf(50000)).close("actor-1", BigDecimal.ZERO);
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(Tenant.create(TENANT_ID, "Travesia")));
        when(cashRegisterRepositoryPort.findAllClosedByTenantId(TENANT_ID)).thenReturn(List.of(closed));

        assertThat(cashRegisterQueryService.listHistory(TENANT_ID)).containsExactly(closed);
    }
}
