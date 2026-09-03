package com.corhuila.errorcapa8.travesia_natural.reservations.domain.model;

public enum ReservationStatus {
    PENDIENTE_DE_PAGO("Pendiente de pago"),
    CONFIRMADA("Confirmada"),
    EN_EJECUCION("En ejecucion"),
    FINALIZADA("Finalizada"),
    CANCELADA("Cancelada");

    private final String label;

    ReservationStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static ReservationStatus fromLabel(String label) {
        for (ReservationStatus status : values()) {
            if (status.label.equals(label)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown ReservationStatus label: " + label);
    }
}
