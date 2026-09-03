# 007 — Tareas

**Batching (excepción regla 4 de `CLAUDE.md` — Backend, implementación inicial):**
lotes de 2-3 tareas, commit + push sin pedir permiso en cada uno, siempre que
`PLAN-VERIFICACION.md` esté al día en el repo al momento del push. La ejecución de
build/tests/servidor sigue pidiendo permiso cada vez (regla 5) — el batching cubre
`commit`/`push`, no la ejecución local.

- [x] T01 — `JwtTokenProvider.parse(String token)` + `JwtPrincipal` (record) · repo:
  backend · ~15 min
- [x] T02 — `JwtAuthenticationFilter` + `JwtAuthenticationEntryPoint` (no se registran
  todavía en `SecurityConfig`) · repo: backend · ~20 min · depende de T01
  **— fin lote (T01-T02): commit + push —**
- [ ] T03 — `TenantMismatchException` (dominio, `reservations`) · repo: backend · ~5 min
- [ ] T04 — `SecurityConfig`: registra `JwtAuthenticationFilter`, exige autenticación
  en `POST /api/tenants/{tenantId}/reservations`, agrega el
  `AuthenticationEntryPoint` para `401` · repo: backend · ~15 min · depende de T02
- [ ] T05 — `ReservationController.create(...)` toma `customerId` del `Authentication`
  autenticado (no del body), compara `tenantId` del token vs. URL (`403` si no
  coincide vía `TenantMismatchException`); `CreateReservationRequest` pierde
  `customerId` · repo: backend · ~20 min · depende de T03, T04
  **— fin lote (T03-T05): commit + push —**
- [ ] T06 — Agrega la sección "007 — Enforcement de JWT" a `PLAN-VERIFICACION.md` con
  los `curl` de cada criterio de aceptación · repo: backend · ~15 min · depende de T05
- [ ] T07 — Verifica que `./mvnw test` sigue en verde y ejecuta la sección nueva de
  `PLAN-VERIFICACION.md` de punta a punta contra el servidor local (pide permiso antes
  de correr build/tests/servidor, regla 5) · repo: backend · ~20 min · depende de T06
  **— fin lote (T06-T07): commit + push —**
