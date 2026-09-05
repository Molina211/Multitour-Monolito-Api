package com.corhuila.errorcapa8.travesia_natural.reservations.infrastructure.in.web.dto;

import java.time.LocalDate;

public record CompanionRequest(String name, String document, LocalDate birthDate) {
}
