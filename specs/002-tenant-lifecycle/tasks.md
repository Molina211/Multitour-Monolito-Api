# 002 — Tareas

**Batching (excepción regla 4 de `CLAUDE.md` — Backend, implementación inicial):**
lotes de 2-3 tareas, commit + push sin pedir permiso en cada uno, siempre que
`PLAN-VERIFICACION.md` esté al día en el repo al momento del push.

- [x] T01 — Migración `db/migration/V2__create_tenants.sql` con `tenants`, `memberships`, `audit_records` (`tenant_id` como PK string en `tenants`, `NOT NULL` en `memberships`) · repo: backend · ~25 min
- [x] T02 — Modelo de dominio: `Tenant`, `TenantStatus`, `Membership`, `MembershipRole`, `MembershipStatus`, `InvalidTenantException`, `TenantAlreadyExistsException`, `TenantNotFoundException` · repo: backend · ~30 min
  **— fin lote 1 (T01-T02): commit + push —**
- [x] T03 — Bean `PasswordEncoder` (`BCryptPasswordEncoder`) en `SecurityConfig`, sin tocar el `permitAll()` existente · repo: backend · ~10 min
- [x] T04 — Puertos de `tenants`: `CreateTenantUseCase`/`Command`, `DeactivateTenantUseCase`/`Command`, `ReactivateTenantUseCase`/`Command`, `TenantQueryUseCase`, `TenantRepositoryPort`, `MembershipRepositoryPort` · repo: backend · ~25 min · depende de T02
- [x] T05 — `common/audit`: modelo `AuditRecord` y puerto de salida `AuditRecorder` (sin implementación todavía) · repo: backend · ~15 min
  **— fin lote 2 (T03-T05): commit + push —**
- [x] T06 — Adaptador de persistencia de `tenants`: `TenantEntity`, `MembershipEntity`, `TenantJpaRepository`, `MembershipJpaRepository`, `TenantRepositoryAdapter`, `MembershipRepositoryAdapter` · repo: backend · ~30 min · depende de T01, T04
- [x] T07 — Adaptador de persistencia de auditoría: `AuditRecordEntity`, `AuditRecordJpaRepository`, `AuditRecorderAdapter` (implementa el puerto de T05) · repo: backend · ~20 min · depende de T01, T05
  **— fin lote 3 (T06-T07): commit + push —**
- [x] T08 — `CreateTenantService`: hashea la contraseña con el `PasswordEncoder` de T03, arma `Tenant` + `Membership`, persiste vía los puertos de T06 y registra auditoría vía `AuditRecorder` · repo: backend · ~30 min · depende de T03, T04, T05, T06, T07
- [x] T09 — `DeactivateTenantService` y `ReactivateTenantService`, cada uno exige `reason`, cambia `TenantStatus` y registra su propio evento de auditoría · repo: backend · ~25 min · depende de T04, T06, T07
- [x] T10 — `TenantQueryService` (`getById`, `listAll`) · repo: backend · ~15 min · depende de T04, T06
  **— fin lote 4 (T08-T10): commit + push —**
- [x] T11 — Mueve `ErrorResponse` a `common/web/dto`; ajusta el import en `ReservationController` · repo: backend · ~15 min
- [x] T12 — `TenantController` (`POST /api/tenants`, `POST /api/tenants/{id}/deactivate`, `POST /api/tenants/{id}/reactivate`, `GET /api/tenants`, `GET /api/tenants/{id}`), DTOs de request/response, mapeo de excepciones a 400/404/409 · repo: backend · ~30 min · depende de T08, T09, T10, T11
- [x] T13 — `AuditController` (`GET /api/audit`) · repo: backend · ~15 min · depende de T07, T11
  **— fin lote 5 (T11-T13): commit + push —**
- [x] T14 — Agrega la sección "002 — Tenant lifecycle" a `PLAN-VERIFICACION.md` con los `curl` y `psql` de cada criterio de aceptación · repo: backend · ~25 min · depende de T12, T13
- [x] T15 — Verifica que el contexto de Spring levanta con todos los beans nuevos y que los tests existentes (incluidos los de spec 001) siguen pasando (`./mvnw test`) · repo: backend · ~15 min · depende de T12, T13
- [x] T16 — Verifica los criterios de aceptación de `spec.md` ejecutando la sección nueva de `PLAN-VERIFICACION.md` de punta a punta · repo: backend · ~25 min · depende de T14, T15
  **— fin lote 6 (T14-T16): commit + push —**
