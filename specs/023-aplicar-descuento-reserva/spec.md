# 023 — Aplicar descuento adicional a una reserva

**Estado:** TERMINADA
**Fecha:** 2026-09-05
**Repos afectados:** backend
**HU relacionada:** ninguna HU formal en el backlog cubre esto punto por punto; recoge
RF-008/RF-005B del PDR ("Aplicar descuento adicional") y cierra el hueco que la propia
spec 008 dejó anotado explícitamente: "Aplicar el descuento al valor final de una
reserva ... tocaría `reservations` por cuarta vez ... queda fuera de alcance". El
Frontend ya construyó la pantalla `apply-discount` (formulario porcentaje + motivo,
botón que ejecuta `applyAdditionalDiscount(...)`), que en su momento no existía cuando
se escribió spec 008.

## Problema

`Reservation.finalValue` hoy solo se fija al crearse (spec 001) o cambia por una
modificación (spec 022) o una cancelación (spec 011); no existe ningún caso de uso que
aplique un descuento puntual y manual sobre una reserva ya creada. `Discount` (spec 008)
gestiona reglas de descuento asociadas a un `CatalogItem` del catálogo, pero nunca se
conecta con una reserva concreta — este descuento adicional es distinto: es una
autorización manual, puntual, sobre una reserva ya existente, sin relación con el
catálogo de reglas.

## Alcance

- Nuevo caso de uso "aplicar descuento adicional": recibe `percentage` (entero, 1-100),
  `reason` (obligatorio) y un actor. Permitido solo cuando `reservationStatus` es
  `PENDIENTE_DE_PAGO` o `CONFIRMADA` (misma precondición que
  `apply-discount.component.ts:29`, `isEligibleForAdditionalDiscount`).
- Recalcula `newFinalValue = finalValue * (1 - percentage / 100)`, aplicado sobre el
  `finalValue` ACTUAL de la reserva (no sobre un "valor original" separado — a
  diferencia del Frontend, que recalcula siempre sobre `OPERATOR_RESERVATIONS` porque ahí
  vive como dato de demostración estático; en el Backend `finalValue` ya es el único
  valor vigente de la reserva).
- Reutiliza la misma fórmula de recalculo de saldo que spec 011/022:
  `amountAlreadyPaid = finalValue - pendingBalance` (con los valores ANTERIORES a
  aplicar el descuento). Si `newFinalValue < amountAlreadyPaid`, el excedente se
  registra como `creditBalance` con `paymentStatus = SALDO_A_FAVOR_PENDIENTE` y
  `refundDecisionStatus = PENDIENTE_AUTORIZACION` (mismo mecanismo ya usado por
  cancelación y modificación). Si no, `pendingBalance = newFinalValue -
  amountAlreadyPaid`.
- Persiste `percentage`, `reason` y el actor de cada aplicación para trazabilidad (mismo
  criterio de auditoría ya usado por `cancellationReason`/`cancelledBy` en spec 011).
- Nuevo endpoint `POST /api/tenants/{tenantId}/reservations/{reservationId}/apply-discount`.
- Validaciones nuevas (`percentage` fuera de 1-100, `reason` vacío) devuelven `400`,
  mismo criterio que el resto del proyecto.

## Fuera de alcance

- Cualquier vínculo con el catálogo de reglas `Discount` (spec 008): este descuento es
  manual y puntual, no referencia ningún `discountId` ni valida contra `catalogItemId`.
- Reglas de acumulación/tope/prioridad entre descuentos (RF-005B: `priority`,
  `stackable`, `cap`, `base`): spec 008 ya las dejó sin motor de cálculo; esta spec
  tampoco lo construye.
- **Bloquear una segunda aplicación de descuento adicional sobre la misma reserva**: el
  propio Frontend no tiene ningún estado que lo impida (`ineligible` en
  `apply-discount.component.ts:29` solo depende del `statusClass`, no de si ya existe un
  descuento previo) — no se inventa una restricción que el Frontend no exige. Si se
  aplica dos veces, la segunda se calcula sobre el `finalValue` ya descontado por la
  primera (ver decisión abierta 1).
- Restricción real de que "solo el Administrador puede autorizar" (texto fijo de
  `apply-discount.component.ts:40`): no existe todavía un mecanismo de sesión/rol
  resuelto para este endpoint — mismo criterio de spec 008/011 (`permitAll()`), y JWT
  (spec 007) solo cubre `POST /reservations` hoy, ningún otro endpoint de escritura.
- Conectar el Frontend a este endpoint: `OperatorReservationService` sigue en
  `localStorage`; integración diferida hasta que ambos módulos estén completos.

## Criterios de aceptación

- [x] Aplicar un descuento del 10% a una reserva `Pendiente de pago` sin pagos
      registrados recalcula `finalValue` y deja `pendingBalance = finalValue`, `200`.
- [x] Aplicar un descuento a una reserva `Confirmada` (saldo en 0) cuyo nuevo
      `finalValue` queda por debajo de lo ya pagado deja el excedente como
      `creditBalance`, `paymentStatus = SALDO_A_FAVOR_PENDIENTE`,
      `refundDecisionStatus = PENDIENTE_AUTORIZACION`.
- [x] `percentage` fuera de 1-100, o `reason` vacío, devuelve `400`.
- [x] Aplicar sobre una reserva `EnEjecucion`, `Finalizada` o `Cancelada` devuelve `409`.
- [x] Cualquier operación sobre un `tenantId` inexistente devuelve `404`; sobre uno
      `Inactivo`, `409`.
- [x] El proyecto compila y los tests existentes (specs 001-022) siguen pasando.

## Impacto en multitenencia

Mismo patrón que las specs anteriores: filtra siempre por `tenantId` de la URL además de
`reservationId`; no hay ningún dato ni cálculo que cruce tenants.

## Riesgos y decisiones abiertas

1. **¿Se permite aplicar más de un descuento adicional sobre la misma reserva?** El
   Frontend no lo bloquea (ver "Fuera de alcance"). Recomendación: permitirlo, cada
   aplicación se calcula sobre el `finalValue` vigente y queda su propio registro de
   auditoría (percentage/reason/actor/fecha) — se confirma en `/plan-tareas`.
2. **Dónde vive el registro de auditoría del descuento**: ¿campos directos en
   `Reservation` (como `cancellationReason`) o una lista aparte tipo `companions`, dado
   que puede haber más de una aplicación? Se decide en `/plan-tareas`, no cambia ningún
   criterio de aceptación.

## Evidencia para la materia

Cierra explícitamente el hueco documentado en spec 008 ("Fuera de alcance" #1) ahora que
la pantalla que lo necesita ya existe en el Frontend. Reutiliza el mismo mecanismo de
saldo a favor ya construido en spec 011/019 y extendido en spec 022, demostrando que ese
diseño sirve para cualquier operación que reduzca `finalValue` por debajo de lo pagado,
no solo cancelación. Demostrable con `curl` (aplicar sin pago previo, aplicar generando
saldo a favor), mismo patrón que las specs anteriores.
