package com.corhuila.errorcapa8.travesia_natural.cash.domain.model;

/**
 * Tipos registrables a mano en el endpoint genérico de movimientos (spec 013). No incluye
 * {@code DEVOLUCION}: esa se calcula en vivo desde las devoluciones ejecutadas de
 * {@code reservations} (spec 012), nunca se registra manualmente.
 *
 * <p>{@code label()} usa los mismos literales en español que {@code operator-cash.service.ts}
 * en el Frontend (rama {@code develop}, aún no integrada por HTTP): sin este alineamiento,
 * el día que se conecten, ningún movimiento podría registrarse.</p>
 */
public enum CashMovementType {
    INGRESO("Ingreso"),
    PAGO("Pago operacional"),
    GASTO("Gasto");

    private final String label;

    CashMovementType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static CashMovementType fromLabel(String label) {
        for (CashMovementType type : values()) {
            if (type.label.equals(label)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown CashMovementType label: " + label);
    }
}
