# 024 — Transporte asociado a una reserva

**Estado:** TERMINADA
**Fecha:** 2026-09-05
**Repos afectados:** backend
**HU relacionada:** RN-TRA-001/RN-TRA-002 del PDR (misma base que spec 015). Spec 015
dejó anotado explícitamente en "Fuera de alcance": "Recalcular el valor de transporte al
modificar una reserva ... depende de una reserva ya creada y de una capacidad de
modificación que no existe hoy en `reservations`" — spec 022 ya cierra la parte de
modificación; esta spec cierra la parte de creación.

## Problema

`ReservedService` (agregado `Reservation`) solo tiene `serviceReference`, `partySize` y
`scheduledDate` — ningún campo referencia qué transporte se usó ni cuánto costó. El
Frontend (`create-reservation.component.ts`) ya resuelve en vivo, para cada reserva, un
transporte real (asociado al Tour o elegido manualmente del catálogo activo de
`TRANSPORT`, spec 015), suma su costo por persona al valor proyectado
(`create-reservation.component.ts:259-261`, `transportCost = transportOption.price *
travelers`) y guarda una descripción de texto (`transportSelected`) junto al resto de
datos de la reserva — pero hoy solo en `localStorage`, porque `POST
/api/tenants/{tenantId}/reservations` (spec 001) no tiene ningún campo para recibirlo.

## Alcance

- `ReservedService` gana dos campos opcionales: `transportItemId` (referencia a un
  `CatalogItem` de `type: TRANSPORT`, spec 015) y `transportCost` (`BigDecimal`, el costo
  ya calculado — precio del transporte × cantidad de viajeros, mismo cálculo que ya hace
  el Frontend, no se recalcula server-side con reglas nuevas).
  Ambos `null` cuando la reserva no lleva transporte (mismo criterio que "Tour sin
  transporte asociado" del Frontend).
- `POST /api/tenants/{tenantId}/reservations` (spec 001, extendido por spec 018) acepta
  estos dos campos nuevos, opcionales, dentro de cada `ReservedService` del body.
- Si `transportItemId` viene informado, se valida que exista, pertenezca al mismo
  `tenantId` del path, sea `type: TRANSPORT` y esté `active` (mismo patrón de validación
  cruzada ya usado en spec 008 para `catalogItemId` de `Discount` contra el tenant del
  path). Si no cumple, `400`.
- Los datos de transporte ya persistidos se devuelven en las consultas existentes de
  reserva (`GET` por id, listados de spec 006), sin crear un endpoint de consulta nuevo
  — mismo criterio ya usado por spec 018 para `companions`.
- Migración Flyway que agregue las dos columnas nuevas, nullable, a la tabla de
  servicios reservados, sin afectar las filas ya existentes.

## Fuera de alcance

- Vínculo Tour → Transporte con tarifa fija por tour (RN-TRA-002, primera mitad): spec
  015 ya lo dejó fuera de alcance como una relación entre dos ítems de catálogo, no un
  atributo de `Reservation`; sigue fuera aquí también. Esta spec solo registra QUÉ
  transporte y QUÉ costo quedaron asociados a una reserva ya creada, sin importar si ese
  transporte vino de un vínculo Tour-Transporte o de una elección manual — el Frontend ya
  resuelve esa distinción antes de enviar el dato (`serviceIncludesTransport`,
  `create-reservation.component.ts:213`).
- Recalcular `transportCost` al modificar una reserva (spec 022): esta spec solo cubre
  creación; la modificación reemplaza `reservedServices` completo (incluyendo transporte)
  como cualquier otro campo, sin una regla especial de recalculo adicional.
- Validar que la capacidad del transporte cubra la cantidad de viajeros
  (`transportOverCapacity`, `create-reservation.component.ts:327`): es una validación de
  formulario ya hecha en el Frontend contra `TransportCatalogOption.capacity`; el Backend
  no repite esa regla aquí (mismo criterio que la validación de capacidad de hospedaje,
  ya fuera de alcance desde spec 001, HU-RES-004).
- Endpoint de consulta o reporte de "transporte más usado"/costos operativos agregados:
  no lo pide ninguna pantalla existente.
- Conectar el Frontend a este endpoint: `OperatorReservationService`/
  `create-reservation.component.ts` siguen en `localStorage`/`sessionStorage`;
  integración diferida hasta que ambos módulos estén completos.

## Criterios de aceptación

- [x] Crear una reserva con un `ReservedService` que incluye `transportItemId` (de un
      `CatalogItem` `TRANSPORT` activo del mismo tenant) y `transportCost` devuelve
      `201` y ambos campos quedan persistidos y visibles en el `GET` de esa reserva.
- [x] Crear una reserva sin `transportItemId`/`transportCost` sigue funcionando igual
      que hoy (`201`, ambos campos `null`).
- [x] Crear una reserva con `transportItemId` que no existe, pertenece a otro tenant, no
      es `type: TRANSPORT`, o está inactivo, devuelve `400`.
- [x] Los criterios ya cubiertos en spec 001/006/018 (aislamiento por tenant, tenant
      inexistente `404`, tenant `Inactivo` `409`) se confirman también con reservas que
      incluyen transporte — con una salvedad ya conocida desde spec 007: el `404` de
      "tenant inexistente" no es alcanzable en `POST /reservations` porque el JWT
      resuelve `403 tenant_mismatch` antes (mismo hueco documentado en la sección "007"
      de `PLAN-VERIFICACION.md`, no introducido por esta spec); `tenant_inactive` `409`
      sí se confirmó.
- [x] El proyecto compila y los tests existentes (specs 001-023) siguen pasando.

## Impacto en multitenencia

`transportItemId` se valida contra el mismo `tenantId` del path (mismo criterio de
aislamiento cruzado ya aplicado en spec 008 para `Discount.catalogItemId` y en spec 015
para `CatalogItem`); ningún dato de transporte cruza tenants.

## Riesgos y decisiones abiertas

1. **`transportCost` como dato de entrada ya calculado (como `projectedValue` en spec
   001) vs. que el Backend lo recalcule a partir de `CatalogItem.price × partySize`**:
   recomendación, recalcularlo server-side a partir del `price` del `CatalogItem`
   referenciado (evita que el cliente HTTP envíe un costo arbitrario que no corresponda
   al transporte elegido) — se confirma en `/plan-tareas`.
2. **Nombre del value object**: extender `ReservedService` (record actual, spec 001) con
   los dos campos nuevos, vs. un value object `TransportSelection` aparte embebido. Se
   decide en `/plan-tareas`, no cambia ningún criterio de aceptación.

## Evidencia para la materia

Cierra el tercero de los huecos identificados el 2026-09-05 al revisar `create-reservation
.component.ts` contra las specs de Backend ya implementadas; mismo patrón ya usado por
spec 018 (acompañantes) para extender el `POST` de creación de reserva con un dato que el
Frontend ya captura y el Backend todavía no recibe. Demostrable con `curl` (crear reserva
con transporte válido, sin transporte, con transporte inválido).
