# 019 — Plan técnico

## Enfoque

Se agrega la máquina de estados de "decisión de devolución" (`RefundDecisionStatus`) como
campos directos sobre `Reservation`, siguiendo el mismo patrón ya usado para cancelación
(spec 011) y devolución de un solo paso (spec 012) — no se crea una entidad `RefundRequest`
aparte porque no hay pantalla que pida ver más de una solicitud por reserva. Se separan en
el dominio los pasos de autorizar / rechazar / ejecutar / registrar-como-saldo-a-favor como
métodos distintos de `Reservation`, cada uno validando la transición previa. El endpoint de
ejecución de spec 012 (`POST /{reservationId}/refund`) se reutiliza tal cual, pero ahora
exige `refundDecisionStatus == AUTORIZADA` como precondición adicional. Se agregan tres
endpoints nuevos para autorizar, rechazar y registrar saldo a favor.

## Cambios por repositorio

Solo backend (`reservations` + `tenants` para la validación de rol).

- `reservations/domain/model/RefundDecisionStatus.java` (nuevo enum).
- `reservations/domain/model/Reservation.java`: nuevos campos `refundDecisionStatus`,
  `refundAuthorizedBy`, `refundAuthorizedAt`, `refundAuthorizationNote`,
  `refundRejectedBy`, `refundRejectedAt`, `refundRejectionReason`; nuevos métodos
  `authorizeRefund(actorId, note)`, `rejectRefund(actorId, reason)`,
  `registerRefundAsCreditBalance(actorId)`; `refund(...)` (ejecución) exige
  `refundDecisionStatus == AUTORIZADA` y setea `EJECUTADA` al terminar; `cancel(...)`
  inicializa `refundDecisionStatus = PENDIENTE_AUTORIZACION` cuando entra en
  `SALDO_A_FAVOR_PENDIENTE`.
- `reservations/domain/exception/RefundNotAuthorizedException.java` (nuevo).
- `reservations/domain/port/in/`: `AuthorizeRefundCommand`/`UseCase`,
  `RejectRefundCommand`/`UseCase`, `RegisterRefundAsCreditBalanceCommand`/`UseCase`.
- `reservations/application/`: `AuthorizeRefundService`, `RejectRefundService`,
  `RegisterRefundAsCreditBalanceService` — cada uno resuelve el `Membership` del `actorId`
  vía `MembershipRepositoryPort.findByTenantIdAndMembershipId` (ya existe) y exige
  `role == ADMINISTRATOR` (403 si no).
- `reservations/infrastructure/in/web/ReservationController.java`: nuevos
  `POST /{reservationId}/refund/authorize`, `POST /{reservationId}/refund/reject`,
  `POST /{reservationId}/refund/credit-balance`.
- `reservations/infrastructure/in/web/dto/`: `AuthorizeRefundRequest`,
  `RejectRefundRequest`, `RegisterRefundAsCreditBalanceRequest`; `ReservationResponse` se
  amplía con los campos nuevos.
- `reservations/infrastructure/out/persistence/ReservationEntity.java` +
  `ReservationRepositoryAdapter.java`: columnas nuevas.
- `src/main/resources/db/migration/V16__add_refund_decision_fields.sql`.

## Decisiones técnicas

- **Campos embebidos en `Reservation`** en vez de entidad `RefundRequest` aparte —
  descartado por no existir necesidad de historial de múltiples solicitudes; mismo criterio
  que motivo/actor de cancelación.
- **Validación de rol vía `MembershipRepositoryPort`** en vez de esperar JWT real —
  descartado esperar porque JWT para operador sigue bloqueado (deuda conocida desde spec
  002/014); esto ya sigue el patrón de `actorId` que usa todo el proyecto, solo que ahora se
  resuelve contra el repositorio en vez de aceptarse como texto libre.
- **Se reutiliza el endpoint de ejecución existente** (`POST /refund`) en vez de crear uno
  nuevo — mismo contrato de request de spec 012, solo cambia la precondición de dominio.
- **No se genera un `CashMovement` real** al ejecutar — fuera de alcance explícito de la
  spec; se revisa en una spec futura de integración con `cash` si se necesita.

## Modelo de datos

`V16__add_refund_decision_fields.sql`:

```sql
ALTER TABLE reservations
    ADD COLUMN refund_decision_status VARCHAR(30),
    ADD COLUMN refund_authorized_by VARCHAR(255),
    ADD COLUMN refund_authorized_at TIMESTAMP,
    ADD COLUMN refund_authorization_note VARCHAR(500),
    ADD COLUMN refund_rejected_by VARCHAR(255),
    ADD COLUMN refund_rejected_at TIMESTAMP,
    ADD COLUMN refund_rejection_reason VARCHAR(500);
```

## Contratos

- `POST /api/tenants/{tenantId}/reservations/{reservationId}/refund/authorize`
  body `{actorId, note}` → `200` con la reserva en `AUTORIZADA`; `403` si el actor no es
  `ADMINISTRATOR`; `409` si no está en `PENDIENTE_AUTORIZACION`.
- `POST /api/tenants/{tenantId}/reservations/{reservationId}/refund/reject`
  body `{actorId, reason}` → `200`, pasa a `RECHAZADA`; mismas reglas de `403`/`409`.
- `POST /api/tenants/{tenantId}/reservations/{reservationId}/refund` (existente, spec 012)
  → ahora `409` si `refundDecisionStatus != AUTORIZADA`; en éxito pasa a `EJECUTADA`.
- `POST /api/tenants/{tenantId}/reservations/{reservationId}/refund/credit-balance`
  body `{actorId}` → `200`, exige `AUTORIZADA`, pasa a `SALDO_A_FAVOR_REGISTRADO`.
- `GET /api/tenants/{tenantId}/reservations/{reservationId}` → la respuesta incluye
  `refundDecisionStatus` y los campos de auditoría de autorización/rechazo.

## Cómo se verifica

Se agrega una sección "019" a `PLAN-VERIFICACION.md` con `curl` contra la app local:
1. Cancelar una reserva con pago (spec 011) hasta dejarla en `SALDO_A_FAVOR_PENDIENTE` /
   `PENDIENTE_AUTORIZACION`.
2. Intentar ejecutar el refund sin autorizar → `409`.
3. Autorizar con un `actorId` de un `Membership` `OPERATIONAL_COLLABORATOR` → `403`.
4. Autorizar con un `actorId` `ADMINISTRATOR` → `200`, `AUTORIZADA`.
5. Ejecutar el refund → `200`, `EJECUTADA`.
6. Repetir 1-4 y en vez de ejecutar, llamar `credit-balance` → `200`,
   `SALDO_A_FAVOR_REGISTRADO`.
7. Repetir 1 y en vez de autorizar, rechazar → `200`, `RECHAZADA`; confirmar que ejecutar
   después devuelve `409`.
