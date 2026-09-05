# 009 — Registro de pago sobre una reserva

**Estado:** TERMINADA
**Fecha:** 2026-09-03
**Repos afectados:** backend (rama `hu-back-001-dev`)
**HU relacionada:** ninguna HU formal en el backlog cubre esto punto por punto; se
construye sobre `Reservation.paymentStatus`/`pendingBalance`, ya declarados desde la
spec 001 pero sin ningún caso de uso que los mueva. El detalle real viene de las
pantallas del Frontend (rama `develop`, commit `93dd4dc`, aún no en `main`):
`operator/payment-management`, `operator/payments`, `operator/validate-support`,
`operator/payment-followup`.

## Problema

`Reservation` se crea siempre en `PENDIENTE_DE_PAGO`/`SIN_PAGO` (spec 001) y no existe
ningún caso de uso que la mueva de ahí: ni registrar un pago, ni confirmar la reserva.
El Frontend ya construyó por completo esa pantalla (`OperatorReservationService`,
simulada en `localStorage`) con reglas concretas de negocio (efectivo debe cubrir el
saldo, transferencia queda en validación, abono acumula pagos parciales), pero no hay
backend real detrás. Sin esto, ninguna reserva puede llegar nunca a `Confirmada`.

## Alcance

- Nuevo caso de uso "registrar pago" sobre una reserva existente, con tres modalidades
  (mismas que ya construyó el Frontend, `payment-management.component.ts`):
  - **Efectivo**: el monto debe cubrir el `pendingBalance` completo en una sola
    operación. Si lo cubre: `pendingBalance = 0`, `paymentStatus = PAGADO`,
    `reservationStatus = CONFIRMADA`. Si no lo cubre: rechazado (`400`), se sugiere usar
    Abono para pagos parciales — mismo mensaje que ya usa el Frontend.
  - **Abono**: acumula sobre `pendingBalance` (puede ser parcial). Si el saldo llega a
    `0`: `paymentStatus = PAGADO`, `reservationStatus = CONFIRMADA`. Si queda saldo:
    `paymentStatus = PARCIAL`, la reserva permanece `PENDIENTE_DE_PAGO`.
  - **Transferencia**: exige una referencia de soporte (texto/nombre de archivo, mismo
    criterio que `CatalogItem.image` en spec 005 — no se sube ningún archivo real). No
    modifica `pendingBalance` todavía: dispara `paymentStatus = EN_VALIDACION` y deja el
    monto y la referencia en espera de una decisión explícita (siguiente punto).
  `Reservation.paymentMethod` se fija con la modalidad usada en el primer pago
  registrado (hoy queda `null` desde la creación, spec 001).
- Nuevo caso de uso "decidir soporte de transferencia" (`validate-support.component.ts`):
  **aprobar** aplica el monto en espera igual que un Abono (recalcula `pendingBalance`,
  `paymentStatus`, `reservationStatus`); **rechazar** deja `paymentStatus = RECHAZADO`
  sin tocar `pendingBalance`. Ambas exigen un motivo obligatorio (mismo criterio que
  desactivar/reactivar tenant, spec 002) y son irreversibles sobre ese mismo intento
  (no se puede volver a decidir sobre una transferencia ya resuelta).
- `GET` de reservas con `paymentStatus = EN_VALIDACION` por tenant, para la pantalla
  `operator/payments` (cuenta y lista soportes pendientes).
- Registrar una nota de seguimiento de pago (`payment-followup.component.ts`) sobre una
  reserva con saldo pendiente: solo deja constancia (texto + quién + cuándo), no cambia
  ningún estado. Consultable después en orden cronológico.
- Todas las operaciones rechazan tenant inexistente (`404`) o `Inactivo` (`409`), mismo
  criterio que el resto del proyecto.

## Fuera de alcance

