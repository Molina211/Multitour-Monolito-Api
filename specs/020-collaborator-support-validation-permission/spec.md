# 020 — Habilitación por tenant de validación de soportes por Colaborador operativo

**Estado:** APROBADA
**Fecha:** 2026-09-05
**Repos afectados:** backend
**HU relacionada:** ninguna HU formal en el backlog; se basa en el PDR
(`03-product/prd.md`, línea 115, "Restricción base").

## Problema

El PDR (línea 115, CONFIRMADO) exige que el Colaborador operativo solo pueda validar o
rechazar soportes de transferencia cuando el negocio lo habilite expresamente **para ese
tenant**, quedando siempre registrada la acción. El Frontend (`operator-role.service.ts`) ya
construyó ese toggle (`collaboratorCanValidateSupport`), hoy simulado en `localStorage`.
Spec 014 dejó esto explícitamente fuera de su alcance ("decisión abierta 2") para no
persistir un dato que ningún caso de uso leía todavía. Hoy `DecidePaymentSupportService` no
valida ningún rol: cualquier `actorId` de texto libre puede aprobar o rechazar un soporte,
sin importar si es Administrador o Colaborador.

## Alcance

- Parámetro por tenant "colaboradores pueden validar soportes de transferencia" (booleano),
  deshabilitado por defecto — vive en `Tenant`, no por colaborador individual (el PDR habla
  de habilitación "para ese tenant", igual que el Frontend lo modela como un único flag).
- Caso de uso para que un actor `ADMINISTRATOR` del tenant active o desactive el parámetro.
- Enforcement real en `DecidePaymentSupportService`: resuelve el `Membership` del `actorId`
  recibido y valida que sea `ADMINISTRATOR`, o `OPERATIONAL_COLLABORATOR` únicamente si el
  parámetro del tenant está habilitado. Si un Colaborador decide estando deshabilitado,
  rechaza con `403` y no modifica la reserva.
- Auditoría de cada cambio del parámetro (quién lo activó/desactivó y cuándo).

## Fuera de alcance

- Permisos granulares por colaborador individual: el PDR habla de habilitación por tenant,
  no de una lista de colaboradores autorizados uno por uno.
- Cualquier otro permiso de Colaborador operativo (catálogo, descuentos, devoluciones): el
  PDR línea 115 solo cubre soportes de transferencia; otros permisos (ej. catálogo, línea
  412) son specs aparte si llega a existir pantalla real que los necesite.
- Enforcement JWT real: `actorId` sigue siendo un identificador de `Membership` recibido
  explícitamente en el cuerpo del request, no extraído de un token de sesión.
- Validar rol en los demás casos de uso que hoy reciben `actorId` como texto libre sin
  verificar (cancelación, devolución, etc.): esta spec solo agrega la validación en
  decide-support; extenderla a los demás es trabajo aparte.
- Editar, desactivar o eliminar colaboradores ya registrados: spec 014 ya lo dejó fuera y
  sigue sin pantalla en el Frontend.

## Criterios de aceptación

- [ ] Con el parámetro deshabilitado (valor por defecto de un tenant nuevo), un `actorId`
      con rol `OPERATIONAL_COLLABORATOR` que intenta `decide-support` recibe `403` y la
      reserva no cambia de estado.
- [ ] Con el parámetro habilitado, el mismo Colaborador puede aprobar o rechazar el soporte,
      igual que hoy.
- [ ] Un `actorId` con rol `ADMINISTRATOR` siempre puede decidir, sin importar el valor del
      parámetro.
- [ ] Activar o desactivar el parámetro solo lo puede hacer un `actorId` con rol
      `ADMINISTRATOR` del mismo tenant; el intento con cualquier otro rol devuelve `403`.
- [ ] Cada cambio del parámetro queda auditado con actor, fecha y valor nuevo.
- [ ] Un `actorId` que no existe, o que pertenece a otro tenant, devuelve `404`/`403` en vez
      de ejecutar la decisión de soporte.
- [ ] El proyecto compila y las specs 001-019 siguen pasando.

## Impacto en multitenencia

El parámetro es un dato propio de cada `Tenant`, sin cruce entre tenants. La resolución del
`Membership` a partir de `actorId` siempre se filtra por el mismo `tenantId` de la URL —
mismo criterio de aislamiento que ya usa `Membership` (`INV-001`).

## Riesgos y decisiones abiertas

1. ¿El parámetro vive como campo directo en `Tenant`, o en un agregado nuevo de
   "configuración/parametrización de tenant"? Recomendación: campo directo en `Tenant`,
   mismo criterio de simplicidad ya usado en el resto del proyecto (no existe hoy ningún
   otro parámetro de tenant que justifique un agregado aparte). Se resuelve en
   `/plan-tareas`.
2. Esta spec agrega validación real de rol solo en `decide-support`. Los demás casos de uso
   (cancelación, devolución) siguen con `actorId` de texto libre sin verificar: ¿se deja
   esa inconsistencia documentada como deuda conocida, o se marca como bloqueante para
   specs futuras? Recomendación: documentarla como deuda conocida, sin ampliar el alcance
   de esta spec.
3. Nombre exacto del campo/parámetro (ej. `allowCollaboratorSupportValidation`) y de la
   excepción de rechazo por rol — se resuelve en `/plan-tareas`.

## Evidencia para materia

Cierra la decisión abierta 2 de spec 014 y la brecha del PDR línea 115; evidencia de
HU-IAM/permisos operativos para el corte de MVP y el Weekly.
