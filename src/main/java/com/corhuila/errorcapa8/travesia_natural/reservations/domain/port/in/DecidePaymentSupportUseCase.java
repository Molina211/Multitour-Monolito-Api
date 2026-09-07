package com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in;

import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.Reservation;

public interface DecidePaymentSupportUseCase {

    Reservation decidePaymentSupport(DecidePaymentSupportCommand command);
}
