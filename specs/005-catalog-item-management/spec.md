# 005 — Gestión de ítems del catálogo operativo

**Estado:** TERMINADA
**Fecha:** 2026-09-03
**Repos afectados:** backend (requiere un cambio futuro en frontend, documentado pero
no ejecutado aquí — ver "Riesgos y decisiones abiertas")
**HU relacionada:** HU-CAT-001 (principal, FR-004). Reglas de negocio asociadas:
RN-ATR-001 (tours/atractivos), RN-ALI-001 (alimentación), RN-HOS-001/RN-HOS-003
(hospedaje). RN-TRA-001/RN-TRA-002 (transporte) quedan fuera — ver "Fuera de alcance".

## Problema

Las pantallas `operator/catalog`, `manage-catalog`, `manage-lodging`, `manage-food` y
`new-service` del Frontend simulan por completo el catálogo operativo (tours,
hospedaje, alimentación) en `localStorage`, sin ningún backend real detrás
(`OperatorCatalogService`). Sin un catálogo persistido y aislado por tenant, no hay
información base confiable para soportar reservas reales (HU-RES-004 necesita validar
capacidad de hospedaje contra un dato real, no contra un valor quemado en el Frontend).

## Alcance

- Nuevo módulo `catalog` (bounded context "Operational Catalog") dentro del monolito,
  con su propio aggregate `CatalogItem`.
- Tipos cubiertos: `TOUR` (atractivo/actividad), `LODGING` (hospedaje), `FOOD`
  (alimentación) — los tres que ya tienen pantalla de gestión en el Frontend.
- Campos del `CatalogItem`: `tenantId`, `name`, `type`, `price` (`BigDecimal`, mismo
  tipo que `Reservation.projectedValue`), `capacity` (entero, opcional — obligatorio
  solo para `LODGING` por RN-HOS-001), `restrictions` (texto libre, opcional),
  `validFrom`/`validTo` (fechas de vigencia), `policy` (texto libre), `image` (URL de
  texto, ver decisión sobre almacenamiento de imágenes), `active` (booleano).
- Endpoints, todos bajo `/api/tenants/{tenantId}/catalog-items` (mismo patrón de
  tenant-en-URL que specs 003/004):
  - `POST` — crea un ítem (`active: true` por defecto).
  - `GET` — lista los ítems del tenant (para `catalog.component.ts`, incluye activos e
    inactivos, igual que hoy hace `activeCount`/`isActive` en el Frontend).
  - `GET /{itemId}` — un ítem puntual (para las pantallas de gestión por tipo).
  - `PATCH /{itemId}` — actualiza campos editables.
  - `POST /{itemId}/deactivate` — desactiva (soft, nunca se borra la fila —
    HU-CAT-001 escenario 1, mantiene trazabilidad histórica).
  - `POST /{itemId}/reactivate` — vuelve a activar.
- Validación de capacidad obligatoria y positiva cuando `type = LODGING` (RN-HOS-003),
  rechazada si falta o es ≤ 0.
- Rechaza operaciones sobre un tenant inexistente o `Inactivo` (mismo criterio que
  spec 003 para registro de clientes).

## Fuera de alcance

