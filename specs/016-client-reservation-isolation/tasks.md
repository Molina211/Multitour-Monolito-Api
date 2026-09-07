# 016 — Tareas

- [x] T01 — `ReservationRepositoryPort` + `ReservationJpaRepository` +
      `ReservationRepositoryAdapter`: agregar
      `findAllByTenantIdAndCustomerId(tenantId, customerId)`  · repo: backend ·
      ~15 min
- [x] T02 — `ReservationQueryUseCase` + `ReservationQueryService`: agregar
      `listByTenantAndCustomer` y `getByIdForCustomer` (404 unificado si no existe
      o no coincide el `customerId`)  · repo: backend · ~15 min · depende de T01
- [x] T03 — `SecurityConfig`: proteger `GET .../reservations/me` y
      `.../reservations/me/**` con autenticación  · repo: backend · ~10 min
- [x] T04 — `ReservationController`: endpoints `GET .../reservations/me` y
      `GET .../reservations/me/{reservationId}` con validación de
      `tenantId`/`TenantMismatchException`  · repo: backend · ~20 min · depende de
      T02, T03
- [x] T05 — Verificar los criterios de aceptación de la spec: `./mvnw test` +
      secuencia curl completa (listado propio, lista vacía, 401 sin token, 404
      reserva ajena, 403 tenant_mismatch, regresión del listado general para
      Staff) + `PLAN-VERIFICACION.md`  · repo: backend · ~25 min · depende de T04
