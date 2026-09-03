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
}
