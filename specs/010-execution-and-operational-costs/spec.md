# 010 — Ejecución de reservas y costos operacionales

**Estado:** TERMINADA
**Fecha:** 2026-09-03
**Repos afectados:** backend (rama `hu-back-001-dev`)
**HU relacionada:** ninguna HU formal en el backlog cubre esto punto por punto; se basa
en RF-007/RF-009 del PRD (`03-product/prd.md`) y en las pantallas ya construidas en el
Frontend (rama `develop`, commit `93dd4dc`, aún no en `main`): `operator/operation`,
`operator/register-execution`, vía `OperatorOperationService` (`registerExecution`,
`getExecution`, `registerCost`, `getAllCosts`).

## Problema

`Reservation` hoy solo puede llegar hasta `CONFIRMADA` (spec 009); no existe ningún caso
de uso que la mueva a `EN_EJECUCION`, que registre lo efectivamente prestado frente a lo
reservado, ni que registre los costos operacionales asociados a esa ejecución. El
Frontend ya construyó esas pantallas (simuladas en `localStorage`) con reglas concretas
de negocio (solo se puede iniciar ejecución sobre una reserva `Confirmada`, causal
obligatoria si el servicio no se prestó, el costo solo se habilita sobre una ejecución
ya iniciada), pero no hay backend real detrás.

## Alcance

- Nuevo caso de uso "registrar ejecución de una reserva" (RF-007,
  `register-execution.component.ts`): solo permitido sobre una reserva en estado
  `CONFIRMADA` que todavía no tenga una ejecución registrada. Recibe si el servicio se
  prestó o no, la cantidad de personas efectivamente atendidas (si se prestó) y una
  causal obligatoria (si no se prestó). Al registrarse: la reserva pasa a
  `EN_EJECUCION` y queda guardado `served`/`executed`/`causal`/actor/fecha, asociado a
  esa reserva. Un único registro de ejecución por reserva (no un historial de
  reintentos — mismo criterio que el Frontend).
- Consultar la ejecución registrada de una reserva.
- Consultar, por tenant, las reservas `CONFIRMADA` que todavía no tienen ejecución
  registrada (equivalente a "próximas ejecuciones" en `operation.component.ts`).
- Nuevo caso de uso "registrar costo operacional" (RF-009,
  `operation.component.ts`, panel de costo): solo permitido sobre una reserva que ya
  tenga una ejecución registrada. Recibe un concepto (texto) y un monto (positivo). Se
  acumulan varios costos por reserva, ninguno reemplaza al anterior — mismo criterio
  que `operator-operation.service.ts` (`registerCost` hace `push`, no reemplazo).
- Consultar los costos operacionales registrados, por reserva y por tenant.
- Todas las operaciones rechazan tenant inexistente (`404`) o `Inactivo` (`409`), mismo
  criterio que el resto del proyecto.

## Fuera de alcance

- Tolerancia operativa de 10 minutos ni control de salida del guía (RN-EJE-001): el
  Frontend no la implementa, solo un booleano prestado/no prestado + causal; no se
  inventa ese cronómetro.
- Bloqueo de ajustes ordinarios sobre una reserva ya en ejecución (RF-008,
  RN-EJE-002/004): hoy no existe ningún caso de uso de "modificar reserva" en el
  backend (solo creación, spec 001) contra el cual aplicar ese bloqueo; se revisita si
  una spec futura lo introduce.
- Cancelación extraordinaria por emergencia durante la ejecución, y el reagendamiento o
  devolución asociados (RN-EJE-002/006): specs futuras (011 cancelación/modificación,
  012 devoluciones).
- Cálculo o parametrización de costos operacionales por atractivo/catálogo
  (RN-ATR-001): el Frontend registra el costo como texto libre (concepto) + monto
  manual, no como un valor derivado de `CatalogItem`; se sigue el mismo criterio simple,
  sin vincular el costo a ningún `catalogItemId`.
- Caja como tal (que el costo operacional se refleje como salida de dinero, RN-CAJ-001 /
  RF-010): spec futura (013), que leerá estos costos, no los registra ella misma.
- Dashboard operativo y reportes agregados (RF-011/RF-012, ventas por día, consolidado
  mensual, etc.): fuera de esta spec — se decide más adelante si va en 013 o en una
  spec de Reportes aparte.
