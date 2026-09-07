# 003 — Plan técnico

## Enfoque

Se reutiliza el módulo `tenants` y el aggregate `Membership` de spec 002: mismo
paquete, misma tabla `memberships`, un segundo factory (`createEndCustomer`) junto al
ya existente `createAdministrator`. No se crea un módulo `identity` separado (decisión
abierta #3 de la spec, resuelta así por ahora). El caso de uso nuevo
(`RegisterCustomerUseCase`) sigue la misma forma que `CreateTenantUseCase`: valida,
hashea con el `PasswordEncoder` de spec 002, arma el aggregate, persiste. La política
de contraseña replica exactamente el texto ya mostrado en el Frontend
(`signup.component.html`: mínimo 8 caracteres, mayúscula, minúscula, número, carácter
especial) — hoy esa política es solo un texto informativo sin validación real ni en
HTML ni en TS, así que el Backend pasa a ser la única validación efectiva.

## Cambios por repositorio

**Backend** (`Repositorio Monolito/Backend`):

```
src/main/resources/db/migration/
  V3__add_end_customer_fields.sql              (nuevo)
src/main/java/.../tenants/
  domain/model/Membership.java                 (+ campos firstName/lastName/phone, + factory createEndCustomer)
  domain/model/PasswordPolicy.java             (nuevo: valida la política, sin estado)
  domain/exception/EmailAlreadyRegisteredException.java (nuevo)
  domain/exception/TenantInactiveException.java (nuevo)
  domain/port/in/RegisterCustomerUseCase.java  (nuevo, + RegisterCustomerCommand)
  domain/port/out/MembershipRepositoryPort.java (+ existsByTenantIdAndEmail)
  application/RegisterCustomerService.java     (nuevo)
  infrastructure/in/web/CustomerController.java (nuevo: POST /api/tenants/{tenantId}/customers)
  infrastructure/in/web/dto/RegisterCustomerRequest.java (nuevo)
  infrastructure/in/web/dto/CustomerResponse.java (nuevo)
  infrastructure/out/persistence/MembershipEntity.java (+ columnas first_name/last_name/phone)
  infrastructure/out/persistence/MembershipJpaRepository.java (+ existsByTenantIdAndEmail)
  infrastructure/out/persistence/MembershipRepositoryAdapter.java (+ existsByTenantIdAndEmail, + campos nuevos en save)
PLAN-VERIFICACION.md                           (+ sección spec 003)
```

No se toca `CreateTenantService` ni `TenantController` (el registro de Administrator
sigue igual); tampoco se toca `reservations` (el vínculo con `Reservation.customerId`
queda fuera de alcance, ver spec).

## Decisiones técnicas

- **Tenant en el path (`/api/tenants/{tenantId}/customers`), no en un header:**
  alternativa descartada: reusar `X-Tenant-Id` como en `reservations` (spec 001) —
  se descarta porque `TenantController` ya anida sus propias acciones bajo
  `/api/tenants/{tenantId}/...` (deactivate, reactivate), y aquí el tenant ya se puede
  y se debe validar contra la tabla `tenants` (a diferencia de spec 001, que no tenía
  esa tabla todavía). Mantiene un solo patrón de URL para todo lo que cuelga de un
  tenant concreto.
- **Bloquear registro sobre un tenant `Inactivo`:** alternativa descartada: permitirlo
  igual, dejando que solo el login (futuro) lo bloquee — se descarta porque
  `INV-TEN-002` ya establece que `Inactivo` no borra evidencia, pero no dice que deba
  seguir aceptando altas nuevas; permitir registros en un tenant desactivado
  contradice la intención de "desactivar". No cierra la brecha ya documentada en
  spec 002 (riesgo #1: `reservations` tampoco valida `tenantStatus`) — sigue abierta
  para ese módulo, esta spec solo la cierra para el registro de clientes.
- **Password policy en el Backend, replicando el texto del Frontend:** el Frontend
  solo *muestra* la política como texto (`signup.component.html`), no la aplica ni en
  HTML ni en TypeScript (`signup.component.ts` no tiene validators). Sin esta
  validación en el Backend, la política completa sería letra muerta. Se implementa
  como una clase sin estado (`PasswordPolicy`) en vez de una anotación Bean Validation
  para no introducir esa dependencia solo para una regla.
- **Sin cambios a `CreateTenantService`:** aunque ahora existe `PasswordPolicy`, no se
  aplica retroactivamente a la creación de Administrator (fuera de alcance de esta
  spec; si se quiere, es una tarea de una línea en una spec futura o un ajuste menor
  aprobado aparte).
- **`Membership.createEndCustomer` como segundo factory, no una subclase:** consistente
  con cómo ya está modelado `Membership` (clase final con factories nombrados por
  rol). `firstName`/`lastName`/`phone` quedan `null` para memberships creadas por
  `createAdministrator`, y `email`/`passwordHash` siguen siendo obligatorios para
  ambos.

## Modelo de datos

Migración `V3__add_end_customer_fields.sql`:

```sql
ALTER TABLE memberships
    ADD COLUMN first_name VARCHAR(100),
    ADD COLUMN last_name  VARCHAR(100),
    ADD COLUMN phone      VARCHAR(30);

CREATE UNIQUE INDEX uq_memberships_tenant_email ON memberships(tenant_id, email);
```

Las tres columnas nuevas quedan nullable porque las memberships `Administrator` ya
persistidas (spec 002) no las tienen y no se van a rellenar retroactivamente. El
índice único reemplaza la verificación aplicativa como última barrera (defensa en
profundidad): `RegisterCustomerService` ya consulta `existsByTenantIdAndEmail` antes
de guardar para devolver un `409` controlado, pero el índice evita una condición de
carrera entre dos registros simultáneos con el mismo email.

## Contratos

- `POST /api/tenants/{tenantId}/customers`
  Request: `{ "firstName": "...", "lastName": "...", "email": "...", "phone": "...", "password": "...", "passwordConfirmation": "..." }`
  (`phone` opcional, puede omitirse o venir `null`).
  `201` → `CustomerResponse` (membershipId, tenantId, firstName, lastName, email,
  phone, role: "END_CUSTOMER", membershipStatus: "ACTIVA", createdAt). Nunca incluye
  `passwordHash`.
  `400` → `firstName`/`lastName`/`email`/`password` faltantes, `password` ≠
  `passwordConfirmation`, o `password` no cumple la política.
  `404` → `tenantId` no existe.
  `409` → tenant `Inactivo`, o email ya registrado en ese mismo tenant.

## Cómo se verifica

- Cada criterio de aceptación de `spec.md` se verifica con un paso curl + una
  consulta `psql` en la sección "003 — End customer registration" que se agrega a
  `PLAN-VERIFICACION.md`.
- `./mvnw test` sigue en verde antes de dar por cerrada la spec (sin regresión sobre
  spec 001 y 002).
