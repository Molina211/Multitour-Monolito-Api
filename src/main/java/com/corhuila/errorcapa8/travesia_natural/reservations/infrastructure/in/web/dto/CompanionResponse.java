package com.corhuila.errorcapa8.travesia_natural.reservations.infrastructure.in.web.dto;

import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.Companion;

import java.time.LocalDate;

public record CompanionResponse(String name, String document, LocalDate birthDate) {

    public static CompanionResponse from(Companion companion) {
        return new CompanionResponse(companion.name(), companion.document(), companion.birthDate());
    }
}
