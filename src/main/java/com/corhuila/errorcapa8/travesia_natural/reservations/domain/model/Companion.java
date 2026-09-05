package com.corhuila.errorcapa8.travesia_natural.reservations.domain.model;

import java.time.LocalDate;

public record Companion(String name, String document, LocalDate birthDate) {

    public Companion {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("companion name is required");
        }
        if (document == null || document.isBlank()) {
            throw new IllegalArgumentException("companion document is required");
        }
        if (birthDate == null) {
            throw new IllegalArgumentException("companion birthDate is required");
        }
    }
}
