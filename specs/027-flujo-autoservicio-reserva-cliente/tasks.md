# 027 — Tareas

**Nota de proceso:** igual que spec 026, esta spec se escribió después de implementar
el código (autorización explícita del responsable humano en sesión, 2026-09-05/06).

- [ ] T01 — `core/customer-api.service.ts` (nuevo): `CustomerApiService.register(...)`
      contra `POST /api/tenants/{tenantId}/customers` · repo: frontend · ~15 min ·
      **implementada y verificada el 2026-09-06, revertida el mismo día** (autoría del
      Frontend es de una compañera de equipo — regla 11 de `CLAUDE.md`). Propuesta sin
      código en `PROPUESTA-INTEGRACION-LOGIN-Y-RESERVA-FRONTEND.md` (raíz del workspace).
- [ ] T02 — `signup.component.ts`/`.html`: formulario real con signals, validación
      mínima local, llamada a `CustomerApiService.register(...)`, distinción del caso
      `409` · repo: frontend · ~30 min · depende de T01 · revertida, ver T01.
- [ ] T03 — `core/reservation-api.service.ts` (nuevo): `ReservationApiService.create(...)`
      contra `POST /api/tenants/{tenantId}/reservations` · repo: frontend · ~15 min ·
      revertida, ver T01.
- [ ] T04 — `client-tour-booking.component.ts`/`.html`: valida sesión `END_CUSTOMER`
      antes de reservar, llama a `ReservationApiService.create(...)`, extrae
      `finishReservation(...)` para preservar el registro espejo en
      `ClientReservationService` con el `reservationId` real · repo: frontend ·
      ~40 min · depende de T03 · revertida, ver T01.
- [x] T05 — Agregar la sección "027 — Flujo de autoservicio de reserva" a
      `PLAN-VERIFICACION.md` con los pasos del flujo completo
      (`signup → login → reservar`) · repo: backend · ~15 min · depende de T02, T04
- [x] T06 — Arrancar el Backend bajo autorización explícita y ejecutar el flujo
      completo contra el servidor local; confirmar la reserva creada vía
      `GET /reservations` (spec 006) y `./mvnw test` en verde · repo: backend ·
      ~20 min · depende de T05

**T06 ejecutada el 2026-09-06** con autorización explícita del responsable humano para
arrancar el Backend (regla 5 de `CLAUDE.md`). Detalle completo en
`PLAN-VERIFICACION.md`, sección "027": registro de un cliente nuevo, login, creación de
reserva real con el `serviceReference` de texto libre del catálogo mock, verificación
en el listado del operador, y rechazo `401` sin token. `./mvnw test` en verde antes y
después.
