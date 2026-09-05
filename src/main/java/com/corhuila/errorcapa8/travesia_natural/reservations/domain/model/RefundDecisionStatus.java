package com.corhuila.errorcapa8.travesia_natural.reservations.domain.model;

/**
 * Estados de la decisión de devolución (spec 019, RN-RES-008): separa la
 * determinación de la solicitud, la autorización/rechazo del Administrador y la
 * resolución final (ejecución con salida de dinero o saldo a favor registrado).
 */
public enum RefundDecisionStatus {
    PENDIENTE_AUTORIZACION("Pendiente de autorizacion"),
    AUTORIZADA("Autorizada"),
    RECHAZADA("Rechazada"),
    EJECUTADA("Ejecutada"),
    SALDO_A_FAVOR_REGISTRADO("Saldo a favor pendiente");

    private final String label;

    RefundDecisionStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static RefundDecisionStatus fromLabel(String label) {
        for (RefundDecisionStatus status : values()) {
            if (status.label.equals(label)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown RefundDecisionStatus label: " + label);
    }
}
