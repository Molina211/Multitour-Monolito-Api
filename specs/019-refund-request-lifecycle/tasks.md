# 019 — Tareas

- [x] T01 — Crear `RefundDecisionStatus` (enum) y agregar los 7 campos nuevos a
      `Reservation` (constructor/reconstitute/getters, sin lógica de transición todavía) ·
      repo: backend · ~25 min
- [x] T02 — Agregar `authorizeRefund`/`rejectRefund`/`registerRefundAsCreditBalance` a
      `Reservation`, modificar `refund()` para exigir `AUTORIZADA`, y crear
      `RefundNotAuthorizedException` · repo: backend · ~30 min · depende de T01
- [x] T03 — Actualizar `cancel(...)` para inicializar `refundDecisionStatus =
      PENDIENTE_AUTORIZACION` al entrar en `SALDO_A_FAVOR_PENDIENTE` · repo: backend ·
      ~20 min · depende de T02
- [x] T04 — Puertos de entrada `AuthorizeRefundCommand/UseCase`,
      `RejectRefundCommand/UseCase`, `RegisterRefundAsCreditBalanceCommand/UseCase` ·
      repo: backend · ~20 min · depende de T02
- [x] T05 — Application services (`AuthorizeRefundService`, `RejectRefundService`,
      `RegisterRefundAsCreditBalanceService`) con validación de rol `ADMINISTRATOR` vía
      `MembershipRepositoryPort` · repo: backend · ~30 min · depende de T04
- [x] T06 — DTOs de request nuevos + 3 endpoints nuevos en `ReservationController` +
      actualizar el endpoint existente de `refund` para propagar el `409` por falta de
      autorización · repo: backend · ~30 min · depende de T05
- [x] T07 — `ReservationEntity` + `ReservationRepositoryAdapter`: mapear las 7 columnas
      nuevas · repo: backend · ~25 min · depende de T01
- [x] T08 — Migración `V16__add_refund_decision_fields.sql` · repo: backend · ~10 min ·
      depende de T07
- [x] T09 — Ampliar `ReservationResponse` para exponer `refundDecisionStatus` y los campos
      de auditoría de autorización/rechazo · repo: backend · ~15 min · depende de T06
- [x] T10 — Verificar los 8 criterios de aceptación de la spec (curl manual) y agregar la
      sección "019" a `PLAN-VERIFICACION.md` · repo: backend · ~20 min · depende de
      T08, T09
