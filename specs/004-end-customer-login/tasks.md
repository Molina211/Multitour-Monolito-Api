# 004 — Tareas

**Batching (excepción regla 4 de `CLAUDE.md` — Backend, implementación inicial):**
lotes de 2-3 tareas, commit + push sin pedir permiso en cada uno, siempre que
`PLAN-VERIFICACION.md` esté al día en el repo al momento del push.

- [x] T01 — Agrega `io.jsonwebtoken:jjwt-api`/`jjwt-impl`/`jjwt-jackson` (0.12.6) a `pom.xml`; agrega `app.jwt.secret` y `app.jwt.expiration-minutes` a `application.properties` con comentario aclarando que el secreto por defecto es solo para desarrollo · repo: backend · ~15 min
- [x] T02 — `Membership.reconstitute(...)`: nuevo factory sin validar invariantes, para reconstruir desde persistencia (mismo patrón de `Tenant.reconstitute`) · repo: backend · ~15 min
- [x] T03 — `InvalidCredentialsException` (dominio, mensaje genérico fijo); agrega `findByTenantIdAndEmail` a `MembershipRepositoryPort` · repo: backend · ~15 min
  **— fin lote 1 (T01-T03): commit + push —**
- [x] T04 — Implementa `findByTenantIdAndEmail` en `MembershipJpaRepository` (query derivada) y en `MembershipRepositoryAdapter` (mapea `MembershipEntity` → `Membership` vía `reconstitute`) · repo: backend · ~20 min · depende de T02, T03
- [x] T05 — `common/security/JwtTokenProvider`: componente Spring que genera un JWT HS256 con claims `sub`/`tenantId`/`email`/`role` y expiración, leyendo `app.jwt.secret`/`app.jwt.expiration-minutes` · repo: backend · ~20 min · depende de T01
- [x] T06 — Puertos `LoginCommand`, `LoginUseCase`, `LoginResult` (`tenants/domain/port/in`) · repo: backend · ~10 min
  **— fin lote 2 (T04-T06): commit + push —**
- [ ] T07 — `LoginService`: resuelve tenant (inexistente o `Inactivo` → `InvalidCredentialsException`), busca membership por tenant+email (ausente o `INACTIVA` → `InvalidCredentialsException`), valida password con `PasswordEncoder.matches` (no coincide → `InvalidCredentialsException`), genera el JWT con `JwtTokenProvider` · repo: backend · ~25 min · depende de T03, T04, T05, T06
- [ ] T08 — `AuthController` (`POST /api/tenants/{tenantId}/login`), DTOs `LoginRequest`/`LoginResponse`, `@ExceptionHandler(InvalidCredentialsException.class)` → `401` con el mensaje genérico. **Incluye el comentario extenso** (por qué el tenant va en la URL, referencia a `ADR-003` y HU-IAM-001 escenario 3, y el aviso explícito de que `login.component.ts`/`.html` y `signup.component.html` del Frontend no tienen campo de tenant y deben agregarlo antes de poder integrar este endpoint) · repo: backend · ~30 min · depende de T07
  **— fin lote 3 (T07-T08): commit + push —**
- [ ] T09 — Agrega la sección "004 — End customer login" a `PLAN-VERIFICACION.md` con los `curl` de cada criterio de aceptación · repo: backend · ~20 min · depende de T08
- [ ] T10 — Verifica que `./mvnw test` sigue en verde (spec 001+002+003+004) y ejecuta la sección nueva de `PLAN-VERIFICACION.md` de punta a punta contra el servidor local · repo: backend · ~25 min · depende de T09
  **— fin lote 4 (T09-T10): commit + push —**
