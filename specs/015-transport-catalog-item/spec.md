# 015 — Tipo de catálogo Transporte

**Estado:** TERMINADA
**Fecha:** 2026-09-04
**Repos afectados:** backend
**HU relacionada:** HU-CAT-001 (extensión de spec 005). PDR: TRA-003, RN-TRA-001,
RN-TRA-002 (parcial — ver "Fuera de alcance").

## Problema

Spec 005 excluyó explícitamente `TRANSPORT` de la gestión de catálogo porque, en ese
momento, "el propio Frontend excluye su pantalla de gestión de este bloque"
(`CatalogItem.java`, comentario de clase). Eso cambió: el commit `b0bf23e` (Fernanda
Robayo, 2026-09-04, rama `develop`) agregó pantallas reales `configure-transport` y
`manage-transport`, con un cuarto tipo de recurso de catálogo (`route`, `capacity`,
`tariff`, `cost`, `validity`, `policy`) que el Backend no soporta —
`CatalogItemType` solo tiene `TOUR`, `LODGING`, `FOOD`.

## Alcance

- Agregar `TRANSPORT` a `CatalogItemType`.
- Agregar dos campos nuevos al agregado `CatalogItem`, ambos opcionales:
  - `route` (texto libre — RN-TRA-001: "el transporte... se maneja por trayectos").
  - `operationalCost` (`BigDecimal` — costo interno del transporte, nunca lo que paga
    el cliente; distinto de `price`, que sigue siendo la tarifa comercial).
- Reutilizar el mismo CRUD-lite ya existente de spec 005 (`POST` / `GET` / `GET
  {itemId}` / `PATCH` / `deactivate` / `reactivate`) para `type: TRANSPORT`, sin
  endpoints nuevos.
- Migración Flyway que agregue las dos columnas nuevas a `catalog_items`, nullable,
  sin afectar las filas ya existentes de `TOUR`/`LODGING`/`FOOD`.

## Fuera de alcance

- Vínculo Tour → Transporte con tarifa específica por tour (RN-TRA-002: "cada tour
  puede tener una tarifa fija propia por persona para el trayecto correspondiente").
  El Frontend ya lo simula (`TourTransportLink`, clave `multitour-tour-transport-links`),
  pero es una **relación entre dos ítems del catálogo**, no un atributo de un ítem
  individual — funcionalidad distinta y más compleja, spec futura si se decide
  construirla.
- Recalcular el valor de transporte al modificar una reserva (segunda mitad de
  RN-TRA-002) — depende de una reserva ya creada y de una capacidad de modificación
  que no existe hoy en `reservations`.
- Validación obligatoria de `route` u `operationalCost`: el propio Frontend permite
  guardarlos vacíos ("Por configurar" como placeholder, nunca un error de validación
  al guardar) — mismo criterio ya usado para `restrictions`/`image`, ambos opcionales
  en spec 005. No se inventa una regla que ni el Frontend exige.
- Restricción de permisos por rol sobre catálogo — ya documentada como fuera de
  alcance en spec 005 (decisión abierta 1): sigue sin existir un mecanismo de sesión
  real para resolver el rol de quien llama.
- Conectar el Frontend a este endpoint (`OperatorCatalogService` sigue en
  `localStorage`) — integración diferida hasta que ambos módulos estén completos.

## Criterios de aceptación

- [ ] Dado un tenant `Activo`, cuando se crea un ítem `type: TRANSPORT` con `name` y
      `price`, sin `capacity`, `route` ni `operationalCost`, entonces se crea con
      `201` (ninguno de esos tres es obligatorio para `TRANSPORT`).
- [ ] Dado un ítem `TRANSPORT` creado con `route` y `operationalCost`, cuando se
      consulta por `GET /{itemId}`, entonces ambos valores se devuelven tal cual se
      guardaron.
- [ ] Dado un ítem `TRANSPORT`, cuando se actualiza solo `route` vía `PATCH`,
      entonces el resto de los campos (incluido `operationalCost`) no cambia.
- [ ] La validación de `capacity` obligatoria y positiva sigue aplicando únicamente a
      `LODGING` (RN-HOS-003) — crear un `TRANSPORT` sin `capacity` nunca devuelve
      `400` por ese motivo.
- [ ] Los criterios ya cubiertos por spec 005 (aislamiento por tenant, soft
      delete/reactivate, tenant inexistente `404`, tenant `Inactivo` `409`) se
      confirman también para `type: TRANSPORT`, sin necesitar un caso nuevo por
      criterio — se ejecutan como parte del plan de verificación.
- [ ] El proyecto compila y los tests existentes (spec 001-014) siguen pasando.

## Impacto en multitenencia

No aplica cambio adicional. `TRANSPORT` es un valor más de un campo (`type`) ya
sujeto a `tenant_id` obligatorio — mismo aislamiento ya verificado en spec 005 para
`TOUR`/`LODGING`/`FOOD`.

## Riesgos y decisiones abiertas

1. Nombre del campo interno: `operationalCost` (explícito, evita confundirlo con el
   "costo operacional" general que spec 005 dejó fuera para HU-COST-001) vs. `cost`
   a secas (igual que el campo del Frontend). No cambia el problema ni los criterios
   de aceptación — se resuelve en `/plan-tareas`.
2. Migración Flyway: columnas nullable directamente en `catalog_items` (mismo patrón
   ya usado para `capacity`, opcional salvo `LODGING`) vs. una tabla de extensión
   propia para transporte. Recomendación: columnas nullable, por simplicidad — se
   confirma en `/plan-tareas`.

## Evidencia para la materia

Cierra el segundo de dos huecos confirmados el 2026-09-04 al revisar el commit más
reciente del Frontend (`b0bf23e`) contra las specs de Backend ya implementadas.
Extiende HU-CAT-001/TRA-003 del PDR a los cuatro tipos de catálogo mencionados en
RN-TRA-001, dejando documentado (no implementado) el vínculo Tour-Transporte de
RN-TRA-002 para una spec futura.
