package com.corhuila.errorcapa8.travesia_natural.establishments.domain.port.in;

import com.corhuila.errorcapa8.travesia_natural.establishments.domain.model.EstablishmentKind;

public record CreateEstablishmentCommand(String tenantId, EstablishmentKind kind, String name, String description,
                                          String image) {
}
