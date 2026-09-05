# 022 — Modificación de reserva antes de ejecución

**Estado:** TERMINADA
**Fecha:** 2026-09-05
**Repos afectados:** backend
**HU relacionada:** HU-RES-002 (nombrada explícitamente como fuera de alcance en spec 001,
"Fuera de alcance" #4: "Modificación (HU-RES-002)... de una reserva ya creada"). También
recoge RF-015A del PDR, ya usado como referencia en el comentario de
`operator-reservation.service.ts:474`.

## Problema

`Reservation` hoy solo tiene un camino hacia adelante en su ciclo de vida (pagos,
cancelación, ejecución, finalización); no existe ningún caso de uso que le cambie el
servicio reservado, la fecha o la cantidad de viajeros a una reserva ya creada. El
Frontend ya construyó la pantalla `cancel-or-modify` con un selector real
"Cancelación"/"Modificación": la opción "Modificación" tiene su propio formulario
(servicio, fecha de salida, viajeros, hospedaje), recalcula proyectado/descuento/valor
final/saldo en vivo, y al confirmar llama a
`OperatorReservationService.registerModification(...)` — hoy solo en `localStorage`,
sin ningún endpoint de Backend detrás.

## Alcance

- Nuevo caso de uso "modificar reserva": permitido solo cuando `reservationStatus` es
  `PENDIENTE_DE_PAGO` o `CONFIRMADA` (misma precondición que `cancel()`, spec 011; en
  `EN_EJECUCION` el propio Frontend ya bloquea la opción "Modificación" y solo deja
  "Cancelación" — `cancel-or-modify.component.ts:131`, RF-008A). Recibe un `reservedServices`
  nuevo (reemplaza al existente), un `projectedValue`/`finalValue` nuevos (aceptados como
  dato de entrada ya calculado, mismo criterio que `Reservation.create()` en spec 001 —
  esta spec no construye un motor de descuentos), un motivo obligatorio y un actor.
- Precondición adicional: no se permite modificar si hay una transferencia pendiente de
  decisión (`pendingTransferAmount != null`), mismo criterio que `cancel()`.
- Recalcula el saldo pendiente sobre el dinero ya recibido, reutilizando la misma fórmula
  de `cancel()`: `amountAlreadyPaid = finalValue - pendingBalance` (con los valores
  ANTERIORES a la modificación).
  - Si `newFinalValue >= amountAlreadyPaid`: `pendingBalance = newFinalValue -
    amountAlreadyPaid`; `creditBalance` y `paymentStatus` no cambian.
  - Si `newFinalValue < amountAlreadyPaid`: el excedente
    (`amountAlreadyPaid - newFinalValue`) se registra como `creditBalance`,
    `paymentStatus` pasa a `SALDO_A_FAVOR_PENDIENTE` y nace una solicitud de devolución en
    `refundDecisionStatus = PENDIENTE_AUTORIZACION` — mismo mecanismo que `cancel()`
    (spec 011) y el mismo ciclo de vida que ya resuelve spec 019 (autorizar/rechazar/
    ejecutar), sin duplicar ese flujo aquí.
- Nuevo endpoint `POST /api/tenants/{tenantId}/reservations/{reservationId}/modify`,
  mismo patrón verbo-acción que `.../cancel` (spec 011).
- Todas las validaciones nuevas (`reservedServices` vacío, `projectedValue` negativo,
  motivo vacío) devuelven `400`, mismo criterio que el resto del proyecto.

## Fuera de alcance

- Motor de descuentos/recalculo de reglas comerciales sobre el nuevo servicio (spec 008
  ya dejó "aplicar descuento al valor final de una reserva" fuera de alcance; sigue
  fuera aquí también, ver spec 023 para el caso de descuento adicional puntual).
- Validación de capacidad de alojamiento (HU-RES-004) — sigue sin existir en el proyecto,
  igual que en spec 001.
- Actualizar `companions`/`holderDocument` como parte de la modificación: el formulario
  de `cancel-or-modify` no captura estos datos (`cancel-or-modify.component.ts` no tiene
  campos de acompañantes), solo spec 018 los cubre en creación.
- Ejecutar la devolución del saldo a favor generado por una modificación a la baja
  (marcar el dinero como efectivamente devuelto): reutiliza el mismo flujo ya construido
  en spec 012/019, no se repite aquí.
- Ventanas de tiempo parametrizables para permitir o bloquear la modificación (mismo
  criterio ya dejado fuera en spec 011 para cancelación: no existe módulo de
  parametrización de plazos).
- Historial de versiones de la reserva (qué servicio/fecha tenía antes de modificarse):
  `Reservation` solo guarda el motivo (`cancellationReason`-equivalente) y el estado
  actual, mismo patrón ya usado por `cancel()` (que tampoco guarda un historial de
  estados previos más allá del motivo).
- Conectar el Frontend a este endpoint: `OperatorReservationService` sigue en
  `localStorage`; integración diferida hasta que ambos módulos estén completos.

## Criterios de aceptación

- [x] Modificar una reserva `Pendiente de pago` sin pagos registrados cambia
      `reservedServices`/`projectedValue`/`finalValue`, deja `pendingBalance = finalValue`
      y devuelve `200`.
- [x] Modificar una reserva `Confirmada` (saldo en 0) a un `finalValue` mayor deja
      `pendingBalance` igual a la diferencia entre el nuevo `finalValue` y lo ya pagado,
      devuelve `200`.
- [x] Modificar una reserva con pago ya registrado a un `finalValue` menor que lo pagado
      deja el excedente como `creditBalance`, `paymentStatus = SALDO_A_FAVOR_PENDIENTE` y
      `refundDecisionStatus = PENDIENTE_AUTORIZACION`.
- [x] Modificar una reserva `EnEjecucion`, `Finalizada` o `Cancelada` devuelve `409`.
- [x] Modificar una reserva con una transferencia pendiente de decisión devuelve `409`.
- [x] `reservedServices` vacío, `projectedValue` negativo o motivo vacío devuelve `400`.
- [x] Cualquier operación sobre un `tenantId` inexistente devuelve `404`; sobre uno
      `Inactivo`, `409`.
- [x] El proyecto compila y los tests existentes (specs 001-021) siguen pasando.

## Impacto en multitenencia

Mismo patrón que las specs anteriores: la modificación filtra siempre por `tenantId` de
la URL además de `reservationId`; no hay ningún dato ni cálculo que cruce tenants.

## Riesgos y decisiones abiertas

1. **¿`reservationStatus`/`paymentStatus` se recalculan automáticamente si el nuevo
   `pendingBalance` queda en positivo sobre una reserva que era `Confirmada`?** (ej.
   volvería a quedar con saldo pendiente). Opciones: (a) revertir a `PENDIENTE_DE_PAGO`/
   `PARCIAL` automáticamente, o (b) dejar `reservationStatus`/`paymentStatus` intactos y
   que un pago posterior los actualice, igual que hoy. Se decide en `/plan-tareas`.
2. **Reemplazo completo vs. parcial de `reservedServices`**: el Frontend siempre maneja
   un solo servicio por reserva y lo reemplaza entero; se sigue ese criterio salvo
   objeción del humano.

## Evidencia para la materia

Primer caso de uso que reutiliza el mecanismo de saldo a favor / solicitud de devolución
(`RefundDecisionStatus`, spec 019) fuera del flujo de cancelación, demostrando que ese
diseño es reutilizable en vez de estar acoplado a `cancel()`. Demostrable con `curl`
(modificar con saldo a favor de $0, con saldo pendiente mayor, y con excedente que genera
solicitud de devolución), mismo patrón que las specs anteriores de `reservations`.
