package com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public record RegisterPaymentCommand(String tenantId, UUID reservationId, String method, BigDecimal amount,
                                      String supportReference) {
}
