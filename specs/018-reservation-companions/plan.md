# 018 — Plan técnico

## Enfoque

`Reservation` gana un value object nuevo `Companion` (nombre, documento, fecha de
nacimiento) y un campo `holderDocument`, siguiendo exactamente el mismo patrón ya usado
para `ReservedService`: value object inmutable, tabla propia con FK a `reservations`,
entidad JPA hermana de `ReservedServiceEntity`, y mapeo en el adaptador de persistencia.
La validación de no-duplicidad de documento (RN-RES-005) se agrega como una invariante
más dentro de `Reservation.create()`, en el mismo lugar donde ya se valida que
`reservedServices` no esté vacío, y reutiliza la excepción `InvalidReservationException`
ya existente (mapeada a `400` en `ReservationController`) en vez de crear una excepción
nueva.

## Cambios por repositorio

**Backend** (único repo afectado):

- `reservations/domain/model/Companion.java` (nuevo, record).
- `reservations/domain/model/Reservation.java`: constructor privado, `create()`,
  `reconstitute()` ganan `holderDocument`/`companions`; nueva validación de duplicados en
  `create()`; getters `holderDocument()`/`companions()`.
- `reservations/infrastructure/out/persistence/CompanionEntity.java` (nuevo, mismo patrón
  que `ReservedServiceEntity`).
- `reservations/infrastructure/out/persistence/ReservationEntity.java`: columna
  `holder_document`, colección `companions` (`@OneToMany`), método `addCompanion()`.
- `reservations/infrastructure/out/persistence/ReservationRepositoryAdapter.java`:
  `toNewEntity()` guarda `holderDocument` y agrega cada `CompanionEntity`; `toDomain()`
  los reconstruye. `applyChanges()` no se toca (companions/holderDocument son fijos desde
  la creación, igual que `reservedServices`).
- `src/main/resources/db/migration/V15__add_reservation_companions.sql` (nuevo).
- `reservations/domain/port/in/CreateReservationCommand.java`: gana
  `holderDocument`/`companions`.
- `reservations/application/CreateReservationService.java`: pasa los dos campos nuevos a
  `Reservation.create()`.
- `reservations/infrastructure/in/web/dto/CompanionRequest.java` y
  `CompanionResponse.java` (nuevos).
- `reservations/infrastructure/in/web/dto/CreateReservationRequest.java` y
  `ReservationResponse.java`: ganan `holderDocument`/`companions`.
- `reservations/infrastructure/in/web/ReservationController.java`: `create()` mapea
  `CompanionRequest` → `Companion` y los pasa al comando.

## Decisiones técnicas

1. **Normalización del documento al comparar duplicados**: se aplica la misma
   normalización que ya usa el Frontend (`trim`, minúsculas, quitar caracteres no
   alfanuméricos) solo para la comparación de igualdad dentro de `create()`; el valor
   guardado en `holderDocument`/`Companion.document` es siempre el que llegó, sin
   modificar. Alternativa descartada: comparar el valor exacto tal cual llega — se
   descarta porque dejaría pasar como "distintos" documentos que el propio Frontend ya
   trata como iguales (ej. `"CC-123"` vs `"cc123"`), lo que produciría duplicados
   silenciosos apenas se integren ambos módulos.
2. **Dónde vive la validación de duplicados**: dentro de `Reservation.create()` (capa de
   dominio), igual que el resto de invariantes de creación (`tenantId` no vacío,
   `reservedServices` no vacío). Alternativa descartada: validarlo en
   `CreateReservationService` (capa de aplicación) — se descarta porque partiría la
   validación de invariantes de creación entre dos capas sin ninguna razón nueva que lo
   justifique, rompiendo el patrón ya establecido.
3. **Excepción a lanzar**: se reutiliza `InvalidReservationException` (ya mapeada a `400`
   en `ReservationController.handleValidationError`) en vez de crear una excepción propia
   para el duplicado. Alternativa descartada: una `DuplicateCompanionDocumentException`
   dedicada — se descarta por ser una capa extra sin comportamiento distinto (mismo
   código `400`, mismo manejador).

## Modelo de datos

`V15__add_reservation_companions.sql`:

```sql
ALTER TABLE reservations ADD COLUMN holder_document VARCHAR(50);

CREATE TABLE reservation_companions (
    id                 BIGSERIAL PRIMARY KEY,
    reservation_id     UUID NOT NULL REFERENCES reservations(reservation_id),
    tenant_id          UUID NOT NULL,
    name               VARCHAR(200) NOT NULL,
    document           VARCHAR(50) NOT NULL,
    birth_date         DATE NOT NULL
);

CREATE INDEX idx_reservation_companions_tenant ON reservation_companions(tenant_id);
CREATE INDEX idx_reservation_companions_reservation ON reservation_companions(reservation_id);
```

## Contratos

`POST /api/tenants/{tenantId}/reservations` — request gana dos campos opcionales:

```json
{
  "projectedValue": 100.00,
  "reservedServices": [ { "serviceReference": "...", "partySize": 2, "scheduledDate": "2026-10-01" } ],
  "holderDocument": "CC-123",
  "companions": [
    { "name": "Ana Pérez", "document": "CC-456", "birthDate": "1990-05-10" }
  ]
}
```

Response (`ReservationResponse`, y por tanto `GET .../reservations`, `GET
.../reservations/{id}`, `GET .../reservations/me*`) gana los mismos dos campos en el
mismo formato.

Error nuevo: si `holderDocument` coincide (normalizado) con algún `companions[].document`,
o dos `companions[].document` coinciden entre sí, `POST .../reservations` devuelve `400`
con `{"error": "validation_error", "message": "..."}`, mismo formato que los demás
errores de validación del endpoint.

## Cómo se verifica

- Crear reserva con `holderDocument` + 2 `companions` sin documentos repetidos → `201`,
  `curl` a `GET .../reservations/{id}` devuelve los mismos datos guardados.
- Crear reserva sin `holderDocument` ni `companions` → `201` igual que antes de esta spec
  (compatibilidad con specs 001-017).
- Crear reserva con un `companion.document` igual a `holderDocument` (con distinta
  capitalización/espacios) → `400`.
- Crear reserva con dos `companions` con el mismo documento → `400`.
- `./mvnw test` en verde tras la migración V15.
