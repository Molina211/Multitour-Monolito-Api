package com.corhuila.errorcapa8.travesia_natural.cash.domain.model;

import com.corhuila.errorcapa8.travesia_natural.cash.domain.exception.CashRegisterClosedException;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.exception.CashRegisterNotClosedException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CashRegisterTest {

    private static final String TENANT_ID = "travesia-natural";
    private static final LocalDate BUSINESS_DATE = LocalDate.of(2026, 9, 6);

    private static CashRegister anOpenRegister() {
        return CashRegister.open(UUID.randomUUID(), TENANT_ID, BUSINESS_DATE, BigDecimal.valueOf(100));
    }

    // --- open ---

    @Test
    void opensWithBaseAmountAndNoMovements() {
        CashRegister register = anOpenRegister();

        assertThat(register.status()).isEqualTo(CashRegisterStatus.ABIERTA);
        assertThat(register.baseAmount()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(register.movements()).isEmpty();
        assertThat(register.corrections()).isEmpty();
        assertThat(register.totalAmount()).isNull();
    }

    @Test
    void openRejectsBlankTenantId() {
        assertThatThrownBy(() -> CashRegister.open(UUID.randomUUID(), " ", BUSINESS_DATE, BigDecimal.TEN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenantId is required");
    }

    @Test
    void openRejectsNullBusinessDate() {
        assertThatThrownBy(() -> CashRegister.open(UUID.randomUUID(), TENANT_ID, null, BigDecimal.TEN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("businessDate is required");
    }

    @Test
    void openRejectsNegativeBaseAmount() {
        assertThatThrownBy(() -> CashRegister.open(UUID.randomUUID(), TENANT_ID, BUSINESS_DATE,
                BigDecimal.valueOf(-1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("baseAmount must be a non-negative amount");
    }

    // --- registerMovement / computeTotal ---

    @Test
    void computeTotalCombinesBaseAmountWithIngresosPagosGastosAndRefunds() {
        CashRegister register = anOpenRegister()
                .registerMovement(CashMovementType.INGRESO, BigDecimal.valueOf(50), "venta", "actor-1")
                .registerMovement(CashMovementType.PAGO, BigDecimal.valueOf(20), "pago proveedor", "actor-1")
                .registerMovement(CashMovementType.GASTO, BigDecimal.valueOf(10), "papeleria", "actor-1");

        BigDecimal total = register.computeTotal(BigDecimal.valueOf(5));

        assertThat(total).isEqualByComparingTo(BigDecimal.valueOf(100 + 50 - 20 - 10 - 5));
    }

    @Test
    void computeTotalTreatsNullRefundsAsZero() {
        CashRegister register = anOpenRegister()
                .registerMovement(CashMovementType.INGRESO, BigDecimal.valueOf(50), "venta", "actor-1");

        assertThat(register.computeTotal(null)).isEqualByComparingTo(BigDecimal.valueOf(150));
    }

    @Test
    void registerMovementRejectsWhenRegisterIsClosed() {
        CashRegister closed = anOpenRegister().close("actor-1", BigDecimal.ZERO);

        assertThatThrownBy(() -> closed.registerMovement(CashMovementType.INGRESO, BigDecimal.TEN, "venta",
                "actor-1"))
                .isInstanceOf(CashRegisterClosedException.class);
    }

    // --- close ---

    @Test
    void closeFreezesTotalAmountAndMarksClosedByAndAt() {
        CashRegister register = anOpenRegister()
                .registerMovement(CashMovementType.INGRESO, BigDecimal.valueOf(50), "venta", "actor-1");

        CashRegister closed = register.close("actor-1", BigDecimal.ZERO);

        assertThat(closed.status()).isEqualTo(CashRegisterStatus.CERRADA);
        assertThat(closed.closedBy()).isEqualTo("actor-1");
        assertThat(closed.closedAt()).isNotNull();
        assertThat(closed.totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(150));
    }

    @Test
    void closeRejectsWhenAlreadyClosed() {
        CashRegister closed = anOpenRegister().close("actor-1", BigDecimal.ZERO);

        assertThatThrownBy(() -> closed.close("actor-1", BigDecimal.ZERO))
                .isInstanceOf(CashRegisterClosedException.class);
    }

    @Test
    void closeRejectsBlankActorId() {
        CashRegister register = anOpenRegister();

        assertThatThrownBy(() -> register.close(" ", BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("actorId is required to close a cash register");
    }

    // --- addCorrection ---

    @Test
    void addCorrectionOnlyAllowedWhenClosed() {
        CashRegister register = anOpenRegister();

        assertThatThrownBy(() -> register.addCorrection("ajuste", "actor-1"))
                .isInstanceOf(CashRegisterNotClosedException.class);
    }

    @Test
    void addCorrectionAppendsToClosedRegister() {
        CashRegister closed = anOpenRegister().close("actor-1", BigDecimal.ZERO);

        CashRegister corrected = closed.addCorrection("ajuste por error de digitacion", "actor-2");

        assertThat(corrected.corrections()).hasSize(1);
        assertThat(corrected.corrections().get(0).justification()).isEqualTo("ajuste por error de digitacion");
        assertThat(corrected.corrections().get(0).appliedBy()).isEqualTo("actor-2");
    }
}
