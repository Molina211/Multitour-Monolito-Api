# 009 — Plan técnico

## Enfoque

`Reservation` (spec 001) gana comportamiento nuevo, no estructura nueva de bounded
context: tres métodos de mutación (`registerCashPayment`, `registerInstallmentPayment`,
`registerTransferPayment`) más dos de resolución (`approveTransferPayment`,
`rejectTransferPayment`), todos dentro del propio agregado — igual que
`CatalogItem.update()`/`deactivate()` en spec 005, nunca lógica de negocio en el
servicio de aplicación. El estado "transferencia en espera" vive como dos columnas
nullable en la misma fila de `reservations` (decisión ya tomada en la spec, riesgo 1).
Las notas de seguimiento reutilizan `common/audit` (ya existente desde spec 002) en vez
de una tabla nueva: una nota es, conceptualmente, una acción auditable más
(`action = "SEGUIMIENTO_PAGO"`), y el módulo ya expone `AuditRecorder.findAll()` que se
filtra en el servicio de aplicación (mismo criterio de simplicidad que `GET /api/audit`
en spec 002: sin filtros en el puerto, se filtra en memoria). Ningún endpoint nuevo
exige JWT: son operaciones de operador/staff y no existe login de staff (mismo hueco ya
documentado desde spec 004/005/007), así que quedan en `permitAll()` como
`catalog-items`/`discounts`.

## Cambios por repositorio

Solo backend, módulo `reservations` existente más el ya compartido `common/audit`.

- `reservations/domain/model/Reservation.java`: nuevos campos
  `pendingTransferAmount` (`BigDecimal`, nullable), `transferSupportReference`
  (`String`, nullable); nuevos métodos `registerCashPayment(BigDecimal)`,
  `registerInstallmentPayment(BigDecimal)`, `registerTransferPayment(BigDecimal,
  String)`, `approveTransferPayment()`, `rejectTransferPayment()`; `reconstitute(...)`
  gana los dos parámetros nuevos.
- `reservations/domain/exception/PaymentAlreadyResolvedException.java` (nueva): para
  decidir dos veces sobre la misma transferencia, o registrar una transferencia nueva
  mientras otra sigue en validación.
- `reservations/domain/port/in/RegisterPaymentCommand.java` /
  `RegisterPaymentUseCase.java` / `application/RegisterPaymentService.java` (nuevos):
  valida tenant existe/activo, busca la reserva (`ReservationNotFoundException` si no
  existe o es de otro tenant), despacha según `method` (`EFECTIVO`/`ABONO`/
  `TRANSFERENCIA`) al método correspondiente del agregado, guarda.
- `reservations/domain/port/in/DecidePaymentSupportCommand.java` /
  `DecidePaymentSupportUseCase.java` / `application/DecidePaymentSupportService.java`
  (nuevos): igual validación de tenant/reserva, aplica `approveTransferPayment()` o
  `rejectTransferPayment()` según `decision`, guarda, y registra la decisión en
  `AuditRecorder` (`action = "APROBAR_SOPORTE_PAGO"` / `"RECHAZAR_SOPORTE_PAGO"`,
  `reason` obligatorio) — mismo patrón que `deactivate`/`reactivate` de tenant (spec 002).
- `reservations/domain/port/in/ReservationQueryUseCase.java`: nuevo método
  `listPendingSupportByTenant(String tenantId)` (filtra `paymentStatus =
  EN_VALIDACION` sobre `findAllByTenantId`, en el servicio de aplicación, sin puerto de
  persistencia nuevo).
- `reservations/domain/port/in/RegisterPaymentFollowupCommand.java` /
  `RegisterPaymentFollowupUseCase.java` / `application/RegisterPaymentFollowupService.java`
  (nuevos): valida tenant/reserva existen, llama `AuditRecorder.record(...)` con
  `action = "SEGUIMIENTO_PAGO"`, `reason = note`.
- `application/PaymentFollowupQueryService.java` (nuevo, o método adicional en
  `ReservationQueryService`): `listFollowups(tenantId, reservationId)` — filtra
  `AuditRecorder.findAll()` por `tenantId`, `affectedRecordId = reservationId` y
  `action = "SEGUIMIENTO_PAGO"`, ordenado por `recordedAt`.
- `reservations/infrastructure/out/persistence/ReservationEntity.java` +
  `ReservationRepositoryAdapter.java`: columnas y mapeo de los dos campos nuevos.
