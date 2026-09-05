# 020 — Plan técnico

## Enfoque

Se agrega un campo booleano `allowCollaboratorSupportValidation` directo en `Tenant`
(default `false` al crear, mismo patrón simple que el resto del proyecto), con un caso de
uso para que un actor `ADMINISTRATOR` lo active/desactive. `DecidePaymentSupportService` se
modifica para resolver el `Membership` del `actorId` recibido (ya no es texto libre sin
verificar) y aplicar la regla del PDR línea 115: `ADMINISTRATOR` siempre puede decidir;
`OPERATIONAL_COLLABORATOR` solo si el tenant tiene el flag habilitado.

## Cambios por repositorio

Solo backend (`tenants` + `reservations`).

- `tenants/domain/model/Tenant.java`: campo `allowCollaboratorSupportValidation` (boolean,
  `false` en `create`); método `updateCollaboratorSupportValidationPermission(boolean)`
  (mismo patrón que `deactivate()`/`reactivate()`, exige tenant activo).
- `tenants/domain/port/in/`: `UpdateCollaboratorSupportValidationPermissionCommand`/
  `UseCase`.
- `tenants/application/UpdateCollaboratorSupportValidationPermissionService.java`: resuelve
  `Membership` del `actorId` vía `MembershipRepositoryPort`, exige `role == ADMINISTRATOR`
  (403 si no), aplica el cambio y audita con `AuditRecorder`.
- `tenants/infrastructure/in/web/TenantController.java`: nuevo
  `PATCH /{tenantId}/collaborator-support-permission`.
- `tenants/infrastructure/in/web/dto/UpdateCollaboratorSupportValidationPermissionRequest.java`
  (nuevo).
- `tenants/infrastructure/out/persistence/TenantEntity.java` +
  `TenantRepositoryAdapter.java`: columna nueva.
- `src/main/resources/db/migration/V17__add_tenant_collaborator_support_permission.sql`.
- `reservations/application/DecidePaymentSupportService.java`: agrega dependencia
  `MembershipRepositoryPort`; resuelve el `Membership` del `actorId`, valida rol y, si es
  `OPERATIONAL_COLLABORATOR`, exige `tenant.allowCollaboratorSupportValidation()`.
- `reservations/domain/exception/SupportValidationNotAllowedException.java` (nuevo, vive en
  `reservations` porque es el caso de uso que la lanza).

## Decisiones técnicas

- **Campo directo en `Tenant`** en vez de tabla de configuración aparte — descartado por
  ser el único parámetro de tenant que existe hoy; se revisa si se justifica una tabla
  cuando aparezca un segundo parámetro.
- **`actorId` se resuelve como `membershipId`** contra `MembershipRepositoryPort` — mismo
  criterio que spec 019; deja de ser texto libre sin verificar en este caso de uso
  específico.
- **Excepción `SupportValidationNotAllowedException` distinta de `TenantInactiveException`/
  `TenantNotFoundException`** — para no mezclar `403` (permiso) con `404`/`409` (estado del
  tenant).
- **No se toca ningún otro caso de uso** que hoy recibe `actorId` como texto libre
  (cancelación, devolución) — queda documentado como deuda conocida (ver spec).

## Modelo de datos

`V17__add_tenant_collaborator_support_permission.sql`:

```sql
ALTER TABLE tenants
    ADD COLUMN allow_collaborator_support_validation BOOLEAN NOT NULL DEFAULT false;
```

## Contratos

- `PATCH /api/tenants/{tenantId}/collaborator-support-permission`
  body `{actorId, allow}` → `200` con el tenant actualizado; `403` si el actor no es
  `ADMINISTRATOR`; `404`/`409` si el tenant no existe o está inactivo.
- `POST /api/tenants/{tenantId}/reservations/{reservationId}/payments/decide-support`
  (existente) — mismo contrato de request; ahora puede devolver `403` nuevo
  (`SupportValidationNotAllowedException`) cuando el actor es `OPERATIONAL_COLLABORATOR` y
  el tenant no tiene el permiso habilitado.

## Cómo se verifica

Se agrega una sección "020" a `PLAN-VERIFICACION.md`:
1. Crear un tenant nuevo (permiso `false` por defecto) y un colaborador operativo (spec
   014).
2. Con una reserva en validación de soporte, decidir con el `actorId` del colaborador →
   `403`.
3. Activar el permiso con un `actorId` `ADMINISTRATOR` → `200`.
4. Repetir el paso 2 → `200`, decisión aplicada.
5. Intentar activar el permiso con el `actorId` del colaborador → `403`.
6. Decidir con un `actorId` `ADMINISTRATOR` en cualquier momento (permiso en `false` o
   `true`) → siempre `200`.
