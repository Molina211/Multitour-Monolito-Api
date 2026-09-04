# 001 — Plan técnico

## Enfoque

Se levanta el paquete `reservations` como el primer módulo hexagonal del monolito, junto
a un `common` mínimo para lo que no pertenece a ningún bounded context (salud del
servicio, seguridad temporal). El dominio (`Reservation` y lo mínimo de `ReservedService`)
se modela en clases planas sin anotaciones de framework; la persistencia real vive en un
adaptador de salida JPA detrás de un puerto, y la entrada HTTP vive en un adaptador de
entrada Spring MVC detrás de un caso de uso. Postgres corre en Docker Compose para
desarrollo local; Flyway aplica el esquema para que exista un historial de migraciones
verificable. Como todavía no hay JWT, el `tenantId` llega por el header `X-Tenant-Id` y
Spring Security se configura para permitir todo temporalmente (documentado como deuda
explícita, no como decisión de seguridad final).

## Cambios por repositorio

**Backend** (`Repositorio Monolito/Backend`), paquete base
`com.corhuila.errorcapa8.travesia_natural` (sin renombrar — fuera de alcance):

```
pom.xml                                   (+ postgresql, flyway)
docker-compose.yml                             (nuevo)
src/main/resources/application.properties      (+ datasource, flyway, jpa)
src/main/resources/db/migration/
  V1__create_reservations.sql                  (nuevo)
src/main/java/.../common/
  web/HealthController.java                    (nuevo)
  security/SecurityConfig.java                 (nuevo)
src/main/java/.../reservations/
  domain/model/Reservation.java                (nuevo)
  domain/model/ReservedService.java            (nuevo)
  domain/model/ReservationStatus.java          (nuevo)
  domain/model/PaymentStatus.java              (nuevo)
  domain/exception/InvalidReservationException.java (nuevo)
  domain/port/in/CreateReservationUseCase.java (nuevo)
  domain/port/out/ReservationRepositoryPort.java (nuevo)
  application/CreateReservationService.java    (nuevo)
  infrastructure/in/web/ReservationController.java (nuevo)
  infrastructure/in/web/dto/CreateReservationRequest.java (nuevo)
  infrastructure/in/web/dto/ReservationResponse.java (nuevo)
  infrastructure/in/web/dto/ErrorResponse.java (nuevo)
  infrastructure/out/persistence/ReservationEntity.java (nuevo)
  infrastructure/out/persistence/ReservedServiceEntity.java (nuevo)
  infrastructure/out/persistence/ReservationJpaRepository.java (nuevo)
  infrastructure/out/persistence/ReservationRepositoryAdapter.java (nuevo)
PLAN-VERIFICACION.md                           (nuevo)
```

No se toca `docs` ni `frontend` en este plan.

## Decisiones técnicas

- **Dominio sin anotaciones JPA (clases planas) vs. entidad JPA como dominio directo:**
  se elige separar dominio de persistencia (hexagonal real). Alternativa descartada:
  anotar `Reservation` directamente con `@Entity` — más rápido de escribir pero acopla
  el dominio a Hibernate desde el día uno, justo lo que la arquitectura hexagonal busca
  evitar; no es válido para materia si el corte se llama "hexagonal".
- **`tenantId` vía header `X-Tenant-Id`, resuelto en el controller, no en un filtro
  global:** ya definido en la spec. Alternativa descartada: un `HandlerInterceptor`/
  filtro que inyecte el tenant en un contexto por request — se pospone porque hoy solo
  hay un endpoint; se reevalúa en la próxima spec de Reservations o en la de JWT.
- **`reservationId` = UUID v4 generado en `CreateReservationService` (capa de
  aplicación):** ya definido en la spec. Se genera antes de construir el agregado de
  dominio, no en el adaptador de persistencia.
- **Flyway, no `ddl-auto`:** ya definido en la spec. Migración única `V1` para este
  corte (una sola tarea de esquema, sin fragmentarla en migraciones por tabla).
- **Spring Security con `permitAll()` temporal:** el starter de seguridad ya está en
  `pom.xml` (heredado del esqueleto); sin configurar bloquearía todo con login
  básico. Se agrega un `SecurityConfig` explícito que permite todo y dice en un
  comentario por qué, en vez de quitar la dependencia (se necesitará para la spec de
  JWT). Alternativa descartada: quitar `spring-boot-starter-security` del `pom.xml`
  y volver a añadirlo después — genera un diff de ida y vuelta sin beneficio.
