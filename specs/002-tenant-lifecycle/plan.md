# 002 — Plan técnico

## Enfoque

Segundo módulo hexagonal del monolito, `tenants`, con la misma estructura que
`reservations` (spec 001): dominio puro, puertos, aplicación, adaptador web y
adaptador de persistencia JPA/Postgres, migrado con Flyway. Se agrega además un
componente compartido `common/audit` (no propio de `tenants`, porque cualquier
módulo futuro necesitará dejar evidencia auditable) con un puerto de salida
(`AuditRecorder`) que `tenants` consume para las tres acciones de ciclo de vida.
El primer Administrador se crea en la misma transacción que el tenant, con la
contraseña hasheada por un `PasswordEncoder` (`BCryptPasswordEncoder`) ya disponible
por `spring-boot-starter-security`, sin tocar `SecurityConfig` (sigue en
`permitAll()`, deuda documentada desde spec 001).

## Cambios por repositorio

**Backend** (`Repositorio Monolito/Backend`):

```
src/main/resources/db/migration/
  V2__create_tenants.sql                         (nuevo: tenants, memberships, audit_records)
src/main/java/.../tenants/
  domain/model/Tenant.java                       (nuevo)
  domain/model/TenantStatus.java                 (nuevo)
  domain/model/Membership.java                   (nuevo)
  domain/model/MembershipRole.java               (nuevo)
  domain/model/MembershipStatus.java             (nuevo)
  domain/exception/InvalidTenantException.java   (nuevo)
  domain/exception/TenantAlreadyExistsException.java (nuevo)
  domain/exception/TenantNotFoundException.java  (nuevo)
  domain/port/in/CreateTenantUseCase.java        (nuevo, + CreateTenantCommand)
  domain/port/in/DeactivateTenantUseCase.java    (nuevo, + DeactivateTenantCommand)
  domain/port/in/ReactivateTenantUseCase.java    (nuevo, + ReactivateTenantCommand)
  domain/port/in/TenantQueryUseCase.java         (nuevo: getById, listAll)
  domain/port/out/TenantRepositoryPort.java      (nuevo)
  domain/port/out/MembershipRepositoryPort.java  (nuevo)
  application/CreateTenantService.java           (nuevo)
  application/DeactivateTenantService.java       (nuevo)
  application/ReactivateTenantService.java       (nuevo)
  application/TenantQueryService.java            (nuevo)
  infrastructure/in/web/TenantController.java    (nuevo)
  infrastructure/in/web/dto/*.java               (nuevo: request/response)
  infrastructure/out/persistence/TenantEntity.java (nuevo)
  infrastructure/out/persistence/MembershipEntity.java (nuevo)
  infrastructure/out/persistence/TenantJpaRepository.java (nuevo)
  infrastructure/out/persistence/MembershipJpaRepository.java (nuevo)
  infrastructure/out/persistence/TenantRepositoryAdapter.java (nuevo)
  infrastructure/out/persistence/MembershipRepositoryAdapter.java (nuevo)
src/main/java/.../common/
  security/SecurityConfig.java                   (+ bean PasswordEncoder)
  audit/AuditRecord.java                         (nuevo)
  audit/AuditRecorder.java                       (nuevo, puerto de salida)
  audit/AuditController.java                     (nuevo: GET /api/audit)
  audit/infrastructure/AuditRecordEntity.java    (nuevo)
  audit/infrastructure/AuditRecordJpaRepository.java (nuevo)
  audit/infrastructure/AuditRecorderAdapter.java (nuevo)
  web/dto/ErrorResponse.java                     (movido desde reservations/infrastructure/in/web/dto)
src/main/java/.../reservations/infrastructure/in/web/
  ReservationController.java                     (actualiza import de ErrorResponse)
PLAN-VERIFICACION.md                             (+ sección spec 002)
```

## Decisiones técnicas

- **`tenantId` como string corto (slug), no UUID:** decisión abierta #2 de la spec,
  resuelta aquí. Regex `^[a-z0-9-]{3,50}$`, validado único antes de persistir.
  Alternativa descartada: UUID autogenerado (consistente con `Reservation.tenantId`)
  — se descarta porque HU-TEN-001 describe explícitamente que el Platform
  Administrator "asigna" el identificador, y porque `reservations` ya está
  deliberadamente desacoplado de `tenants` en este corte (decisión abierta #1 de la
  spec), así que el tipo no necesita coincidir todavía entre los dos módulos.
