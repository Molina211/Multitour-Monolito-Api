package com.corhuila.errorcapa8.travesia_natural.establishments.application;

import com.corhuila.errorcapa8.travesia_natural.establishments.domain.exception.EstablishmentNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.establishments.domain.model.AssociatedEstablishment;
import com.corhuila.errorcapa8.travesia_natural.establishments.domain.model.EstablishmentKind;
import com.corhuila.errorcapa8.travesia_natural.establishments.domain.port.out.EstablishmentRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeactivateEstablishmentServiceTest {

    private static final String TENANT_ID = "travesia-natural";

    @Mock
    private EstablishmentRepositoryPort establishmentRepositoryPort;

    private DeactivateEstablishmentService deactivateEstablishmentService;

    @BeforeEach
    void setUp() {
        deactivateEstablishmentService = new DeactivateEstablishmentService(establishmentRepositoryPort);
    }

    @Test
    void deactivatesAnEstablishment() {
        UUID establishmentId = UUID.randomUUID();
        AssociatedEstablishment establishment = AssociatedEstablishment.create(TENANT_ID, EstablishmentKind.HOTEL,
                "Hotel Andino", null, null);
        when(establishmentRepositoryPort.findByTenantIdAndEstablishmentId(TENANT_ID, establishmentId))
                .thenReturn(Optional.of(establishment));
        when(establishmentRepositoryPort.save(any(AssociatedEstablishment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AssociatedEstablishment result = deactivateEstablishmentService.deactivateEstablishment(TENANT_ID,
                establishmentId);

        assertThat(result.active()).isFalse();
    }

    @Test
    void rejectsWhenEstablishmentDoesNotExist() {
        UUID establishmentId = UUID.randomUUID();
        when(establishmentRepositoryPort.findByTenantIdAndEstablishmentId(TENANT_ID, establishmentId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> deactivateEstablishmentService.deactivateEstablishment(TENANT_ID, establishmentId))
                .isInstanceOf(EstablishmentNotFoundException.class);
    }
}