- `TRA` (Transporte, RN-TRA-001/002): el propio Frontend excluye su pantalla de gestión
  de este bloque (`operator-catalog.service.ts`, comentario "Transporte queda fuera: su
  pantalla de gestión no forma parte de este bloque"). No se inventa un endpoint para
  algo que ni el Frontend ni esta spec necesitan todavía.
- Costos operacionales (segunda mitad de RN-ATR-001: "cada atractivo debe tener
  definidos valor comercial **y costos operacionales**"). El formulario real de
  creación (`new-service.component.ts`) solo captura `price` (valor comercial), nunca
  un costo operacional — y ese dato pertenece a HU-COST-001 ("Registrar costos
  operacionales"), una historia propia y no implementada. Se documenta el vacío, no se
  inventa el campo.
- Restricción de permisos por rol dentro de esta spec (HU-CAT-001 escenario 2: un
  Operational Collaborator no debería poder tocar tarifa/capacidad, solo campos
  descriptivos). Ver "Riesgos y decisiones abiertas" — no existe hoy ningún mecanismo
  para saber qué rol tiene quien llama a un endpoint sin sesión real (no hay login de
  staff, spec 004 lo dejó fuera de alcance explícitamente).
- Subida real de archivos de imagen: el campo `image` es una URL de texto. La pantalla
  actual solo hace un preview local en base64 (`FileReader`) sin subir nada a ningún
  servidor; construir almacenamiento de archivos es una decisión de infraestructura
  aparte, no necesaria para que el catálogo funcione.
- Vincular `CatalogItem` con `Reservation`/`ReservedService` (validar capacidad real al
  crear una reserva, HU-RES-004) — spec futura, esta solo construye el catálogo base.
- Consulta pública del catálogo desde el canal digital del cliente final (HU-RES-005) —
  spec futura, distinta pantalla (`home`/pública, no `operator/*`).

## Criterios de aceptación

- [x] `POST /api/tenants/{tenantId}/catalog-items` con datos válidos de un tenant
      `Activo` devuelve `201` con el ítem creado, `active: true`.
- [x] Crear un `LODGING` sin `capacity` o con `capacity <= 0` devuelve `400` sin
      persistir nada (RN-HOS-003).
- [x] `GET /api/tenants/{tenantId}/catalog-items` devuelve todos los ítems del tenant
      (activos e inactivos), nunca ítems de otro tenant.
- [x] `GET /api/tenants/{tenantId}/catalog-items/{itemId}` de un ítem de otro tenant
      devuelve `404` (aislamiento, no filtra existencia cruzada).
- [x] `PATCH /api/tenants/{tenantId}/catalog-items/{itemId}` actualiza los campos
      enviados y conserva los no enviados.
- [x] `POST .../{itemId}/deactivate` pone `active: false` sin borrar la fila; el ítem
      sigue siendo consultable por `GET /{itemId}` (trazabilidad histórica).
- [x] `POST .../{itemId}/reactivate` vuelve a poner `active: true`.
- [x] Operar sobre un `tenantId` inexistente devuelve `404`; sobre un tenant `Inactivo`
      devuelve `409`.
- [x] El proyecto compila y los tests existentes (spec 001-004) siguen pasando.

## Impacto en multitenencia

Cada `CatalogItem` lleva `tenant_id` como columna obligatoria, igual que
`Reservation`/`Membership`. El aislamiento se verifica igual que en specs anteriores:
un ítem de un tenant nunca es visible ni editable desde otro (`GET`/`PATCH` por
`itemId` siempre filtran también por `tenantId` de la URL).

## Riesgos y decisiones abiertas

1. **HU-CAT-001 escenario 2 (restricción de permisos por rol) no se implementa en este
   corte.** Todo `actorId` que ya existe en otras partes del backend (spec 002) es un
   string libre usado solo para auditoría, nunca se valida contra ninguna membership —
   no hay precedente de resolver un rol real a partir de él, y el Frontend
   (`new-service.component.ts`) tampoco envía ningún dato de actor hoy. Implementar la
   restricción de rol de verdad necesita primero un mecanismo de sesión/actor
   verificable (bloqueado en login de staff, fuera de alcance desde spec 004). Se deja
   documentado aquí para la próxima vez que se retome login de staff.
2. **El Frontend no llama a ningún backend real todavía para el catálogo** — igual que
   pasó con login/registro en specs 003/004, esta spec construye el backend pero la
   integración real (reemplazar `OperatorCatalogService`/`localStorage` por HTTP) queda
   pendiente para quien conecte ambos repos.
3. **Campo `image` como URL de texto, no archivo subido**: decisión deliberada para no
   abrir la puerta de almacenamiento de archivos sin necesidad — se revisita si alguna
   spec futura sí lo requiere de verdad.

## Evidencia para la materia

Cierra FR-004/HU-CAT-001 (parcial, sin escenario de permisos por rol), primera
funcionalidad real del bounded context "Operational Catalog" y prerequisito de datos
reales para HU-RES-004 (validación de capacidad) en una spec futura de reservas.
