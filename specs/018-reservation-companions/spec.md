# 018 — Acompañantes individualizados en la reserva

**Estado:** IMPLEMENTADA
**Fecha:** 2026-09-05
**Repos afectados:** backend
**HU relacionada:** ninguna HU formal en el backlog cubre esto punto por punto; se basa en
RF-002 "Registrar acompañantes" del PDR (`PDR_Travesia_Natural.md`), sus reglas
relacionadas RN-CLI-002 y RN-RES-005, y en la pantalla ya construida en el Frontend
(`create-reservation.component.ts`), que hoy captura `holderDocument` y
`companionRecords[]` (nombre, documento, fecha de nacimiento) solo en `localStorage`, con
el comentario explícito "BACKEND API FALTANTE — ACOMPAÑANTES"
(`operator-reservation.service.ts:36-40`).

## Problema

`Reservation` hoy solo recibe `partySize` (un número) por cada `ReservedService`
(`ReservedService.java`); no existe ningún dato que identifique quién es el titular ni
quiénes son los acompañantes de una reserva. El PDR (RF-002, RN-CLI-002) exige registrar
"cero o varios acompañantes", cada uno con nombre completo, documento de identidad y
fecha de nacimiento, y prohíbe (RN-RES-005) que un documento se repita dentro de la misma
reserva. El Frontend ya construyó el formulario que captura estos datos, pero no tiene
dónde enviarlos.

## Alcance

- `Reservation` gana un documento de titular opcional (`holderDocument: String`) y una
  lista de acompañantes (`companions: List<Companion>`), donde `Companion` es un value
  object con `name`, `document` y `birthDate`. Ambos viven a nivel de la reserva completa,
  no por `ReservedService` — el formulario del Frontend solo permite elegir un servicio
  por reserva, así que los acompañantes aplican a toda la reserva.
- Los acompañantes se registran junto con la creación de la reserva (mismo endpoint
  `POST /api/tenants/{tenantId}/reservations` de spec 001), no en un endpoint aparte.
- Invariante de dominio (RN-RES-005): dentro de una misma reserva, el documento del
  titular no puede repetirse con el de ningún acompañante, ni un acompañante repetir el
  documento de otro. Se valida al crear la reserva.
- Los datos de cada acompañante ya persistidos se devuelven en las consultas existentes
  de reserva (`GET` por id, listados), sin crear un endpoint de consulta nuevo.
- Todas las validaciones nuevas devuelven `400` (mismo criterio que otros errores de
  validación del proyecto, ej. `serviceReference` vacío en `ReservedService`).

## Fuera de alcance

- Cualquier dato del titular más allá de `holderDocument`: nombre, contacto, fecha de
  nacimiento, aceptación de términos y condiciones. Eso pertenece a RF-001 "Registrar
  cliente titular", un caso de uso completo aparte que no existe hoy en el backend
  (`Customer` se identifica solo por `customerId`) y que amerita su propia spec.
- Datos adicionales para actividades de riesgo (RN-CLI-003: consentimiento informado,
  tipo de sangre, contacto de emergencia, restricciones de movilidad): quedan explícitos
  como fuera de alcance en el propio PDR para el flujo base.
- Derivar o reconciliar `partySize` a partir de la cantidad de acompañantes: `partySize`
  en `ReservedService` sigue siendo un campo independiente, sin relación calculada con
  `companions`. Una reserva puede tener varios `ReservedService`, cada uno con su propio
  `partySize`, mientras que `companions` es una sola lista a nivel de reserva; no hay una
  correspondencia 1 a 1 que permita derivar uno del otro sin inventar una regla que el
  PDR no especifica.
- Editar acompañantes de una reserva ya creada: no existe ninguna pantalla ni acción en
  el Frontend que lo necesite.
- Cualquier validación de formato de documento (longitud, tipo de documento, dígito de
  verificación): el PDR no especifica ninguna, y el Frontend tampoco la aplica hoy.

## Criterios de aceptación

- [x] Crear una reserva con `holderDocument` y una lista de `companions` (nombre,
      documento, fecha de nacimiento) sin documentos repetidos devuelve `201` y los datos
      quedan guardados y disponibles en la reserva creada.
- [x] Crear una reserva sin `holderDocument` ni `companions` (ambos opcionales) sigue
      funcionando exactamente igual que hoy (compatibilidad con specs 001-017).
- [x] Crear una reserva donde el documento de un acompañante coincide con
      `holderDocument` devuelve `400` y no crea la reserva.
- [x] Crear una reserva donde dos acompañantes comparten el mismo documento devuelve
      `400` y no crea la reserva.
- [x] Consultar una reserva ya creada (`GET` existente) incluye `holderDocument` y la
      lista de `companions` con sus tres datos cada uno.
- [x] El proyecto compila y los tests existentes (specs 001-017) siguen pasando.

## Impacto en multitenencia

No aplica un caso nuevo: `companions` y `holderDocument` viven dentro del agregado
`Reservation`, que ya filtra siempre por `tenantId`. No hay ningún dato ni cálculo que
cruce tenants.

## Riesgos y decisiones abiertas

1. **Normalización del documento para comparar duplicados**: el Frontend normaliza
   (`trim`, minúsculas, quita caracteres no alfanuméricos) antes de comparar
   (`normalizeDocument()`, `create-reservation.component.ts:108-110`). Se decide en
   `/plan-tareas` si el backend aplica la misma normalización o compara el valor exacto
   tal como llega; no cambia ningún criterio de aceptación, solo qué combinaciones cuentan
   como "mismo documento".
2. **Dónde vive la validación de duplicados**: como invariante en el constructor/factory
   de `Reservation` (mismo patrón que otras validaciones del agregado, ej.
   `reservedServices` no vacío) o como paso previo en el servicio de aplicación
   `CreateReservationService`. Se decide en `/plan-tareas`.

## Evidencia para la materia

Primera spec que modela personas dentro de la reserva más allá de un conteo (`partySize`),
cerrando la brecha documentada entre PDR (RF-002/RN-CLI-002/RN-RES-005) y la pantalla ya
construida en el Frontend; demostrable con `curl` (creación con acompañantes válidos,
rechazo por documento duplicado con el titular, rechazo por documento duplicado entre
acompañantes, consulta que devuelve los datos guardados).
