# 024 — Plan técnico

## Enfoque

`ReservedService` gana dos campos opcionales, `transportItemId` (UUID) y `transportCost`
(BigDecimal). Al crear una reserva, si el cliente envía `transportItemId` en algún
servicio, el backend busca ese `CatalogItem` filtrando por `tenantId` (mismo patrón de
`CreateDiscountService`), valida `type == TRANSPORT` y `active == true`, y calcula
`transportCost = catalogItem.price() * partySize` — el valor nunca viene del cliente.
Cualquier fallo de esa validación (no existe, es de otro tenant, no es TRANSPORT, está
inactivo) se traduce en `InvalidReservationException`, que `ReservationController` ya
mapea a `400 validation_error` — no se crea ninguna excepción nueva.

## Cambios por repositorio

**Backend**, todo dentro de `reservations` salvo la lectura de `catalog`:

- `db/migration/V20__add_transport_fields_to_reserved_services.sql`: columnas nullable
  `transport_item_id` (UUID) y `transport_cost` (NUMERIC(12,2)) en `reserved_services`.
- `reservations/domain/model/ReservedService.java`: agregar `transportItemId`,
  `transportCost` al record (ambos nullable, sin nueva validación en el compact
  constructor — `transportItemId` ya se validó antes de llegar aquí).
- `reservations/infrastructure/out/persistence/ReservedServiceEntity.java`: agregar
  columnas/getters `transportItemId`/`transportCost` al constructor y a la entidad.
- `reservations/infrastructure/out/persistence/ReservationRepositoryAdapter.java`:
  actualizar los 4 puntos de mapeo (`applyChanges` comparación + reconstrucción,
  `toNewEntity`, `toDomain`) para llevar ambos campos en los dos sentidos.
- `reservations/infrastructure/in/web/dto/ReservedServiceRequest.java`: agregar
  `transportItemId` (no `transportCost` — ese lo calcula el servidor).
- `reservations/infrastructure/in/web/dto/ReservedServiceResponse.java`: agregar
  `transportItemId` y `transportCost`, actualizar `from()`.
- `reservations/application/CreateReservationService.java`: inyectar
  `CatalogItemRepositoryPort`; antes de `Reservation.create(...)`, transformar
  `command.reservedServices()` resolviendo `transportItemId` → `transportCost` por cada
  servicio que lo traiga.

## Decisiones técnicas

- **`transportCost` se recalcula en el servidor, nunca se recibe del cliente.**
  Alternativa descartada: aceptar `transportCost` en el request. Motivo: evita que un
  cliente envíe un costo arbitrario o desactualizado; el servidor es la única fuente de
  verdad del precio, igual que ya ocurre con `finalValue`/descuentos en specs 011/022/023.
- **Extender `ReservedService` directamente, sin un value object nuevo.** Alternativa
  descartada: `TransportSelection` como tipo aparte. Motivo: preferir lo simple (regla 10
  de CLAUDE.md); el proyecto ya extiende records existentes para casos similares
  (`holderDocument`/`companions` en `Reservation`, spec 018), y aquí solo son dos campos
  nullable sin lógica propia adicional.
- **Reusar `InvalidReservationException`/`400` en vez de `CatalogItemNotFoundException`/
  `404`.** Alternativa descartada: replicar el patrón de `CreateDiscountService` (spec
  008), que devuelve `404` para `catalogItemId` inválido. Motivo: la spec 024 exige
  explícitamente `400` para todos los casos de `transportItemId` inválido (inexistente,
  otro tenant, tipo incorrecto, inactivo); usar la excepción y el manejador que
  `ReservationController` ya tiene evita crear una excepción nueva solo para esto.
- **Validación ocurre en `CreateReservationService`, no dentro de `Reservation.create()`.**
  Motivo: `Reservation` (dominio puro) no tiene acceso a `CatalogItemRepositoryPort`; el
  servicio de aplicación es el lugar correcto para orquestar la consulta al catálogo,
  igual que ya hace con `TenantRepositoryPort`.

## Modelo de datos

`V20__add_transport_fields_to_reserved_services.sql`:

```sql
ALTER TABLE reserved_services
    ADD COLUMN transport_item_id UUID,
    ADD COLUMN transport_cost    NUMERIC(12,2);
```

Ambas nullable: la mayoría de servicios reservados no tienen transporte asociado.

## Contratos

`POST /api/tenants/{tenantId}/reservations` — `reservedServices[].transportItemId`
(UUID, opcional) se agrega al request. Response (`GET`/list ya existentes) agrega
`reservedServices[].transportItemId` y `reservedServices[].transportCost`.

Nuevo caso de error: `transportItemId` presente pero inválido (no existe, otro tenant,
`type != TRANSPORT`, `active == false`) → `400 validation_error` (reutiliza el handler
existente de `InvalidReservationException`).

## Cómo se verifica

- Crear reserva con un `transportItemId` válido (TRANSPORT, activo, mismo tenant) → `201`,
  `transportCost` calculado y visible en la respuesta y en un `GET` posterior.
- Crear reserva sin `transportItemId` en ningún servicio → `201`, ambos campos `null`.
- Crear reserva con `transportItemId` inexistente, de otro tenant, de otro `type`, o
  inactivo (4 sub-casos) → `400` en cada uno.
- Reconfirmar que los criterios de aislamiento/404/409 de tenant ya existentes siguen
  vigentes (no se tocan, pero se re-ejecutan como parte de la suite).
- `./mvnw test` en verde.