- `reservations/infrastructure/in/web/PaymentController.java` (nuevo, o métodos
  agregados a `ReservationController.java` bajo el mismo
  `@RequestMapping("/api/tenants/{tenantId}/reservations")`): endpoints de la sección
  "Contratos".
- `reservations/infrastructure/in/web/dto/RegisterPaymentRequest.java`,
  `DecidePaymentSupportRequest.java`, `PaymentFollowupRequest.java`,
  `PaymentFollowupResponse.java` (nuevos).
- `src/main/resources/db/migration/V7__add_reservation_payment_fields.sql` (nueva).

## Decisiones técnicas

1. **Estado de transferencia en la misma fila de `reservations`, no tabla aparte.**
   Alternativa descartada: una tabla `payment_support_requests` con historial de
   intentos. Motivo: el propio Frontend solo modela un intento a la vez sobre la misma
   reserva (spec, riesgo 1); una tabla con historial es estructura que nadie pidió
   todavía.
2. **Notas de seguimiento reutilizan `common/audit`, no una tabla nueva.**
   Alternativa descartada: tabla `payment_followups` dedicada. Motivo: es
   conceptualmente una acción auditable más y el módulo ya existe desde spec 002;
   evita duplicar infraestructura de "texto + actor + fecha" que ya existe.
3. **Sin JWT en estos endpoints.** Alternativa descartada: exigir el mismo JWT que
   protege la creación de reservas (spec 007). Motivo: ese JWT es de End Customer
   (`sub` = su propio `customerId`); estas operaciones las hace el operador/staff, que
   no tiene ningún mecanismo de login todavía (hueco documentado desde spec 004) — exigir
   un JWT que nadie puede obtener bloquearía la funcionalidad por completo.
4. **`RegisterPaymentCommand.method` como `String` validado en el servicio, no un
   enum de dominio nuevo.** Alternativa descartada: enum `PaymentMethodType`. Motivo:
   mismo patrón ya usado en `DiscountRequest.toDiscountBase()` (spec 008) — el mapeo
   `String` → comportamiento vive en el borde (DTO/servicio), no se agrega un tipo de
   dominio para tres literales que ya tiene el propio `Reservation.paymentMethod`
   como texto libre.

## Modelo de datos

`V7__add_reservation_payment_fields.sql`:

```sql
ALTER TABLE reservations
    ADD COLUMN pending_transfer_amount NUMERIC(14, 2),
    ADD COLUMN transfer_support_reference VARCHAR(255);
```

No se toca `audit_records` (spec 002 ya tiene `action`, `reason`, `actor_id`,
`affected_record_id`, `recorded_at` — suficiente para `SEGUIMIENTO_PAGO`,
`APROBAR_SOPORTE_PAGO`, `RECHAZAR_SOPORTE_PAGO`).

## Contratos

Todos bajo `/api/tenants/{tenantId}/reservations/{reservationId}` salvo el listado.

- `POST .../payments` — body `{ "method": "EFECTIVO"|"ABONO"|"TRANSFERENCIA", "amount":
  number, "supportReference": string (solo TRANSFERENCIA), "actorId": string }` →
  `200` con la reserva actualizada. `400` si el monto no aplica (efectivo insuficiente,
  monto ≤ 0, falta `supportReference` en transferencia). `409` si ya hay una
  transferencia en validación y se intenta registrar otra.
- `POST .../payments/decide-support` — body `{ "decision": "APPROVE"|"REJECT",
  "reason": string, "actorId": string }` → `200` con la reserva actualizada. `400` sin
  `reason`. `409` si no hay ninguna transferencia pendiente de decidir.
- `GET /api/tenants/{tenantId}/reservations/pending-support` → lista de reservas con
  `paymentStatus = EN_VALIDACION` de ese tenant.
- `POST .../payments/followups` — body `{ "note": string, "actorId": string }` → `201`.
  `400` sin `note`.
- `GET .../payments/followups` → lista de notas, orden cronológico.
- Todos: `404` tenant o reserva inexistente/de otro tenant; `409` tenant `Inactivo`.

## Cómo se verifica

`PLAN-VERIFICACION.md`, sección "009 — Registro de pago sobre una reserva": un `curl`
por cada criterio de aceptación (efectivo exacto, efectivo insuficiente, abono parcial,
abono que completa, transferencia, aprobar, rechazar, doble decisión, listado de
pendientes, seguimiento, tenant inexistente/inactivo), más `./mvnw test` en verde.
