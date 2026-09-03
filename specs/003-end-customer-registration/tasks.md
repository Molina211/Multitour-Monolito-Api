# 003 — Tareas

**Batching (excepción regla 4 de `CLAUDE.md` — Backend, implementación inicial):**
lotes de 2-3 tareas, commit + push sin pedir permiso en cada uno, siempre que
`PLAN-VERIFICACION.md` esté al día en el repo al momento del push.

- [x] T01 — Migración `db/migration/V3__add_end_customer_fields.sql`: columnas `first_name`, `last_name`, `phone` (nullable) en `memberships` + índice único `uq_memberships_tenant_email` · repo: backend · ~15 min
- [x] T02 — Extiende `Membership`: nuevos campos `firstName`/`lastName`/`phone`, factory `createEndCustomer(tenantId, firstName, lastName, email, phone, passwordHash)`; `createAdministrator` pasa `null` en los tres campos nuevos · repo: backend · ~20 min · depende de T01
- [x] T03 — Nuevas excepciones `EmailAlreadyRegisteredException` y `TenantInactiveException`, y clase `PasswordPolicy` (sin estado, valida longitud mínima 8, mayúscula, minúscula, número, carácter especial) · repo: backend · ~20 min
  **— fin lote 1 (T01-T03): commit + push —**
- [ ] T04 — Puerto `RegisterCustomerUseCase` + `RegisterCustomerCommand`; agrega `existsByTenantIdAndEmail` a `MembershipRepositoryPort` · repo: backend · ~15 min · depende de T02
- [ ] T05 — Adaptador de persistencia: agrega `first_name`/`last_name`/`phone` a `MembershipEntity` y al `save()` de `MembershipRepositoryAdapter`; agrega `existsByTenantIdAndEmail` a `MembershipJpaRepository` y al adaptador · repo: backend · ~20 min · depende de T01, T04
  **— fin lote 2 (T04-T05): commit + push —**
- [ ] T06 — `RegisterCustomerService`: busca el tenant (404 si no existe), rechaza tenant `Inactivo` (409 vía `TenantInactiveException`), valida `PasswordPolicy`, rechaza email duplicado en el tenant (409 vía `EmailAlreadyRegisteredException`), hashea con el `PasswordEncoder` existente, arma `Membership.createEndCustomer` y persiste · repo: backend · ~25 min · depende de T03, T04, T05
- [ ] T07 — `CustomerController` (`POST /api/tenants/{tenantId}/customers`), DTOs `RegisterCustomerRequest`/`CustomerResponse` (sin `passwordHash`), valida `password == passwordConfirmation` antes de construir el comando, mapea excepciones a 400/404/409 · repo: backend · ~25 min · depende de T06
  **— fin lote 3 (T06-T07): commit + push —**
- [ ] T08 — Agrega la sección "003 — End customer registration" a `PLAN-VERIFICACION.md` con los `curl`/`psql` de cada criterio de aceptación · repo: backend · ~20 min · depende de T07
- [ ] T09 — Verifica que el contexto de Spring levanta y que `./mvnw test` sigue en verde (spec 001 + 002 + 003) · repo: backend · ~15 min · depende de T07
- [ ] T10 — Verifica los criterios de aceptación de `spec.md` ejecutando la sección nueva de `PLAN-VERIFICACION.md` de punta a punta · repo: backend · ~25 min · depende de T08, T09
  **— fin lote 4 (T08-T10): commit + push —**
