# 013 — Caja diaria (base, movimientos, cierre y consolidación)

**Estado:** APROBADA — alcance actualizado 2026-09-04 tras push real de Frontend (ver
nota de reanudación y segunda revisión más abajo). `/plan-tareas` completado
(`plan.md`, `tasks.md`); lista para implementación (T01).
**Fecha:** 2026-09-03 (alcance revisado 2026-09-04)
**Repos afectados:** backend (rama `hu-back-001-dev`)
**HU relacionada:** ninguna HU formal en el backlog; se basa en RF-010/RN-CAJ-001 del PRD
(`03-product/prd.md`). Confirmado con `git pull` fresco en Frontend: el ítem de navegación
"Caja" en `operator-shell.component.html` sigue sin `routerLink`, y el dashboard operativo
(`operator/dashboard/dashboard.component.html` + `.ts`) solo muestra una cifra estática
("Total operativo de caja: $1.840.000") con un enlace `<a>Consultar caja</a>` sin
`routerLink` ni handler, sobre un componente vacío (`export class DashboardComponent {}`).
No existe ningún consumidor real de Caja en el Frontend. Esta spec no sigue ninguna
pantalla existente, sino directamente al PRD, con el alcance recortado al mínimo
verificable — mismo criterio que specs 011/012, que dejaron explícitamente diferido este
módulo (spec 010, sección "Fuera de alcance": *"Caja como tal ... spec futura (013), que
leerá estos costos, no los registra ella misma"*).

RN-CAJ-001 es una regla amplia (base parametrizable, ingresos, pagos, gastos, devoluciones,
cierre formal, histórico, corrección excepcional post-cierre, consolidación mensual): no
cabe en una sola spec. Se parte en dos: esta spec (013) cubre el ciclo de un día de caja
(abrir, registrar movimientos, cerrar, consultar); la consolidación mensual (RF-012) y la
corrección excepcional post-cierre quedan para una spec futura (014), después de esta.

## Problema

Hoy no existe ningún registro de caja: los costos operacionales (spec 010) y los pagos/
devoluciones de reserva (specs 009/012) ocurren sin que quede ninguna base diaria, ningún
movimiento de caja identificable, ni un total operativo del día. No hay forma de verificar
`BASE + INGRESOS - PAGOS - GASTOS - DEVOLUCIONES = TOTAL` para ninguna jornada.

## Alcance

- Nuevo módulo `cash`, mismo patrón hexagonal que `operations`/`reservations`.
- **Abrir caja del día**: `tenantId`, fecha (`businessDate`), `baseAmount` (base
  parametrizable, ingresada por quien abre — sin validar rol Administrador real, mismo
  criterio que `actorId` de texto libre en specs 011/012), `actorId`. Una sola caja
  `ABIERTA` por `tenantId` + `businessDate`; abrir una segunda para la misma fecha con una
  ya abierta devuelve `409`.
- **Registrar movimiento**: sobre una caja `ABIERTA`, con tipo (`INGRESO`, `PAGO`, `GASTO`),
  monto positivo, concepto (texto libre) y `actorId`. **`DEVOLUCION` no es un tipo
  aceptado por este endpoint** — enviarlo devuelve `400` (ver bullet de integración con
  spec 012 más abajo: se genera solo, nunca a mano). Registrar sobre una caja `CERRADA`
  devuelve `409`; monto `<= 0` o campos obligatorios vacíos devuelve `400`.
- **Cerrar caja del día**: solo si está `ABIERTA`. Calcula y congela
  `totalAmount = baseAmount + ingresos - pagos - gastos - devoluciones`, pasa a `CERRADA`,
  registra `closedBy`/`closedAt`. Cerrar una caja ya `CERRADA` devuelve `409`. Tras el
  cierre no se admiten más movimientos sobre esa caja.
- **Consultar caja**: por `tenantId` + `businessDate` (incluye estado, base, movimientos y
  total — calculado en vivo si está `ABIERTA`, congelado si está `CERRADA`), y listado
  histórico de cajas cerradas por `tenantId`.
- Todas las operaciones rechazan tenant inexistente (`404`) o `Inactivo` (`409`), mismo
  criterio que el resto del proyecto.
- **Consolidación mensual** (RF-012, parte de RN-CAJ-001: "consolidacion mensual ...
  sin sumar repetidamente cada base diaria"): por `tenantId` + periodo (`YYYY-MM`), agrupa
  los cierres `CERRADA`s de ese mes y devuelve **7 valores**: ingresos, pagos
  operacionales, gastos, devoluciones y total sumados (de los cierres de caja),
  **más `cancelaciones` (conteo de reservas canceladas en ese periodo, spec 011) y
  `costosOperacionales` (suma de costos operacionales registrados en ese periodo, spec
  010)** — mismos 7 valores que consume `cash-monthly.component.html` desde
  `OperatorCashService.getMonthlyConsolidation()` (interfaz `MonthlyConsolidation`, 8
  campos incluyendo `period`), reutilizados también por "Reportes"
  (`operator-reports.service.ts`) filtrando el mismo resultado por periodo. Confirmado el
  2026-09-04 contra el componente real: `cancelaciones` y `costosOperacionales` no
  estaban en la versión anterior de esta spec — es un vacío real, no solo texto
  desactualizado. Requiere lectura cruzada de `reservations` (reservas `CANCELADA` con
  `cancelledAt` en el periodo) y `operations` (`OperationCost.recordedAt` en el periodo);
  mecanismo exacto (nuevo método de query vs. filtrar `findAllByTenantId` ya existente en
  la capa de aplicación) se decide en `/plan-tareas`.
- **Corrección excepcional post-cierre**: sobre una caja `CERRADA`, un actor puede agregar
  una corrección con justificación — no reabre el cierre ni cambia `totalAmount`
  congelado, queda registrada como historial adicional (justificación, autor, fecha)
  visible al consultar esa caja. Confirmado el 2026-09-04: la pantalla real ya la
  implementa (`cash-history.component`, `addCorrection()`), sin validar rol Administrador
  real (mismo criterio recurrente del proyecto sin JWT de operador).
- **Movimiento `DEVOLUCION` generado por integración con spec 012, no manual**: la
  pantalla real (`registerMovement()`) excluye `Devolución` de los tipos registrables a
  mano — lo calcula siempre a partir de las devoluciones ya `Ejecutada` de spec 012
  (`getExecutedRefundMovements()`). Cuando spec 012 ejecuta una devolución sobre una
  reserva de ese `tenantId`+`businessDate`, `cash` debe reflejarla en el total sin que
  nadie la registre por el endpoint genérico de movimientos. `RefundRequest` (spec 012,
  Frontend) ya reserva un campo `cashMovementRef` para este vínculo. Mecanismo exacto
  (escritura de un movimiento real al ejecutar la devolución, vs. cálculo en vivo al
  consultar caja) se decide en `/plan-tareas` — no cambia ningún criterio de aceptación.

## Fuera de alcance

- **Integración automática con pagos de reserva** (spec 009 pago en efectivo no genera un
  movimiento de caja automáticamente): `INGRESO`, `PAGO` y `GASTO` se siguen registrando
  manualmente por el endpoint propio de este módulo — la pantalla real confirma este
  comportamiento tal cual estaba previsto; solo `DEVOLUCION` cambió de manual a
  automático (ver arriba). Acoplar `reservations` a `cash` para `INGRESO` se revisita si
  una pantalla real de Frontend lo pide.
- **Lectura de `OperationCost`** (spec 010) desde `cash` para poblar pagos/gastos
  automáticamente: spec 010 ya anticipó que "013 ... leerá estos costos", pero sin un
  consumidor real que lo exija, se deja fuera; los movimientos de caja se registran todos
  manualmente por ahora.
- Validación real de rol Administrador para abrir/cerrar caja o fijar la base: no existe
  módulo de roles ni JWT de operador en el backend (mismo criterio recurrente).
- **Endpoint nuevo para el dashboard operativo**: `dashboard.component.ts` ya muestra el
  total de caja del día (`cajaTotalLabel`), pero lo lee de los mismos datos que
  "Consultar caja" (día actual) — no hace falta un endpoint aparte, solo que "Consultar
  caja" ya cubierto en el alcance exista. (Corrección 2026-09-04: la nota anterior decía
  que el dashboard seguía hardcodeado; ya no es así, pero no cambia el alcance del
  backend.)
- Multitenencia entre historial de cajas de distintos tenants: cada consulta filtra por
  `tenantId`, sin cruces — cubierto en la sección de impacto, no como caso de uso aparte.

## Criterios de aceptación

- [ ] Abrir caja con `tenantId`, `businessDate`, `baseAmount` y `actorId` válidos devuelve
      `201`, estado `ABIERTA`, `totalAmount` inicial igual a `baseAmount` (sin movimientos).
- [ ] Abrir una segunda caja para el mismo `tenantId` + `businessDate` mientras la primera
      sigue `ABIERTA` devuelve `409`.
- [ ] Registrar un movimiento `INGRESO` sobre una caja `ABIERTA` devuelve `200`/`201` y el
      `totalAmount` consultado sube por ese monto.
- [ ] Registrar movimientos `PAGO`, `GASTO` y `DEVOLUCION` sobre una caja `ABIERTA` hacen
      bajar el `totalAmount` consultado por cada monto respectivo.
- [ ] Registrar un movimiento con tipo `DEVOLUCION` en el endpoint genérico devuelve
      `400` — no es un tipo aceptado manualmente (se genera solo, ver AC de integración
      con spec 012 más abajo).
- [ ] Registrar un movimiento con monto `<= 0`, sin tipo, o sin `actorId`/concepto devuelve
      `400`.
- [ ] Registrar un movimiento sobre una caja `CERRADA` devuelve `409` y no la modifica.
- [ ] Cerrar una caja `ABIERTA` devuelve `200`, pasa a `CERRADA`, y dejan registrados
      `closedBy`, `closedAt` y `totalAmount = baseAmount + ingresos - pagos - gastos -
      devoluciones` calculado con los movimientos registrados.
- [ ] Cerrar una caja ya `CERRADA` devuelve `409`.
- [ ] Consultar la caja de un `tenantId` + `businessDate` devuelve estado, base, todos los
      movimientos registrados y el total (en vivo si `ABIERTA`, congelado si `CERRADA`).
- [ ] Consultar el histórico de cajas `CERRADA`s de un `tenantId` devuelve la lista
      ordenada, sin mezclar cajas de otros tenants.
- [ ] Cualquier operación sobre un `tenantId` inexistente devuelve `404`; sobre uno
      `Inactivo`, `409`.
- [ ] Al ejecutarse una devolución (spec 012) sobre una reserva, el total de caja del
      `tenantId`+`businessDate` correspondiente refleja esa devolución sin que nadie la
      registre manualmente por el endpoint de movimientos.
- [ ] Consultar la consolidación mensual de un `tenantId` + periodo (`YYYY-MM`) devuelve
      ingresos, pagos operacionales, gastos, devoluciones y total sumados a partir de los
      cierres `CERRADA`s de ese periodo (sin duplicar jornadas), **más `cancelaciones`
      (conteo de reservas `CANCELADA` con `cancelledAt` en ese periodo) y
      `costosOperacionales` (suma de `OperationCost` con `recordedAt` en ese periodo)**.
- [ ] Agregar una corrección justificada sobre una caja ya `CERRADA` no reabre el cierre
      ni cambia su `totalAmount` congelado: queda registrada como historial adicional
      (justificación, autor, fecha) visible al consultar esa caja.
- [ ] El proyecto compila y los tests existentes (specs 001-012) siguen pasando.

## Impacto en multitenencia

Toda caja y todo movimiento quedan asociados a un `tenantId`; abrir, registrar, cerrar y
consultar siempre filtran por el `tenantId` de la URL además de `businessDate`/
`cashRegisterId`. El histórico nunca cruza cajas de distintos tenants — mismo patrón que
`reservations`/`operations`.

## Riesgos y decisiones abiertas

1. **Nombre del módulo y del agregado**: `cash` con `CashRegister` (caja del día) +
   `CashMovement` (movimiento), o un solo agregado `CashRegister` con lista interna de
   movimientos. Se decide en `/plan-tareas`, no cambia ningún criterio de aceptación.
2. **Persistencia de movimientos**: tabla propia `cash_movements` con FK a
   `cash_registers`, siguiendo el mismo patrón relacional que `reservations`/
   `reserved_services`. Se confirma en `/plan-tareas`.
3. **Clave de unicidad de "una caja abierta por día"**: constraint de base de datos
   (`UNIQUE(tenant_id, business_date)` + regla de negocio que además exige que no haya
   otra `ABIERTA`) vs. solo validación en el caso de uso. Se decide en `/plan-tareas`.
4. **Mecanismo del vínculo `cash` ↔ devolución (spec 012)**: escritura de un
   `CashMovement` real en el momento en que spec 012 ejecuta la devolución (evento/llamada
   directa entre módulos) vs. cálculo en vivo al consultar caja (consulta a `refunds`
   filtrando `EJECUTADA` de ese `tenantId`+`businessDate`, sin persistir el movimiento).
   Se decide en `/plan-tareas`; no cambia ningún criterio de aceptación.

## Evidencia para la materia

Primer módulo que cierra el ciclo BASE + INGRESOS - PAGOS - GASTOS - DEVOLUCIONES = TOTAL
descrito en RN-CAJ-001, demostrable con `curl` (abrir caja, registrar los 4 tipos de
movimiento, cerrar, consultar total congelado, rechazo por caja cerrada, rechazo por caja
duplicada), mismo patrón que las specs anteriores.

## Nota de pausa (2026-09-03/04, histórica)

Aprobada formalmente por el humano, pero con el ciclo de specs de backend **en pausa**
inmediatamente después: se confirmó (grep de `HttpClient`/`fetch`/`apiUrl`/
`provideHttpClient` en todo `src/app` del Frontend) que el Frontend no tenía ninguna
integración HTTP real con el backend en ninguna pantalla. Además, a diferencia de toda
otra spec ya construida (001-012), que sí mapeaba a una ruta real en `app.routes.ts`
(aunque sin lógica real detrás), "Caja" ni siquiera tenía una ruta.

Condición de salida fijada entonces: retomar cuando el Frontend tuviera cambios reales
que `git pull` mostrara — una ruta de Caja real, o el primer `HttpClient`/integración real
con cualquiera de las 12 specs ya construidas.

## Nota de reanudación (2026-09-04)

Push nuevo de Fernanda Robayo a `develop` (`93dd4dc`, `b0bf23e`) cumple la condición de
salida: `app.routes.ts` ya tiene `{ path: 'cash', ... }`, `cash/history` y `cash/monthly`
reales (confirmado por diff contra el estado previo, que no las tenía). `HttpClient`
sigue sin existir en la app — la integración es solo de rutas/pantallas, no de red — pero
eso ya no bloquea: el criterio de salida era "ruta real **o** integración HTTP", no ambas.

Al revisar el contenido real de esas pantallas (`operator-cash.service.ts`,
`cash-history.component.ts`, `cash-monthly.component.ts`) contra esta spec aparecieron 3
desalineaciones, ya corregidas arriba (alcance, fuera de alcance, criterios de
aceptación): consolidación mensual y corrección post-cierre pasan de "fuera de alcance /
spec futura" a **dentro de esta spec**, y el movimiento `DEVOLUCION` pasa de "registrable
manualmente" a "generado por integración con spec 012". Nada de esto cambia el problema
ni el módulo (`cash`), así que se corrige la misma spec en vez de crear una nueva.

Decisión: el ciclo de specs de backend se retoma. Esta spec queda lista para
`/plan-tareas`. El resto del push de hoy (portal de cliente, colaboradores, transporte)
se revisó por separado — colaboradores no tiene spec de backend todavía (candidata a spec
nueva, después de esta), transporte es una brecha chica sobre la spec 005 ya implementada
(falta `TRANSPORT` en `CatalogItemType`), y el portal de cliente sigue siendo simulación
local sin backend real detrás.

## Segunda revisión de alineación (2026-09-04, antes de `/plan-tareas`)

El humano pidió una revisión más profunda antes de aprobar `/plan-tareas`, para detectar
contradicciones que la corrección anterior dejó a medias (esa corrección solo tocó 3
puntos puntuales, sin releer la spec completa contra el código real). Se leyó el contenido
íntegro de los 5 archivos reales de `origin/develop` que tocan caja
(`operator-cash.service.ts`, `cash.component.ts`, `cash-history.component.ts`,
`cash-monthly.component.ts`, `dashboard.component.ts`) y el consumidor indirecto
(`operator-reports.service.ts`). Se corrigieron 3 inconsistencias adicionales,
directamente en las secciones de arriba (no como parche aparte):

1. **Contradicción `DEVOLUCION`**: "Alcance" seguía diciendo que el endpoint genérico de
   movimientos aceptaba `DEVOLUCION` a mano; la corrección anterior ya decía lo contrario
   en otro bullet, y dos criterios de aceptación se contradecían entre sí. Se resolvió a
   favor de "no aceptado, `400`" — respaldado por la firma real de
   `registerMovement(type: 'Ingreso' | 'Pago operacional' | 'Gasto', ...)`, que excluye
   `Devolución` a nivel de tipo, más una guarda explícita en tiempo de ejecución
   (`type === 'Devolución'` rechazado) en `cash.component.ts`.
2. **Consolidación mensual incompleta**: la spec solo listaba 5 valores agregados
   (ingresos, pagos, gastos, devoluciones, total). La interfaz real `MonthlyConsolidation`
   y la plantilla `cash-monthly.component.html` tienen 8 campos — faltaban
   `cancelaciones` y `costosOperacionales`, ambos de lectura cruzada con `reservations` y
   `operations`. Es alcance nuevo real, no cosmético: agregado al bullet de "Alcance" y a
   los criterios de aceptación.
3. **"Fuera de alcance" desactualizado**: decía que el dashboard operativo mostraba un
   valor hardcodeado sin consumidor. Ya no es cierto — `dashboard.component.ts` ahora lee
   el total de caja en vivo (`cajaTotalLabel`) desde el mismo `OperatorCashService`. No
   cambia el alcance del backend (reutiliza "Consultar caja"), pero la justificación
   escrita ya no era verdad y se corrigió.

Ningún otro archivo del Frontend real referencia `OperatorCashService` fuera de estos 6
(confirmado por búsqueda completa sobre `origin/develop`). No se encontraron más
contradicciones. Spec 013 queda alineada con el push actual y lista para `/plan-tareas`.