- **`reserved_services` como tabla mínima, no agregado propio:** una fila por servicio
  reservado, con `tenant_id`, `service_reference`, `party_size`, `scheduled_date`
  (nullable); sin su propio ciclo de vida ni endpoints — cubre solo la regla "al menos
  un servicio reservado" del agregado `Reservation`.

## Modelo de datos

Migración `V1__create_reservations.sql`:

```sql
CREATE TABLE reservations (
    reservation_id   UUID PRIMARY KEY,
    tenant_id        UUID NOT NULL,
    customer_id      VARCHAR(100) NOT NULL,
    projected_value  NUMERIC(12,2) NOT NULL,
    final_value      NUMERIC(12,2) NOT NULL,
    pending_balance  NUMERIC(12,2) NOT NULL,
    credit_balance   NUMERIC(12,2) NOT NULL DEFAULT 0,
    reservation_status VARCHAR(30) NOT NULL,
    payment_status     VARCHAR(30) NOT NULL,
    payment_method     VARCHAR(30),
    created_at       TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE reserved_services (
    id                BIGSERIAL PRIMARY KEY,
    reservation_id    UUID NOT NULL REFERENCES reservations(reservation_id),
    tenant_id         UUID NOT NULL,
    service_reference VARCHAR(100) NOT NULL,
    party_size        INTEGER,
    scheduled_date    DATE
);

CREATE INDEX idx_reservations_tenant ON reservations(tenant_id);
CREATE INDEX idx_reserved_services_tenant ON reserved_services(tenant_id);
CREATE INDEX idx_reserved_services_reservation ON reserved_services(reservation_id);
```

`reservation_status` inicial: `Pendiente de pago`. `payment_status` inicial: `Sin pago`.
`payment_method` nulo hasta que exista un flujo de pago (fuera de este corte).

## Contratos

### `GET /health`
- **200 OK**, cuerpo `{"status":"UP"}`. Sin autenticación.

### `POST /api/reservations`
- **Headers:** `X-Tenant-Id` (obligatorio, string/UUID).
- **Request body:**
  ```json
  {
    "customerId": "string",
    "projectedValue": 250000,
    "reservedServices": [
      { "serviceReference": "tour-laguna-verde", "partySize": 2, "scheduledDate": "2026-10-01" }
    ]
  }
  ```
- **201 Created:**
  ```json
  {
    "reservationId": "uuid",
    "tenantId": "uuid",
    "customerId": "string",
    "projectedValue": 250000,
    "finalValue": 250000,
    "pendingBalance": 250000,
    "creditBalance": 0,
    "reservationStatus": "Pendiente de pago",
    "paymentStatus": "Sin pago",
    "paymentMethod": null,
    "createdAt": "2026-09-02T20:00:00Z"
  }
  ```
- **400 Bad Request:** falta `X-Tenant-Id`, falta `customerId`, o `reservedServices`
  vacío/ausente. Cuerpo:
  ```json
  { "error": "validation_error", "message": "explicación concreta del campo faltante" }
  ```

## Cómo se verifica

| Criterio de aceptación (spec) | Verificación |
|---|---|
| Postgres disponible vía Docker Compose | `docker compose up -d` y `docker compose ps` muestra el contenedor `healthy` |
| `GET /health` responde 200 | `curl -i localhost:8080/health` |
| Crear reserva persiste y devuelve `reservationId` | `curl -X POST localhost:8080/api/reservations` con body válido y header `X-Tenant-Id`; verificar fila en `psql` contra el contenedor |
| Rechazo sin `tenantId`/`customerId`/servicios | Mismo `curl` sin cada campo por separado; esperar 400 y confirmar que no quedó fila en `reservations` |
| `tenant_id NOT NULL` desde la primera migración | Revisar que solo existe `V1__create_reservations.sql` en el historial y que la columna es `NOT NULL` |
| Dos tenants no se cruzan | Crear dos reservas con el mismo `customerId` y distinto `X-Tenant-Id`; consultar ambas filas y confirmar `tenant_id` distinto sin relación entre sí |
| Compila y el test por defecto pasa | `./mvnw test` |

Todo el detalle operativo (comandos exactos, orden de pasos) vive en
`PLAN-VERIFICACION.md`, generado como la tarea T12 de `tasks.md`.
