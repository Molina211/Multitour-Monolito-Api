# 011 — Cancelación de reserva antes de ejecución

**Estado:** TERMINADA
**Fecha:** 2026-09-03
**Repos afectados:** backend (rama `hu-back-001-dev`)
**HU relacionada:** ninguna HU formal en el backlog cubre esto punto por punto; se basa en
RF-003A/RN-RES-004/RN-RES-008/RN-RES-009 del PRD (`03-product/prd.md`). El Frontend no
tiene ninguna pantalla ni acción de cancelar reserva construida todavía (solo una
etiqueta estática `"Cancelada"` en datos de ejemplo de `reservations.component.ts`, sin
botón ni servicio detrás); esta spec no sigue a ninguna pantalla existente, sino
directamente al PRD, con el alcance recortado al mínimo verificable.

## Problema

`Reservation` hoy solo avanza (`PENDIENTE_DE_PAGO` → `CONFIRMADA` → `EN_EJECUCION` →
...); no existe ningún caso de uso que la lleve a `CANCELADA` de forma explícita antes de
que inicie ejecución, ni que calcule qué pasa con un pago ya registrado cuando eso
ocurre.

## Alcance

- Nuevo caso de uso "cancelar reserva": permitido solo cuando `reservationStatus` es
  `PENDIENTE_DE_PAGO` o `CONFIRMADA`. Recibe un motivo (obligatorio) y un actor. Al
  cancelarse: la reserva pasa a `CANCELADA`.
- Si la reserva ya tenía dinero recibido (`finalValue - pendingBalance > 0`, es decir
  hubo pago en efectivo, abono(s) o transferencia aprobada), la cancelación calcula ese
  monto ya pagado y lo deja registrado como `creditBalance` (saldo a favor), con
  `paymentStatus = SALDO_A_FAVOR_PENDIENTE`. Si no había ningún pago recibido
  (`paymentStatus = SIN_PAGO`), la cancelación no genera saldo a favor.
- Solo **calcular y registrar** el saldo a favor resultante, sin ejecutar ninguna
  devolución de dinero ni movimiento de caja: eso es la spec futura de devoluciones
  (RN-RES-008 separa determinación de valor, autorización y ejecución en tres
  decisiones distintas; aquí solo se cubre la primera).
- Rechazar la cancelación si la reserva tiene una transferencia con soporte todavía sin
  aprobar/rechazar (`pendingTransferAmount != null`): esa decisión debe resolverse
  primero (aprobar o rechazar la transferencia, spec 009) antes de poder cancelar.
- Todas las operaciones rechazan tenant inexistente (`404`) o `Inactivo` (`409`), mismo
  criterio que el resto del proyecto.

## Fuera de alcance

- Modificar una reserva (cambiar servicios, cantidades, acompañantes, hospedaje,
  transporte con recálculo de valores/descuentos/capacidad — RF-003A completo): no hay
  ninguna pantalla ni servicio de Frontend que lo necesite todavía; se retoma en una
  spec propia si en algún momento el Frontend construye esa pantalla.
- Ejecutar la devolución de dinero (marcar el saldo a favor como efectivamente devuelto,
  movimiento de caja): spec futura de devoluciones (012).
- Autorización explícita del Administrador para la devolución (RN-RES-008): no aplica
  aquí porque esta spec no ejecuta devoluciones, solo calcula el saldo potencial.
- Cancelación automática de reservas `Pendiente de pago` por vencimiento de plazo
  (RN-RES-006): requiere un mecanismo de temporizador/job que no existe en el proyecto;
  spec aparte si se decide construirlo.
- Ventanas de tiempo parametrizables por actividad para permitir o bloquear la
  cancelación (RN-RES-004, ej. "hasta 6 horas antes"): no existe módulo de
  parametrización de plazos en el backend ni el Frontend lo simula; no se inventa esa
  validación.
- Cancelación extraordinaria por emergencia durante la ejecución (RF-008): solo aplica
  antes de `EN_EJECUCION`; el caso "ya en ejecución" queda fuera, tal como ya se dejó
  fuera de alcance en spec 010.
- Liberación de cupo asociado a la reserva cancelada (RN-RES-006/007): no existe todavía
  ningún mecanismo de apartamiento de cupo en el backend contra el cual liberar nada.

## Criterios de aceptación

- [x] Cancelar una reserva `Pendiente de pago` sin pagos registrados devuelve `200`,
      deja `reservationStatus = CANCELADA`, `creditBalance = 0` y `paymentStatus` sin
      cambios (`Sin pago`).
- [x] Cancelar una reserva `Confirmada` que ya tiene el valor completo pagado (efectivo
      o abono que saldó el pendiente) devuelve `200`, deja `reservationStatus =
      CANCELADA`, `creditBalance` igual al valor pagado y `paymentStatus = Saldo a
      favor pendiente`.
- [x] Cancelar una reserva con un abono parcial ya registrado devuelve `200` y deja
      `creditBalance` igual solo al monto efectivamente abonado (no al valor total).
- [x] Cancelar sin indicar motivo devuelve `400`.
- [x] Cancelar una reserva que ya está `En ejecucion`, `Finalizada` o `Cancelada`
      devuelve un código de error (409, mismo criterio de spec 010) y no la modifica.
- [x] Cancelar una reserva con una transferencia en espera de aprobación/rechazo
      (`pendingTransferAmount` presente) devuelve un código de error (409) y no la
      modifica.
- [x] Consultar una reserva cancelada devuelve `reservationStatus = Cancelada` y el
      `creditBalance`/`paymentStatus` resultantes, junto con el motivo y actor de la
      cancelación.
- [x] Cualquier operación sobre un `tenantId` inexistente devuelve `404`; sobre uno
      `Inactivo`, `409`.
- [x] El proyecto compila y los tests existentes (specs 001-010) siguen pasando.

## Impacto en multitenencia

Mismo patrón que las specs anteriores: la cancelación filtra siempre por `tenantId` de
la URL además de `reservationId`; no hay ningún dato ni cálculo que cruce tenants.

## Riesgos y decisiones abiertas

1. **Dónde vive el motivo/actor de la cancelación**: ¿se agrega como campos directos de
   `Reservation` (similar a como `Execution` guarda su propio `causal`/`actorId` en un
   módulo aparte) o se extiende el propio agregado `Reservation` con esos dos campos
   nuevos? Se decide en `/plan-tareas`, no cambia ningún criterio de aceptación.
2. **Código de error exacto para "cancelación sobre estado inválido" o "transferencia
   pendiente"**: `409` en ambos casos, siguiendo el mismo criterio ya usado en spec 010
   para "la reserva no cumple la precondición de estado". Se confirma en `/plan-tareas`.

## Evidencia para la materia

Primer caso de uso que mueve `Reservation` a `CANCELADA` y primer uso real del campo
`creditBalance` (existente desde spec 001 pero sin ningún caso de uso que lo llenara);
demostrable con `curl` (cancelar sin pago, cancelar con pago total, cancelar con abono
parcial, rechazo por transferencia pendiente), mismo patrón que las specs anteriores.
