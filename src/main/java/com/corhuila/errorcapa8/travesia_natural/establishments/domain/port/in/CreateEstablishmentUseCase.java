package com.corhuila.errorcapa8.travesia_natural.establishments.domain.port.in;

import com.corhuila.errorcapa8.travesia_natural.establishments.domain.model.AssociatedEstablishment;

public interface CreateEstablishmentUseCase {

    AssociatedEstablishment createEstablishment(CreateEstablishmentCommand command);
}
