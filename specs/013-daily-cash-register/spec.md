# 013 — Caja diaria (base, movimientos y cierre)

**Estado:** APROBADA (implementación en pausa — ver nota de cierre de sesión más abajo)
**Fecha:** 2026-09-03
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
- **Registrar movimiento**: sobre una caja `ABIERTA`, con tipo (`INGRESO`, `PAGO`, `GASTO`,
  `DEVOLUCION`), monto positivo, concepto (texto libre) y `actorId`. Para `DEVOLUCION`,
  acepta opcionalmente un `reservationId` de referencia (texto libre, sin validar contra
  `reservations`) para satisfacer RN-CAJ-001 ("asociado a la reserva correspondiente"), sin
  clasificarse nunca como `GASTO`. Registrar sobre una caja `CERRADA` devuelve `409`; monto
  `<= 0` o campos obligatorios vacíos devuelve `400`.
- **Cerrar caja del día**: solo si está `ABIERTA`. Calcula y congela
  `totalAmount = baseAmount + ingresos - pagos - gastos - devoluciones`, pasa a `CERRADA`,
  registra `closedBy`/`closedAt`. Cerrar una caja ya `CERRADA` devuelve `409`. Tras el
  cierre no se admiten más movimientos sobre esa caja.
- **Consultar caja**: por `tenantId` + `businessDate` (incluye estado, base, movimientos y
  total — calculado en vivo si está `ABIERTA`, congelado si está `CERRADA`), y listado
  histórico de cajas cerradas por `tenantId`.
- Todas las operaciones rechazan tenant inexistente (`404`) o `Inactivo` (`409`), mismo
  criterio que el resto del proyecto.

## Fuera de alcance

- **Consolidación mensual** (RF-012, parte de RN-CAJ-001: "consolidacion mensual ...
  sin sumar repetidamente cada base diaria"): se construye a partir de las cajas cerradas
  de esta spec, pero es una spec propia (014) — no hay pantalla que la pida hoy.
- **Corrección excepcional post-cierre** (observación de RN-CAJ-001: reapertura/ajuste
  restringido a Administrador con justificación y trazabilidad): el propio PRD deja el
  mecanismo técnico "sujeto a definición de arquitectura"; no se inventa aquí. Una caja
  `CERRADA` es inmutable en esta spec.
- **Integración automática con pagos/devoluciones de reserva** (spec 009 pago en efectivo,
  spec 012 ejecución de devolución no generan un movimiento de caja automáticamente): los
  movimientos se registran manualmente por un endpoint propio de este módulo. Acoplar
  `reservations`/`operations` a `cash` se revisita si una pantalla real de Frontend lo
  necesita — mismo criterio que la integración diferida de OperationCost en spec 010.
- **Lectura de `OperationCost`** (spec 010) desde `cash` para poblar pagos/gastos
  automáticamente: spec 010 ya anticipó que "013 ... leerá estos costos", pero sin un
  consumidor real que lo exija, se deja fuera; los movimientos de caja se registran todos
  manualmente por ahora.
- Validación real de rol Administrador para abrir/cerrar caja o fijar la base: no existe
  módulo de roles ni JWT de operador en el backend (mismo criterio recurrente).
- Dashboard operativo o reportes agregados con los datos de caja: no hay pantalla que
  consuma esto hoy (el "Total operativo de caja" del dashboard operator es texto
  hardcodeado, sin `routerLink` ni servicio).
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
- [ ] Registrar un movimiento `DEVOLUCION` con `reservationId` de referencia queda visible
      al consultar ese movimiento, y no se clasifica como `GASTO`.
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

## Evidencia para la materia

Primer módulo que cierra el ciclo BASE + INGRESOS - PAGOS - GASTOS - DEVOLUCIONES = TOTAL
descrito en RN-CAJ-001, demostrable con `curl` (abrir caja, registrar los 4 tipos de
movimiento, cerrar, consultar total congelado, rechazo por caja cerrada, rechazo por caja
duplicada), mismo patrón que las specs anteriores.

## Nota de cierre de sesión (2026-09-04)

Aprobada formalmente por el humano, pero con el ciclo de specs de backend **en pausa**
inmediatamente después: se confirmó (grep de `HttpClient`/`fetch`/`apiUrl`/
`provideHttpClient` en todo `src/app` del Frontend) que el Frontend no tiene ninguna
integración HTTP real con el backend en ninguna pantalla — ni siquiera `HttpClient` está
provisto en la app. Además, a diferencia de toda otra spec ya construida (001-012), que sí
mapea a una ruta real en `app.routes.ts` (aunque esa ruta no tenga lógica real detrás),
**"Caja" ni siquiera tiene una ruta**: no existe `{ path: 'cash', ... }` en el router, solo
un `<a>` de menú sin destino y una cifra hardcodeada en el dashboard del operador.

Decisión: no se ejecuta `/plan-tareas` ni implementación todavía. No se crean specs
nuevas (014, 015, ...) hasta que el Frontend tenga cambios reales que journal (`git pull`)
muestre — ya sea una ruta de Caja real, o el primer `HttpClient`/integración real con
cualquiera de las 12 specs ya construidas. Cuando eso ocurra, se retoma desde aquí: esta
spec queda `APROBADA` y lista para `/plan-tareas` sin necesidad de reescribirla, salvo que
el Frontend real termine pidiendo algo distinto a lo aquí descrito.
