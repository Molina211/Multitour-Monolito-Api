package com.corhuila.errorcapa8.travesia_natural.cash.infrastructure.out.persistence;

import com.corhuila.errorcapa8.travesia_natural.cash.domain.model.CashCorrection;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.model.CashMovement;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.model.CashMovementType;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.model.CashRegister;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.model.CashRegisterStatus;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.port.out.CashRegisterRepositoryPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class CashRegisterRepositoryAdapter implements CashRegisterRepositoryPort {

    private final CashRegisterJpaRepository jpaRepository;

    public CashRegisterRepositoryAdapter(CashRegisterJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    /**
     * Movimientos y correcciones son de solo-agregado (nunca se editan ni se quitan una
     * vez creados, ver {@code CashRegister.registerMovement}/{@code addCorrection}): si la
     * caja ya existe, se actualizan los campos escalares y solo se insertan los hijos
     * nuevos, sin tocar los ya persistidos. Mismo criterio aplicado en `reservations`
     * (spec 001) para corregir el mismo patrón: reconstruir el agregado completo en cada
     * guardado hacía que `orphanRemoval` borrara y reinsertara todo el historial.
     */
    @Override
    @Transactional
    public CashRegister save(CashRegister cashRegister) {
        CashRegisterEntity entity = jpaRepository.findById(cashRegister.cashRegisterId())
                .map(existing -> applyChanges(existing, cashRegister))
                .orElseGet(() -> toNewEntity(cashRegister));

        jpaRepository.save(entity);

        return cashRegister;
    }

    private static CashRegisterEntity applyChanges(CashRegisterEntity entity, CashRegister cashRegister) {
        entity.updateState(cashRegister.status().name(), cashRegister.closedBy(), cashRegister.closedAt(),
                cashRegister.totalAmount());

        List<CashMovement> movements = cashRegister.movements();
        for (int i = entity.getMovements().size(); i < movements.size(); i++) {
            entity.addMovement(toMovementEntity(movements.get(i)));
        }

        List<CashCorrection> corrections = cashRegister.corrections();
        for (int i = entity.getCorrections().size(); i < corrections.size(); i++) {
            entity.addCorrection(toCorrectionEntity(corrections.get(i)));
        }

        return entity;
    }

    private static CashRegisterEntity toNewEntity(CashRegister cashRegister) {
        CashRegisterEntity entity = new CashRegisterEntity(
                cashRegister.cashRegisterId(),
                cashRegister.tenantId(),
                cashRegister.businessDate(),
                cashRegister.baseAmount(),
                cashRegister.status().name(),
                cashRegister.closedBy(),
                cashRegister.closedAt(),
                cashRegister.totalAmount());

        for (CashMovement movement : cashRegister.movements()) {
            entity.addMovement(toMovementEntity(movement));
        }

        for (CashCorrection correction : cashRegister.corrections()) {
            entity.addCorrection(toCorrectionEntity(correction));
        }

        return entity;
    }

    private static CashMovementEntity toMovementEntity(CashMovement movement) {
        return new CashMovementEntity(movement.type().name(), movement.amount(), movement.concept(),
                movement.actorId(), movement.recordedAt());
    }

    private static CashCorrectionEntity toCorrectionEntity(CashCorrection correction) {
        return new CashCorrectionEntity(correction.justification(), correction.appliedBy(), correction.appliedAt());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CashRegister> findByTenantIdAndCashRegisterId(String tenantId, UUID cashRegisterId) {
        return jpaRepository.findByTenantIdAndCashRegisterId(tenantId, cashRegisterId)
                .map(CashRegisterRepositoryAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CashRegister> findByTenantIdAndBusinessDate(String tenantId, LocalDate businessDate) {
        return jpaRepository.findByTenantIdAndBusinessDate(tenantId, businessDate)
                .map(CashRegisterRepositoryAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CashRegister> findAllClosedByTenantId(String tenantId) {
        return jpaRepository.findAllByTenantIdAndStatus(tenantId, CashRegisterStatus.CERRADA.name()).stream()
                .map(CashRegisterRepositoryAdapter::toDomain)
                .toList();
    }

    private static CashRegister toDomain(CashRegisterEntity entity) {
        List<CashMovement> movements = entity.getMovements().stream()
                .map(m -> new CashMovement(CashMovementType.valueOf(m.getType()), m.getAmount(), m.getConcept(),
                        m.getActorId(), m.getRecordedAt()))
                .toList();

        List<CashCorrection> corrections = entity.getCorrections().stream()
                .map(c -> new CashCorrection(c.getJustification(), c.getAppliedBy(), c.getAppliedAt()))
                .toList();

        return CashRegister.reconstitute(
                entity.getCashRegisterId(),
                entity.getTenantId(),
                entity.getBusinessDate(),
                entity.getBaseAmount(),
                CashRegisterStatus.valueOf(entity.getStatus()),
                movements,
                corrections,
                entity.getClosedBy(),
                entity.getClosedAt(),
                entity.getTotalAmount());
    }
}
