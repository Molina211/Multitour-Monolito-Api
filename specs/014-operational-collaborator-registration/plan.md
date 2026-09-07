# 014 — Plan técnico

## Enfoque

No es un módulo nuevo: se extiende `tenants`, mismo bounded context que ya modela
`Membership` (Administrator, End Customer). Se agrega un tercer factory
`Membership.createOperationalCollaborator(...)` y un caso de uso de registro que replica
exactamente `RegisterCustomerService` (tenant activo → `PasswordPolicy` → email único →
`save`), más una consulta de lista/detalle filtrada por `tenantId` + `role`. El campo
"nombre completo" que pide el Frontend se guarda en `firstName` (igual que hoy
`lastName` queda `null` para el Administrator) — no hay migración de esquema porque
`memberships` ya es una tabla genérica sin restricciones `NOT NULL` en esos campos.

El toggle "colaborador puede validar soportes de transferencia" (PDR línea 115) queda
fuera de este plan por decisión de la spec (fuera de alcance #3).

## Cambios por repositorio

**Backend** (`com.corhuila.errorcapa8.travesia_natural.tenants`, rama `hu-back-001-dev`):

- `domain/model/Membership.java`: nuevo factory estático `createOperationalCollaborator
  (String tenantId, String name, String email, String passwordHash)` — mismas
  validaciones de blank que `createEndCustomer`, sin `phone` ni `lastName`.
- `domain/exception/CollaboratorNotFoundException.java`: nueva, 404, mismo patrón que
  `TenantNotFoundException`.
- `domain/port/in/`: `RegisterCollaboratorCommand` (`tenantId, name, email, password,
  actorId`), `RegisterCollaboratorUseCase`, `CollaboratorQueryUseCase` (`listByTenant
  (String tenantId)`, `getById(String tenantId, UUID membershipId)`).
- `domain/port/out/MembershipRepositoryPort.java`: se agregan `findAllByTenantIdAndRole
  (String tenantId, MembershipRole role)` y `findByTenantIdAndMembershipId(String
  tenantId, UUID membershipId)` — no rompe el contrato existente, solo añade métodos.
- `infrastructure/out/persistence/`: `MembershipJpaRepository` gana
  `findByTenantIdAndRole(String, String)` y `findByTenantIdAndMembershipId(String,
  UUID)`; `MembershipRepositoryAdapter` implementa los dos métodos nuevos del puerto.
- `application/`: `RegisterCollaboratorService` (implementa `RegisterCollaboratorUseCase`,
  audita con `AuditRecorder` — acción `COLLABORATOR_REGISTERED`, mismo patrón que
  `CreateTenantService`) y `CollaboratorQueryService` (implementa
  `CollaboratorQueryUseCase`).
- `infrastructure/in/web/`: `CollaboratorController`
  (`/api/tenants/{tenantId}/collaborators`) + DTOs `RegisterCollaboratorRequest(name,
  email, password, passwordConfirmation, actorId)` y `CollaboratorResponse
  (membershipId, tenantId, name, email, role, membershipStatus, createdAt)` +
  `@ExceptionHandler` locales (mismo patrón que `CustomerController`, sin
  `@ControllerAdvice` global).

No hay cambios en `LoginService`/`AuthController` (ya es agnóstico al rol) ni migración
Flyway (tabla `memberships` reutilizada tal cual).

## Decisiones técnicas

- **`name` en `firstName`, `lastName = null`** — alternativa descartada: agregar columna
  `displayName`. Motivo: cero migración, mismo patrón ya usado por Administrator (ambos
  campos `null`); partir el nombre en dos strings frágiles no aporta nada que el PDR pida.
- **Auditar el registro con `AuditRecorder`** — alternativa descartada: no auditar (como
  `RegisterCustomerService`). Motivo: el registro de colaborador es una acción
  administrativa de un Administrator sobre su propio tenant, no un autoregistro público;
  mismo criterio que `CreateTenantService` (creación de Administrator sí se audita).
- **`CollaboratorController` separado de `TenantController`** — alternativa descartada:
  meter los endpoints en `TenantController`. Motivo: mismo patrón ya usado para
  `CustomerController`, un controller por tipo de membership bajo `/api/tenants
  /{tenantId}/...`.
- **Sin enforcement de rol en los nuevos endpoints** — coherente con el resto del
  proyecto (`permitAll()`); no se inventa una excepción a la deuda técnica conocida.

## Modelo de datos

Sin cambios. Se reutiliza la tabla `memberships` existente (spec 002); los nuevos
registros solo usan un valor de `role` (`OPERATIONAL_COLLABORATOR`) ya presente en el
enum desde spec 002 pero nunca persistido hasta ahora.

## Contratos

`POST /api/tenants/{tenantId}/collaborators`
- Request: `{ "name": string, "email": string, "password": string, "passwordConfirmation":
  string, "actorId": string }`
- 201 → `CollaboratorResponse`
- 400 `validation_error` — password/confirmación no coinciden, o `PasswordPolicy` falla,
  o campos vacíos
- 404 `tenant_not_found`
- 409 `tenant_inactive`
- 409 `email_already_registered`

`GET /api/tenants/{tenantId}/collaborators`
- 200 → `List<CollaboratorResponse>` (solo `role=OPERATIONAL_COLLABORATOR` de ese tenant)

`GET /api/tenants/{tenantId}/collaborators/{membershipId}`
- 200 → `CollaboratorResponse`
- 404 `collaborator_not_found` — no existe, es de otro tenant, o no es
  `OPERATIONAL_COLLABORATOR`

## Cómo se verifica

- `./mvnw test` en verde (compilación + lo que ya existe).
- Curl end-to-end documentado en `PLAN-VERIFICACION.md` (mismo formato que specs 010-013):
  1. Crear tenant + Administrator (endpoint existente).
  2. `POST .../collaborators` con datos válidos → 201, `role=OPERATIONAL_COLLABORATOR`.
  3. Repetir mismo email → 409 `email_already_registered`.
  4. `POST` con password débil → 400.
  5. `GET .../collaborators` → aparece el colaborador creado.
  6. `GET .../collaborators/{id}` → detalle sin `passwordHash` en el JSON.
  7. `POST /api/tenants/{tenantId}/login` con el colaborador → 200, JWT con
     `role=OPERATIONAL_COLLABORATOR`.
  8. Crear un segundo tenant, confirmar que su `GET .../collaborators` no incluye al
     colaborador del primero (aislamiento).