- **Credenciales embebidas en `Membership`, sin entidad `Identity`/`User` global:**
  `passwordHash` vive directamente en `memberships`. Alternativa descartada:
  identidad global compartida entre tenants (mencionada como pregunta abierta en
  `06-data/models.md`) — se descarta para este corte por ser más compleja y no
  tener todavía una HU que la necesite (un usuario con cuentas en dos tenants);
  se revisita si la spec de Identity and Access (JWT) lo requiere. Candidata a
  ADR corto en Docs si se confirma como decisión de arquitectura permanente.
- **Auditoría como componente compartido (`common/audit`), no parte de `tenants`:**
  `Audit and traceability` es su propia porción de dominio en `06-data/models.md`;
  ponerla en `common` evita que el próximo módulo que necesite auditar (Discounts,
  Reservations al implementar modificación/cancelación) tenga que depender de
  `tenants`.
- **`ErrorResponse` se mueve a `common/web/dto`:** hasta spec 001 solo existía un
  controlador; con `TenantController` y `AuditController` sumándose, mantenerlo
  dentro de `reservations` obligaría a duplicarlo o a que otros módulos dependan de
  `reservations`. Es un movimiento de archivo, no un cambio de contrato.
- **Rol de la membresía inicial:** `MembershipRole` incluye el catálogo completo
  confirmado en `02-domain/entities-and-rules.md` (Platform Administrator,
  Administrator, Operational Collaborator, End Customer, Manager, Accountant,
  Analyst) aunque este corte solo asigna `ADMINISTRATOR` — evita tener que romper
  el enum cuando una spec futura agregue los demás roles.

## Modelo de datos

Migración `V2__create_tenants.sql`:

```sql
CREATE TABLE tenants (
    tenant_id       VARCHAR(50) PRIMARY KEY,
    commercial_name VARCHAR(150) NOT NULL,
    tenant_status   VARCHAR(20) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE memberships (
    membership_id      UUID PRIMARY KEY,
    tenant_id          VARCHAR(50) NOT NULL REFERENCES tenants(tenant_id),
    email              VARCHAR(150) NOT NULL,
    password_hash      VARCHAR(255) NOT NULL,
    role               VARCHAR(30) NOT NULL,
    membership_status  VARCHAR(20) NOT NULL,
    created_at         TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE audit_records (
    audit_record_id       UUID PRIMARY KEY,
    tenant_id             VARCHAR(50),
    actor_id              VARCHAR(150) NOT NULL,
    action                VARCHAR(50) NOT NULL,
    affected_record_id    VARCHAR(100) NOT NULL,
    reason                VARCHAR(500),
    recorded_at           TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_memberships_tenant ON memberships(tenant_id);
CREATE INDEX idx_audit_records_tenant ON audit_records(tenant_id);
```

`tenant_id` no lleva `NOT NULL` en `audit_records` porque acciones a nivel
plataforma (fuera de alcance todavía) podrían no tener tenant asociado; en este
corte siempre viene con valor porque las tres acciones auditadas son de ciclo de
vida de un tenant concreto.

## Contratos

- `POST /api/tenants`
  Request: `{ "tenantId": "travesia-natural", "commercialName": "...", "actorId": "...", "administrator": { "name": "...", "email": "...", "password": "...", "passwordConfirmation": "..." } }`
  `201` → `TenantResponse` (tenantId, commercialName, tenantStatus, createdAt).
  `400` → datos faltantes o `password` ≠ `passwordConfirmation`.
  `409` → `tenantId` ya existe.
- `POST /api/tenants/{tenantId}/deactivate` — body `{ "reason": "...", "actorId": "..." }`.
  `200` → `TenantResponse` actualizado. `400` sin `reason`. `404` tenant inexistente.
- `POST /api/tenants/{tenantId}/reactivate` — mismo contrato que deactivate.
- `GET /api/tenants` → `200` lista de `TenantResponse`.
- `GET /api/tenants/{tenantId}` → `200` `TenantResponse` o `404`.
- `GET /api/audit` → `200` lista de registros de auditoría (todos, sin filtro, ver
  "fuera de alcance" de la spec).

`actorId` viaja en el body de las peticiones de ciclo de vida porque todavía no hay
sesión autenticada de la cual tomarlo (deuda compartida con `X-Tenant-Id` en
spec 001, ambas se resuelven cuando exista JWT).

## Cómo se verifica

- Cada criterio de aceptación de `spec.md` se verifica con un paso curl + una
  consulta `psql` en la sección "002 — Tenant lifecycle" que se agrega a
  `PLAN-VERIFICACION.md` (T14).
- `./mvnw test` sigue en verde (T15) antes de dar por cerrada la spec.
