# 019 — Ciclo de vida completo de solicitud de devolución (autorización y ejecución separadas)

**Estado:** APROBADA
**Fecha:** 2026-09-05
**Repos afectados:** backend
**HU relacionada:** ninguna HU formal en el backlog; se basa en RF-015B/RN-RES-008/RN-CAJ-001
del PRD (`03-product/prd.md`, líneas 579-588 y 705-708).

## Problema

Spec 012 modeló la devolución como un solo paso (`actorId` + monto + método, ejecutado de
una vez) porque en ese momento el Frontend no tenía ningún flujo real construido — la propia
spec 012 lo dejó anotado como decisión a revisitar si el Frontend construía un flujo real de
solicitud→aprobación. Hoy ese flujo ya existe: `operator-refund.service.ts` implementa el
ciclo completo que exige RN-RES-008 — solicitud, autorización exclusiva del Administrador,
rechazo, ejecución con salida de dinero (solo si hay autorización previa trazable) o registro
como saldo a favor pendiente cuando no hay salida efectiva. El Backend no distingue estos
pasos ni valida quién puede autorizar.

## Alcance

- Modelar la solicitud de devolución con los estados que ya usa el Frontend y que coinciden
  con RN-RES-008: `Pendiente de autorización`, `Autorizada`, `Rechazada`, `Ejecutada`,
  `Saldo a favor pendiente`.
- Caso de uso "crear solicitud de devolución" sobre una reserva con causal ya registrada
  (cancelación existente), con motivo y monto — el monto sigue siendo un dato de entrada
  (ver Fuera de alcance), no calculado por el sistema.
- Caso de uso "autorizar" — solo un actor con rol `ADMINISTRATOR` del tenant puede
  ejecutarlo; requiere nota de autorización.
- Caso de uso "rechazar" — solo `ADMINISTRATOR`; motivo obligatorio; no ejecuta ninguna
  salida de dinero.
- Caso de uso "ejecutar con salida de dinero" — exige autorización previa trazable
  (RN-RES-008 permite que la ejecute `ADMINISTRATOR` u `OPERATIONAL_COLLABORATOR`, nunca sin
  autorización previa); registra método de salida y referencia de caja.
- Caso de uso "registrar como saldo a favor pendiente" cuando no hay salida efectiva de
  dinero.
- Toda transición queda auditada (actor, fecha, motivo), mismo patrón ya usado en el resto
  del proyecto.
- Consulta de la solicitud de devolución asociada a una reserva.

## Fuera de alcance

- Cálculo automático del "valor potencial a devolver" según reglas comerciales
  parametrizadas (RN-RES-004): no existe motor de reglas comerciales; el monto sigue siendo
  un dato de entrada manual, igual que en spec 012.
- Generación automática de un movimiento de caja real (`cash` module) al ejecutar: esta spec
  solo guarda una referencia/texto del movimiento, no crea un `CashMovement` vinculado — se
  revisa en `/plan-tareas` si conviene enlazarlo según el estado actual de `cash`.
- Enforcement JWT real: sigue sin token. La validación de "solo Administrador autoriza" se
  hace consultando el rol del `Membership` correspondiente al `actorId` recibido, sin
  extraerlo de un token.
- Reagendamiento (RF-015C) y aplicación de saldo a favor a una reserva futura vinculada
  (RN-RES-009): fuera de alcance, mismo criterio que spec 012.
- Migración de devoluciones ya ejecutadas bajo el modelo de spec 012 a este nuevo modelo: no
  hay datos reales en producción que migrar.

## Criterios de aceptación

- [ ] Crear una solicitud de devolución sobre una reserva con causal registrada la deja en
      `Pendiente de autorización`, con motivo y monto guardados.
- [ ] Autorizar una solicitud pendiente con un actor `ADMINISTRATOR` del mismo tenant la
      deja en `Autorizada`, con nota y actor registrados. Con un actor que no sea
      `ADMINISTRATOR`, devuelve `403` y no la modifica.
- [ ] Rechazar una solicitud pendiente con un actor `ADMINISTRATOR` la deja en `Rechazada`,
      con motivo obligatorio, y no genera ninguna salida de dinero.
- [ ] Ejecutar con salida de dinero una solicitud `Autorizada` la deja en `Ejecutada`, con
      método y referencia de caja registrados. Ejecutar una solicitud que no está
      `Autorizada` devuelve `409`.
- [ ] Registrar una solicitud `Autorizada` como saldo a favor pendiente (sin salida
      efectiva) la deja en `Saldo a favor pendiente`, nunca en `Ejecutada`.
- [ ] Consultar la solicitud de devolución de una reserva devuelve su estado actual junto
      con el historial de motivo/actor/fecha de cada transición.
- [ ] Toda operación sobre un `tenantId` inexistente devuelve `404`; sobre uno `Inactivo`,
      `409`.
- [ ] El proyecto compila y las specs 001-018 siguen pasando.

## Impacto en multitenencia

Mismo patrón que el resto del proyecto: toda operación filtra por `tenantId` de la URL
además del identificador de la solicitud/reserva; la validación de rol del actor también se
resuelve dentro del mismo tenant (`Membership` no cruza tenants).

## Riesgos y decisiones abiertas

1. ¿Esta spec reemplaza el endpoint de un solo paso de spec 012, o convive como una acción
   "directa" para cuando el propio Administrador ejecuta sin pasar por solicitud previa
   (RN-RES-008 lo permite)? Se decide en `/plan-tareas`.
2. ¿La solicitud de devolución es un sub-objeto de `Reservation` (mismo patrón que
   motivo/actor de cancelación hoy) o una entidad independiente que permite varias
   solicitudes históricas por reserva? Se decide en `/plan-tareas`.
3. ¿Cómo se valida "actor con rol Administrador" sin JWT? Recomendación: `actorId` se recibe
   como identificador de `Membership` y se consulta contra `MembershipRepositoryPort`
   (ya existe), en vez de seguir como texto libre sin verificar.
4. ¿Se enlaza con un movimiento real de `cash` (módulo ya implementado en spec 013) o queda
   solo como referencia de texto? Depende de qué tan reutilizable sea el modelo actual de
   `cash` — se revisa en `/plan-tareas`.

## Evidencia para materia

Cierra RF-015B/RN-RES-008 de forma completa (las tres decisiones funcionales separadas que
exige la regla, no solo un paso); evidencia de HU de devoluciones para el corte de MVP y el
Weekly.
