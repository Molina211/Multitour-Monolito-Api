# 026 — Tareas

**Nota de proceso:** esta spec se escribió después de implementar el código (autorización
explícita del responsable humano en sesión, 2026-09-05/06), no antes — a diferencia del
flujo habitual `/spec-nueva → aprobación → /plan-tareas → implementación`. Las tareas
quedan documentadas igual, en el orden en que tendrían sentido si se rehicieran desde
cero, para mantener el mismo artefacto de trazabilidad que el resto del repo.

- [x] T01 — `Membership.createPlatformAdministrator(tenantId, email, passwordHash)`:
      nuevo factory con las mismas validaciones de nulidad/blank que los demás
      factories de `Membership` · repo: backend · ~15 min
- [x] T02 — Actualizar el Javadoc de `MembershipRole` para reflejar que
      `PLATFORM_ADMINISTRATOR` ya tiene camino de creación (el seed) · repo: backend ·
      ~5 min · depende de T01
- [x] T03 — `PlatformAdministratorSeeder` (`ApplicationRunner`, `tenants/infrastructure`):
      siembra idempotente del tenant reservado `platform` y su membership
      `PLATFORM_ADMINISTRATOR`, usando `TenantRepositoryPort`/`MembershipRepositoryPort`/
      `PasswordEncoder` ya existentes · repo: backend · ~25 min · depende de T01
- [ ] T04 — `core/login-api.service.ts` (Frontend, nuevo): `LoginApiService.login(...)`
      contra `POST /api/tenants/{tenantId}/login` · repo: frontend · ~15 min ·
      **implementada y verificada el 2026-09-06, revertida el mismo día** (autoría del
      Frontend es de una compañera de equipo — regla 11 de `CLAUDE.md`). Propuesta sin
      código en `PROPUESTA-INTEGRACION-LOGIN-Y-RESERVA-FRONTEND.md` (raíz del workspace).
- [ ] T05 — `core/tenant.constants.ts` (Frontend): agrega `PLATFORM_RESERVED_TENANT_ID
      = 'platform'` · repo: frontend · ~5 min · depende de T03 · revertida, ver T04.
- [ ] T06 — `login.component.ts`/`.html` (Frontend): elimina el selector manual
      Administrador/Colaborador; conecta a `LoginApiService`, guarda sesión real vía
      `SessionService`, enruta según el `role` devuelto · repo: frontend · ~30 min ·
      depende de T04 · revertida, ver T04.
- [ ] T07 — `admin-login.component.ts`/`.html` (Frontend): conecta a
      `LoginApiService.login(PLATFORM_RESERVED_TENANT_ID, ...)`, guarda sesión real,
      enruta a `/platform` · repo: frontend · ~20 min · depende de T04, T05 · revertida,
      ver T04.
- [x] T08 — Agregar la sección "026 — Login staff y Platform Administrator" a
      `PLAN-VERIFICACION.md` con los `curl`/pasos de cada criterio de aceptación · repo:
      backend · ~20 min · depende de T03, T06, T07
- [x] T09 — Arrancar el Backend bajo autorización explícita, confirmar el seed y
      ejecutar la sección nueva de `PLAN-VERIFICACION.md` de punta a punta contra el
      servidor local; confirmar `./mvnw test` en verde (specs 001-025 sin romperse) ·
      repo: backend · ~25 min · depende de T08

**T09 ejecutada el 2026-09-06** con autorización explícita del responsable humano para
arrancar el Backend (regla 5 de `CLAUDE.md`). Detalle completo en
`PLAN-VERIFICACION.md`, sección "026": arranque, verificación del seed, reinicio para
confirmar idempotencia, login de Platform Administrator, Administrador y Colaborador
reales, y `401` genérico ante credenciales incorrectas. `./mvnw test` en verde antes y
después.
