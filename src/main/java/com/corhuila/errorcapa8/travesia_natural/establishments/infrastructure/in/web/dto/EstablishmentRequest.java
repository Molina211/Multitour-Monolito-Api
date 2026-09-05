package com.corhuila.errorcapa8.travesia_natural.establishments.infrastructure.in.web.dto;

import com.corhuila.errorcapa8.travesia_natural.establishments.domain.model.EstablishmentKind;

public record EstablishmentRequest(EstablishmentKind kind, String name, String description, String image) {
}
