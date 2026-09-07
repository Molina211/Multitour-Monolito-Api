package com.corhuila.errorcapa8.travesia_natural.cash.application;

import com.corhuila.errorcapa8.travesia_natural.cash.domain.exception.CashRegisterAlreadyOpenException;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.model.CashRegister;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.port.in.OpenCashRegisterCommand;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.port.out.CashRegisterRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Tenant;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.TenantRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenCashRegisterServiceTest {

    private static final String TENANT_ID = "travesia-natural";
    private static final LocalDate BUSINESS_DATE = LocalDate.of(2026, 9, 5);

    @Mock
    private TenantRepositoryPort tenantRepositoryPort;
    @Mock
    private CashRegisterRepositoryPort cashRegisterRepositoryPort;

    private OpenCashRegisterService openCashRegisterService;

    @BeforeEach
    void setUp() {
        TenantGuard tenantGuard = new TenantGuard(tenantRepositoryPort);
        openCashRegisterService = new OpenCashRegisterService(tenantGuard, cashRegisterRepositoryPort);
    }

    @Test
    void opensACashRegisterWhenNoneExistsForTheBusinessDate() {
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(Tenant.create(TENANT_ID, "Travesia")));
        when(cashRegisterRepositoryPort.findByTenantIdAndBusinessDate(TENANT_ID, BUSINESS_DATE))
                .thenReturn(Optional.empty());
        when(cashRegisterRepositoryPort.save(any(CashRegister.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CashRegister result = openCashRegisterService.openCashRegister(
                new OpenCashRegisterCommand(TENANT_ID, BUSINESS_DATE, BigDecimal.valueOf(50000)));

        assertThat(result.baseAmount()).isEqualByComparingTo(BigDecimal.valueOf(50000));
        assertThat(result.businessDate()).isEqualTo(BUSINESS_DATE);
    }

    @Test
    void rejectsWhenTenantDoesNotExist() {
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> openCashRegisterService.openCashRegister(
                new OpenCashRegisterCommand(TENANT_ID, BUSINESS_DATE, BigDecimal.valueOf(50000))))
                .isInstanceOf(TenantNotFoundException.class);

        verify(cashRegisterRepositoryPort, never()).save(any());
    }

    @Test
    void rejectsWhenACashRegisterAlreadyExistsForTheBusinessDate() {
        CashRegister existing = CashRegister.open(UUID.randomUUID(), TENANT_ID, BUSINESS_DATE,
                BigDecimal.valueOf(50000));
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(Tenant.create(TENANT_ID, "Travesia")));
        when(cashRegisterRepositoryPort.findByTenantIdAndBusinessDate(TENANT_ID, BUSINESS_DATE))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> openCashRegisterService.openCashRegister(
                new OpenCashRegisterCommand(TENANT_ID, BUSINESS_DATE, BigDecimal.valueOf(50000))))
                .isInstanceOf(CashRegisterAlreadyOpenException.class);

        verify(cashRegisterRepositoryPort, never()).save(any());
    }
}