- Cálculo de vuelto o saldo a favor cuando un pago en efectivo excede el saldo
  pendiente: el Frontend no define esa regla (no hay fórmula ni plazo parametrizado,
  confirmado en el propio código: "Sin parametrización de plazo definida para esta
  modalidad"). Un monto en efectivo que exceda el saldo se rechaza igual que uno
  insuficiente, no se inventa una regla de vuelto.
- `creditBalance` (ya existe como campo desde spec 001): sigue sin ningún caso de uso
  que lo modifique; pertenece a la futura spec de devoluciones (012), que si genera
  saldo a favor sí lo tocará.
- Restricción por rol de quién puede aprobar/rechazar un soporte (el Frontend ya
  contempla que un "Colaborador operativo" podría hacerlo si el tenant lo habilita,
  pero hoy esa habilitación está deshabilitada por defecto y no hay login de staff
  real, mismo hueco documentado desde spec 004/005): se acepta cualquier `actor` que
  venga en la petición, sin validarlo contra una sesión.
- Ejecución del servicio, cancelación/modificación de la reserva y devoluciones:
  specs futuras (010, 011, 012), en ese orden.
- Caja (registrar el ingreso como movimiento de caja): spec futura (013), que leerá
  estos pagos, no los registra ella misma.
- Cambiar `POST /api/tenants/{tenantId}/reservations` (creación) para aceptar un pago
  inicial en el mismo request: la creación sigue dejando la reserva en
  `PENDIENTE_DE_PAGO`/`SIN_PAGO`, el pago siempre es una operación posterior separada.

## Criterios de aceptación

- [x] Registrar un pago **Efectivo** que cubre exactamente el `pendingBalance` de una
      reserva `PENDIENTE_DE_PAGO` devuelve `200`, deja `paymentStatus = PAGADO`,
      `reservationStatus = CONFIRMADA`, `pendingBalance = 0`.
- [x] Registrar un pago **Efectivo** que no cubre el `pendingBalance` devuelve `400` sin
      modificar la reserva.
- [x] Registrar un pago **Abono** parcial devuelve `200`, reduce `pendingBalance` en el
      monto pagado, deja `paymentStatus = PARCIAL` y `reservationStatus` sin cambios
      (`PENDIENTE_DE_PAGO`).
- [x] Un **Abono** cuyo monto acumulado llega a cubrir el `pendingBalance` deja
      `paymentStatus = PAGADO` y `reservationStatus = CONFIRMADA`, igual que Efectivo.
- [x] Registrar un pago **Transferencia** con referencia de soporte devuelve `200`,
      deja `paymentStatus = EN_VALIDACION`, y **no** modifica `pendingBalance` todavía.
- [x] **Aprobar** una transferencia en validación aplica su monto como un Abono
      (recalcula `pendingBalance`/`paymentStatus`/`reservationStatus`); sin `reason`
      devuelve `400`.
- [x] **Rechazar** una transferencia en validación deja `paymentStatus = RECHAZADO` sin
      tocar `pendingBalance`; sin `reason` devuelve `400`.
- [x] Decidir dos veces sobre la misma transferencia ya resuelta (aprobar/rechazar de
      nuevo) devuelve `409`.
- [x] `GET` de reservas en `EN_VALIDACION` de un tenant devuelve solo las de ese tenant.
- [x] Registrar una nota de seguimiento sobre una reserva con saldo pendiente queda
      consultable después en orden cronológico; no cambia `paymentStatus` ni
      `reservationStatus`.
- [x] Cualquier operación sobre un `tenantId` inexistente devuelve `404`; sobre uno
      `Inactivo`, `409`.
- [x] El proyecto compila y los tests existentes (specs 001-008) siguen pasando.

## Impacto en multitenencia

Todas las operaciones nuevas filtran siempre por `tenantId` de la URL además del
`reservationId`, mismo criterio de aislamiento que specs 006/007 ya aplicaron a
`reservations`.

## Riesgos y decisiones abiertas

1. **Dónde vive el estado "transferencia en espera de decisión"**: el Frontend lo
   modela como dos campos sueltos sobre la misma reserva (`pendingTransferAmount`,
   `supportPending`), no como una entidad con su propio ciclo de vida ni historial de
   intentos previos. Se replica igual (campos nullable en el agregado `Reservation`,
   no una tabla nueva) para no inventar más estructura de la que el propio Frontend
   necesita — se revisita si alguna vez hace falta guardar más de un intento a la vez.
2. **Registro de la nota de seguimiento**: podría reutilizar `common/audit` (ya
   existente desde spec 002) en vez de una tabla nueva, tratándola como una acción de
   auditoría más. Se decide en `/plan-tareas`, no cambia ningún criterio de aceptación.

## Evidencia para la materia

Primer caso de uso que mueve `Reservation` fuera del estado inicial fijado en spec 001;
demostrable con `curl` (efectivo/abono/transferencia, aprobar/rechazar soporte),
mismo patrón que las specs anteriores.
