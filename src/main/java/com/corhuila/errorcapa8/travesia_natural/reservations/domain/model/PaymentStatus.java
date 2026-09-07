package com.corhuila.errorcapa8.travesia_natural.reservations.domain.model;

public enum PaymentStatus {
    SIN_PAGO("Sin pago"),
    EN_VALIDACION("En validacion"),
    PARCIAL("Parcial"),
    PAGADO("Pagado"),
    RECHAZADO("Rechazado"),
    SALDO_A_FAVOR_PENDIENTE("Saldo a favor pendiente"),
    DEVUELTO_PARCIAL_O_TOTAL("Devuelto parcial o total");

    private final String label;

    PaymentStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static PaymentStatus fromLabel(String label) {
        for (PaymentStatus status : values()) {
            if (status.label.equals(label)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown PaymentStatus label: " + label);
    }
}
