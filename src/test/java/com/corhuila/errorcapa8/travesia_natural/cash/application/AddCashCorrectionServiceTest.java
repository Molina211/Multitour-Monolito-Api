package com.corhuila.errorcapa8.travesia_natural.cash.application;

import com.corhuila.errorcapa8.travesia_natural.cash.domain.exception.CashRegisterNotClosedException;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.exception.CashRegisterNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.model.CashRegister;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.port.in.AddCashCorrectionCommand;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.port.out.CashRegisterRepositoryPort;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddCashCorrectionServiceTest {

    private static final String TENANT_ID = "travesia-natural";
    private static final LocalDate BUSINESS_DATE = LocalDate.of(2026, 9, 5);

    @Mock
    private TenantRepositoryPort tenantRepositoryPort;
    @Mock
    private CashRegisterRepositoryPort cashRegisterRepositoryPort;

    private AddCashCorrectionService addCashCorrectionService;

    @BeforeEach
    void setUp() {
        TenantGuard tenantGuard = new TenantGuard(tenantRepositoryPort);
        addCashCorrectionService = new AddCashCorrectionService(tenantGuard, cashRegisterRepositoryPort);
    }

    @Test
    void addsACorrectionToAClosedCashRegister() {
        UUID cashRegisterId = UUID.randomUUID();
        CashRegister closed = CashRegister.open(cashRegisterId, TENANT_ID, BUSINESS_DATE, BigDecimal.valueOf(50000))
                .close("actor-1", BigDecimal.ZERO);
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(Tenant.create(TENANT_ID, "Travesia")));
        when(cashRegisterRepositoryPort.findByTenantIdAndCashRegisterId(TENANT_ID, cashRegisterId))
                .thenReturn(Optional.of(closed));
        when(cashRegisterRepositoryPort.save(any(CashRegister.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CashRegister result = addCashCorrectionService.addCorrection(
                new AddCashCorrectionCommand(TENANT_ID, cashRegisterId, "ajuste por diferencia", "actor-2"));

        assertThat(result.corrections()).hasSize(1);
    }

    @Test
    void rejectsWhenCashRegisterDoesNotExist() {
        UUID cashRegisterId = UUID.randomUUID();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(Tenant.create(TENANT_ID, "Travesia")));
        when(cashRegisterRepositoryPort.findByTenantIdAndCashRegisterId(TENANT_ID, cashRegisterId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> addCashCorrectionService.addCorrection(
                new AddCashCorrectionCommand(TENANT_ID, cashRegisterId, "ajuste", "actor-2")))
                .isInstanceOf(CashRegisterNotFoundException.class);
    }

    @Test
    void rejectsWhenCashRegisterIsStillOpen() {
        UUID cashRegisterId = UUID.randomUUID();
        CashRegister open = CashRegister.open(cashRegisterId, TENANT_ID, BUSINESS_DATE, BigDecimal.valueOf(50000));
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(Tenant.create(TENANT_ID, "Travesia")));
        when(cashRegisterRepositoryPort.findByTenantIdAndCashRegisterId(TENANT_ID, cashRegisterId))
                .thenReturn(Optional.of(open));

        assertThatThrownBy(() -> addCashCorrectionService.addCorrection(
                new AddCashCorrectionCommand(TENANT_ID, cashRegisterId, "ajuste", "actor-2")))
                .isInstanceOf(CashRegisterNotClosedException.class);
    }
}