- Registrar el resultado de la ejecución por cada servicio reservado individualmente
  (tour, hospedaje, alimentación por separado): el Frontend registra un único
  resultado (prestado/no prestado) por reserva completa, no por servicio.
- Validar la cantidad ejecutada contra un tope de "personas reservadas": el propio
  Frontend no aplica ese tope (`Math.max(0, executed)`, sin límite superior); no se
  inventa esa validación.

## Criterios de aceptación

- [x] Registrar la ejecución de una reserva `CONFIRMADA` marcando el servicio como
      prestado devuelve `200`, deja `reservationStatus = EN_EJECUCION`, y guarda
      `executed`/`served`/actor/fecha.
- [x] Registrar la ejecución marcando el servicio como no prestado exige una causal: sin
      causal devuelve `400`; con causal devuelve `200` y `reservationStatus =
      EN_EJECUCION` igual que si se hubiera prestado.
- [x] Registrar una ejecución sobre una reserva que no está `CONFIRMADA` (por ejemplo
      `PENDIENTE_DE_PAGO`) no la modifica y devuelve un código de error (400 o 409,
      exacto a definir en el plan).
- [x] Registrar una segunda ejecución sobre una reserva que ya tiene una ejecución
      registrada devuelve `409`.
- [x] Consultar la ejecución registrada de una reserva devuelve los datos guardados
      (prestado/no, cantidad ejecutada, causal, actor, fecha).
- [x] Consultar las reservas pendientes de ejecución de un tenant devuelve solo
      reservas `CONFIRMADA` sin ejecución registrada, y solo las de ese tenant.
- [x] Registrar un costo operacional sobre una reserva sin ejecución registrada no
      queda guardado y devuelve un código de error (400 o 409, exacto a definir en el
      plan).
- [x] Registrar un costo operacional con concepto vacío o monto menor o igual a cero
      devuelve `400`.
- [x] Registrar un costo operacional válido sobre una reserva con ejecución ya iniciada
      devuelve éxito (200 o 201, a definir en el plan) y queda consultable después.
- [x] Registrar un segundo costo operacional sobre la misma reserva se acumula junto al
      primero, sin reemplazarlo.
- [x] Consultar los costos operacionales de una reserva devuelve todos los registrados,
      en orden cronológico.
- [x] Cualquier operación sobre un `tenantId` inexistente devuelve `404`; sobre uno
      `Inactivo`, `409`.
- [x] El proyecto compila y los tests existentes (specs 001-009) siguen pasando.

## Impacto en multitenencia

Mismo patrón que las specs anteriores: todas las operaciones nuevas filtran siempre por
`tenantId` de la URL además de `reservationId`.

## Riesgos y decisiones abiertas

1. **Código de error exacto para "ejecución/costo sobre estado inválido"**: `400`
   (regla de negocio incumplida) vs `409` (conflicto de estado). Se decide en
   `/plan-tareas` siguiendo el criterio ya usado en spec 009 (`PaymentAlreadyResolvedException`
   usa `409` para "ya resuelto"; esto se parece más a "la reserva no cumple la
   precondición de estado").
2. **Dónde vive el registro de ejecución y de costos**: ¿se extiende el agregado
   `Reservation` (como hizo spec 009 con el pago) o se crea un nuevo bounded context
   `operations` con sus propias entidades? Se decide en `/plan-tareas`, no cambia
   ningún criterio de aceptación.
3. **Cómo se interpreta "personas reservadas" en el backend real**: el Frontend usa un
   campo `travelers` simulado que no existe en el agregado `Reservation` real (solo
   `partySize`, opcional, por cada `ReservedService`). Como el criterio de aceptación
   no exige validar `executed` contra ningún tope, esto no bloquea la spec — se resuelve
   en el plan solo como dato informativo a guardar, no como regla de validación.

## Evidencia para la materia

Primer caso de uso que mueve `Reservation` a `EN_EJECUCION` y primer registro de costos
operacionales del proyecto; demostrable con `curl` (registrar ejecución prestada/no
prestada, registrar costo, consultar ambos), mismo patrón que las specs anteriores.
