# 012 — Ejecución de devolución sobre saldo a favor

**Estado:** TERMINADA
**Fecha:** 2026-09-03
**Repos afectados:** backend (rama `hu-back-001-dev`)
**HU relacionada:** ninguna HU formal en el backlog cubre esto punto por punto; se basa en
RF-015B/RN-RES-008/RN-RES-009 del PRD (`03-product/prd.md`). El Frontend no tiene ninguna
acción real de devolución construida: el panel "Devoluciones monetarias" de
`payments.component.html` es texto estático con un botón "Consultar solicitudes" sin
`click` ni servicio detrás (`payments.component.ts` no tiene lógica de devoluciones), y el
ítem de navegación "Caja" en `operator-shell.component.html` no tiene `routerLink` (no
existe módulo de Caja real en ningún lado del Frontend). Esta spec no sigue ninguna
pantalla existente, sino directamente al PRD, con el alcance recortado al mínimo
verificable.

## Problema

Spec 011 dejó reservas en `paymentStatus = SALDO_A_FAVOR_PENDIENTE` con un `creditBalance`
calculado, pero no existe ningún caso de uso que resuelva ese saldo: hoy queda pendiente
indefinidamente, sin forma de registrar que el dinero efectivamente salió (o parte de él).

## Alcance

- Nuevo caso de uso "ejecutar devolución": recibe `reservationId`, actor, motivo/
  observación, monto a devolver y método de salida (texto libre, ej. "efectivo",
  "transferencia"). Actor y motivo son obligatorios, igual que en cancelación (spec 011).
- Precondición: `paymentStatus == SALDO_A_FAVOR_PENDIENTE` y `creditBalance > 0`. Si no se
  cumple, `409`.
- El monto a devolver no puede superar el `creditBalance` disponible; si lo supera, `400`.
- Al ejecutar (total o parcial): `creditBalance` se reduce por el monto devuelto y
  `paymentStatus` pasa a `DEVUELTO_PARCIAL_O_TOTAL` — el PRD (sección de transiciones de
  pago) usa ese mismo estado tanto para devolución total como parcial, no distingue dos
  estados distintos. Queda registrado el monto devuelto, motivo, actor, método y fecha
  como campos directos de `Reservation` (mismo patrón que motivo/actor/fecha de
  cancelación en spec 011).
- Un único paso: "ejecutar" registra en la misma operación lo que el PRD llama
  autorización y ejecución (RN-RES-008 las separa conceptualmente, pero aquí se modela
  como un solo caso de uso con actor+motivo, igual que cancelación no separó "solicitar"
  de "cancelar"). Si el Frontend construye un flujo real de solicitud→aprobación
  independiente, se revisita.
- Todas las operaciones rechazan tenant inexistente (`404`) o `Inactivo` (`409`), mismo
  criterio que el resto del proyecto.

## Fuera de alcance

- Movimiento real de caja (RN-CAJ-001: base diaria parametrizable, ingresos, gastos,
  cierre formal, histórico, consolidación BASE+INGRESOS-PAGOS-GASTOS-DEVOLUCIONES=TOTAL):
  no existe ningún módulo de Caja en el backend ni en el Frontend (el nav "Caja" no tiene
  `routerLink`). Se difiere a una spec futura de Caja (013), mismo criterio que spec 010
  ya usó para dejar "Caja como tal" fuera. Esta spec solo deja el monto devuelto y su
  método registrados en la propia reserva, sin generar ningún movimiento de caja real.
- Validación real de rol Administrador vs Colaborador operativo (RF-015B distingue quién
  autoriza y quién solo ejecuta con autorización previa): no existe módulo de roles ni
  JWT para operador en el backend. El actor se modela como `actorId` de texto libre sin
  verificación, igual que en cancelación (spec 011).
- Aplicación del saldo a favor a una reserva futura vinculada del mismo cliente
  (RN-RES-009): no hay pantalla ni flujo de Frontend que lo pida; se deja fuera.
- Reagendamiento como alternativa a devolución (RF-008/RF-015C): fuera de alcance, spec
  aparte si se construye una pantalla que lo necesite.
- Devolución sobre modificación de reserva (recálculo de servicios): no aplica, spec 011
  ya dejó fuera la modificación completa de reserva (RF-003A); no hay ninguna devolución
  que se origine ahí porque el caso de uso que la generaría no existe.
- Auditoría separada tipo `AuditRecorder` para la devolución: mismo criterio que
  cancelación (spec 011), motivo/actor quedan en el propio agregado, no en un módulo
  aparte.

## Criterios de aceptación

- [x] Ejecutar devolución total (monto == `creditBalance`) sobre una reserva con
      `SALDO_A_FAVOR_PENDIENTE` devuelve `200`, deja `creditBalance = 0`,
      `paymentStatus = Devuelto parcial o total`, y quedan registrados monto devuelto,
      motivo, actor, método y fecha.
- [x] Ejecutar devolución parcial (monto < `creditBalance`) devuelve `200`, deja
      `creditBalance` reducido por el monto devuelto (no en cero) y
      `paymentStatus = Devuelto parcial o total`.
- [x] Ejecutar devolución con monto mayor al `creditBalance` disponible devuelve `400`.
- [x] Ejecutar devolución sin motivo o sin actor devuelve `400`.
- [x] Ejecutar devolución sobre una reserva sin `SALDO_A_FAVOR_PENDIENTE` (por ejemplo
      `Sin pago`, `Confirmada` sin cancelar, o ya `Devuelto parcial o total`) devuelve
      `409` y no la modifica.
- [x] Consultar una reserva con devolución ejecutada devuelve `paymentStatus`,
      `creditBalance` resultantes, junto con monto devuelto, motivo, actor, método y
      fecha de la devolución.
- [x] Cualquier operación sobre un `tenantId` inexistente devuelve `404`; sobre uno
      `Inactivo`, `409`.
- [x] El proyecto compila y los tests existentes (specs 001-011) siguen pasando.

## Impacto en multitenencia

Mismo patrón que las specs anteriores: la ejecución de devolución filtra siempre por
`tenantId` de la URL además de `reservationId`; no hay ningún dato ni cálculo que cruce
tenants.

## Riesgos y decisiones abiertas

1. **Dónde vive el monto devuelto acumulado**: ¿un campo nuevo `refundedAmount` en
   `Reservation`, o basta con inferirlo de `finalValue - pendingBalance - creditBalance`?
   Se decide en `/plan-tareas`, no cambia ningún criterio de aceptación.
2. **Nombre exacto de la excepción para "reserva sin saldo a favor pendiente"**: ¿se
   reutiliza `ReservationNotCancellableException` con otro mensaje, o se crea
   `ReservationNotRefundableException`? Mismo patrón de una sola excepción por causa que
   ya se usó en specs 010/011; se confirma en `/plan-tareas`.

## Evidencia para la materia

Primer caso de uso que mueve `Reservation` a `paymentStatus = Devuelto parcial o total`
y cierra el ciclo de vida económico abierto por spec 011 (saldo a favor); demostrable con
`curl` (devolución total, devolución parcial, rechazo por falta de saldo, rechazo por
monto excesivo), mismo patrón que las specs anteriores.
