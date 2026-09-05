# 017 — Finalización de la ejecución de una reserva

**Estado:** TERMINADA
**Fecha:** 2026-09-04
**Repos afectados:** backend
**HU relacionada:** ninguna HU formal en el backlog cubre esto punto por punto; se basa en
la Sección 16 "Reserva" del PRD (transición `En ejecución` → `Finalizada`, "cuando termina
la prestación del servicio y se cierra operativamente") y en la pantalla ya construida en
el Frontend (`operation.component.ts`, botón "Finalizar ejecución"), vía
`OperatorOperationService.finalizeExecution()` (`operator-operation.service.ts:139`), que
hoy cierra la ejecución solo en `localStorage` con un comentario explícito: "BACKEND API
FALTANTE — FINALIZAR EJECUCIÓN: no existe endpoint real" (línea 134-137).

## Problema

`Reservation` hoy puede llegar hasta `EN_EJECUCION` (spec 010, `Reservation.startExecution()`),
pero no existe ningún caso de uso que la mueva a `FINALIZADA`. El valor
`ReservationStatus.FINALIZADA` existe en el enum del dominio desde el inicio del proyecto
pero ningún código lo produce. El Frontend ya construyó la pantalla y el botón que cierran
la ejecución, simulados en `localStorage` sobre el mismo registro `Execution` que registra
spec 010 (`served`/`executed`/`causal`), agregando `finalized`/`finalizedAt`/`finalizedBy`.

## Alcance

- Nuevo caso de uso "finalizar ejecución de una reserva": permitido solo cuando
  `reservationStatus` es `EN_EJECUCION` y existe un registro de `Execution` para esa
  reserva (spec 010). Recibe un actor. Al finalizarse: la reserva pasa a `FINALIZADA` y
  queda guardado quién y cuándo finalizó, asociado a esa reserva o a su `Execution`
  (se decide en `/plan-tareas` cuál agregado lo guarda, sin cambiar ningún criterio de
  aceptación).
- Rechazar la finalización si la reserva no está `EN_EJECUCION` (por ejemplo
  `CONFIRMADA`, `PENDIENTE_DE_PAGO`, `CANCELADA` o ya `FINALIZADA`): mismo criterio de
  error que usan spec 010/011 para "la reserva no cumple la precondición de estado".
- Rechazar una segunda finalización sobre la misma reserva.
- Consultar el estado de finalización junto con el resto de la ejecución (extender la
  respuesta ya existente de `GET .../execution`, spec 010), sin crear un endpoint de
  consulta nuevo.
- Todas las operaciones rechazan tenant inexistente (`404`) o `Inactivo` (`409`), mismo
  criterio que el resto del proyecto.

## Fuera de alcance

- Cualquier cambio a precio, descuentos, pagos o saldo de la reserva: la propia
  finalización, según el Frontend, "no modifica precio, descuentos, pagos ni saldo: solo
  cierra la ejecución".
- Reversar una finalización (volver de `FINALIZADA` a `EN_EJECUCION`): no existe ninguna
  pantalla ni acción en el Frontend que lo necesite.
- Cualquier reporte o dashboard agregado sobre reservas finalizadas (RF-011/RF-012):
  igual que quedó fuera en spec 010, se decide en una spec de Reportes aparte si se
  necesita.
- Vincular la finalización a costos operacionales adicionales: eso ya lo cubre el caso de
  uso "registrar costo operacional" de spec 010 (`registerCost`), que no cambia con esta
  spec.
- Finalización automática por vencimiento de plazo o tolerancia operativa (RN-EJE-001):
  no existe mecanismo de temporizador/job en el proyecto; mismo criterio ya excluido en
  spec 010.

## Criterios de aceptación

- [x] Finalizar la ejecución de una reserva `EN_EJECUCION` con una `Execution` registrada
      devuelve `200`, deja `reservationStatus = FINALIZADA`, y guarda quién y cuándo se
      finalizó.
- [x] Finalizar una reserva que no está `EN_EJECUCION` (`PENDIENTE_DE_PAGO`, `CONFIRMADA`,
      `CANCELADA` o `FINALIZADA`) no la modifica y devuelve `409`.
- [x] Finalizar una reserva `EN_EJECUCION` que no tiene ninguna `Execution` registrada
      (caso que hoy no debería poder ocurrir, porque `startExecution()` y el registro de
      `Execution` van de la mano) devuelve un código de error y no la modifica. No se
      probó en runtime porque el flujo normal no permite construir ese estado: se verifica
      por diseño (ver `plan.md`), gracias a que `RegisterExecutionService.registerExecution()`
      es `@Transactional` y crea la `Execution` en la misma transacción donde
      `Reservation.startExecution()` mueve el estado a `EN_EJECUCION`.
- [x] Finalizar dos veces la misma reserva: la primera devuelve `200`; la segunda
      devuelve `409` y no cambia el registro ya finalizado.
- [x] Consultar la ejecución de una reserva finalizada (`GET .../execution`) incluye el
      estado de finalización (finalizado, actor, fecha) junto con los demás datos ya
      existentes de spec 010.
- [x] El precio, descuentos, pago y saldo de la reserva finalizada quedan exactamente
      iguales a como estaban antes de finalizar.
- [x] Cualquier operación sobre un `tenantId` inexistente devuelve `404`; sobre uno
      `Inactivo`, `409`.
- [x] El proyecto compila y los tests existentes (specs 001-016) siguen pasando.

## Impacto en multitenencia

Mismo patrón que las specs anteriores: la finalización filtra siempre por `tenantId` de
la URL además de `reservationId`; no hay ningún dato ni cálculo que cruce tenants.

## Riesgos y decisiones abiertas

1. **Dónde vive el estado de finalización**: ¿se agrega como método `finalize()` al
   agregado `Reservation` (como hizo `startExecution()`/`cancel()`), y/o se extiende el
   registro `Execution` de spec 010 con `finalizedAt`/`finalizedBy` (paralelo a como el
   Frontend extiende su `ReservationExecution` local)? Se decide en `/plan-tareas`, no
   cambia ningún criterio de aceptación.
2. **Código de error exacto para "finalizar sin `Execution` registrada"**: caso teórico
   que no debería ocurrir en flujo normal (si la reserva está `EN_EJECUCION`, ya pasó por
   `registerExecution`); se decide en `/plan-tareas` si amerita una excepción propia o si
   basta con `ExecutionNotFoundException` ya existente de spec 010.

## Evidencia para la materia

Primer caso de uso que mueve `Reservation` a `FINALIZADA`, cerrando el ciclo de vida
completo del estado (`PENDIENTE_DE_PAGO` → `CONFIRMADA` → `EN_EJECUCION` → `FINALIZADA`,
con la rama alterna `CANCELADA`); demostrable con `curl` (finalizar ejecución en curso,
rechazo por estado inválido, rechazo por doble finalización, consulta de ejecución
finalizada), mismo patrón que las specs anteriores.
