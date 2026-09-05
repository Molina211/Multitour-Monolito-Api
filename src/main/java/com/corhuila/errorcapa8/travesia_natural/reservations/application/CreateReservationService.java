package com.corhuila.errorcapa8.travesia_natural.reservations.application;

import com.corhuila.errorcapa8.travesia_natural.catalog.domain.model.CatalogItem;
import com.corhuila.errorcapa8.travesia_natural.catalog.domain.model.CatalogItemType;
import com.corhuila.errorcapa8.travesia_natural.catalog.domain.port.out.CatalogItemRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.InvalidReservationException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.Reservation;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.ReservedService;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in.CreateReservationCommand;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in.CreateReservationUseCase;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.out.ReservationRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantInactiveException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Tenant;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.TenantStatus;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.TenantRepositoryPort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class CreateReservationService implements CreateReservationUseCase {

    private final TenantRepositoryPort tenantRepositoryPort;
    private final ReservationRepositoryPort reservationRepositoryPort;
    private final CatalogItemRepositoryPort catalogItemRepositoryPort;

    public CreateReservationService(TenantRepositoryPort tenantRepositoryPort,
                                     ReservationRepositoryPort reservationRepositoryPort,
                                     CatalogItemRepositoryPort catalogItemRepositoryPort) {
        this.tenantRepositoryPort = tenantRepositoryPort;
        this.reservationRepositoryPort = reservationRepositoryPort;
        this.catalogItemRepositoryPort = catalogItemRepositoryPort;
    }

    @Override
    public Reservation createReservation(CreateReservationCommand command) {
        Tenant tenant = tenantRepositoryPort.findById(command.tenantId())
                .orElseThrow(() -> new TenantNotFoundException(command.tenantId()));

        if (tenant.tenantStatus() == TenantStatus.INACTIVO) {
            throw new TenantInactiveException(tenant.tenantId());
        }

        List<ReservedService> reservedServicesWithTransport = command.reservedServices().stream()
                .map(rs -> resolveTransport(tenant.tenantId(), rs))
                .toList();

        Reservation reservation = Reservation.create(
                UUID.randomUUID(),
                tenant.tenantId(),
                command.customerId(),
                command.projectedValue(),
                reservedServicesWithTransport,
                command.holderDocument(),
                command.companions());

        return reservationRepositoryPort.save(reservation);
    }

    private ReservedService resolveTransport(String tenantId, ReservedService reservedService) {
        if (reservedService.transportItemId() == null) {
            return reservedService;
        }

        CatalogItem catalogItem = catalogItemRepositoryPort
                .findByTenantIdAndCatalogItemId(tenantId, reservedService.transportItemId())
                .orElseThrow(() -> new InvalidReservationException(
                        "transportItemId does not exist for this tenant: " + reservedService.transportItemId()));

        if (catalogItem.type() != CatalogItemType.TRANSPORT) {
            throw new InvalidReservationException(
                    "transportItemId must reference a TRANSPORT catalog item: " + reservedService.transportItemId());
        }
        if (!catalogItem.active()) {
            throw new InvalidReservationException(
                    "transportItemId references an inactive catalog item: " + reservedService.transportItemId());
        }

        BigDecimal transportCost = catalogItem.price().multiply(BigDecimal.valueOf(reservedService.partySize()));
        return new ReservedService(reservedService.serviceReference(), reservedService.partySize(),
                reservedService.scheduledDate(), reservedService.transportItemId(), transportCost);
    }
}
