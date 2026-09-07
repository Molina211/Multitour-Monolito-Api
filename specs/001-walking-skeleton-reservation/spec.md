# 001 — Walking Skeleton del Backend + Reservation persistida

**Estado:** TERMINADA (reconciliada 2026-09-03: sus criterios originales quedaron
superados por la spec 006 — ver nota antes de "Criterios de aceptación")
**Fecha:** 2026-09-02
**Repos afectados:** backend
**HU relacionada:** HU-RES-001 (principal); HU-RES-002, HU-RES-003, HU-RES-004, HU-RES-008, HU-RES-009 quedan fuera de este corte pero definen la forma futura del agregado (ver "Fuera de alcance")

## Problema

Hoy el repo Backend es solo el esqueleto de Spring Initializr: una clase de arranque y su
test por defecto, sin entidades, controladores, repositorios ni base de datos (`CLAUDE.md`
§5). No existe ningún corte vertical end-to-end que pruebe que la arquitectura elegida
(monolito modular Java, ADR-002) y la estrategia de aislamiento de tenant (schema
compartido + discriminador `tenantId`, ADR-003) realmente funcionan de punta a punta:
API expuesta → dominio → persistencia real contra un motor de base de datos, en lugar de
solo en el papel.

## Alcance

- Layout hexagonal (puertos y adaptadores) como estructura de paquetes base del monolito,
  organizada por bounded context (empezando por `reservations`), no por capa técnica
  transversal a todo el proyecto.
- Composition root de Spring Boot que arranca el contenedor con esa estructura.
- Endpoint `GET /health` (adaptador de entrada HTTP) que confirma que la aplicación
  levanta y responde.
- `docker-compose.yml` con un contenedor PostgreSQL para desarrollo local.
- Primera entidad real del dominio persistida: `Reservation` (agregado, contexto
  Reservations), con exactamente los atributos que `02-domain/entities-and-rules.md`
  marca como obligatorios para el agregado y los campos que `06-data/models.md` define
  para el registro lógico `reservations`:
  `reservationId`, `tenantId`, `customerId`, `projectedValue`, `finalValue`,
  `pendingBalance`, `creditBalance`, `reservationStatus`, `paymentStatus`,
  `paymentMethod`, `createdAt`.
- `tenantId` como columna obligatoria desde el primer commit que toque esta entidad,
  por ADR-003 — no se agrega después.
- Puerto de salida (repositorio) y adaptador de persistencia (JPA/Postgres) para
  `Reservation`, cubriendo únicamente la operación necesaria para el criterio de
  aceptación de HU-RES-001 Escenario 1: crear una reserva con datos completos y que
  quede persistida con su `projectedValue` calculado.
- Caso de uso de aplicación "crear reserva" que orquesta la validación mínima de
  HU-RES-001 Escenario 2 (datos obligatorios faltantes) y delega en el puerto de salida.
- `PLAN-VERIFICACION.md` con los pasos para levantar el contenedor, arrancar la app y
  comprobar `/health` y la creación de una reserva.

## Fuera de alcance

- Autenticación/autorización (JWT o cualquier otro mecanismo) — capa transversal con
  spec propia, según instrucción explícita.
- Cálculo real de `projectedValue` con reglas de descuento o transporte por persona
  (HU-RES-003) — en este corte el valor proyectado se acepta como dato de entrada ya
  calculado por quien crea la reserva, no se recalcula con reglas de negocio.
- Validación de capacidad de alojamiento (HU-RES-004).
- Modificación (HU-RES-002), consulta de ejecución (HU-RES-008) y reprogramación
  (HU-RES-009) de una reserva ya creada.
- `companions`, `reservedServices` como entidades propias persistidas (registros lógicos
  `reservation_companions`, `reserved_services` de `06-data/models.md`) — quedan
  modeladas como el mínimo necesario para que `Reservation` tenga "al menos un servicio
  reservado" (regla del agregado), no como agregados/tablas completos con su propio
  ciclo de vida.
- Canal digital de autoservicio (HU-RES-005, HU-RES-006, HU-RES-007) — dependen de
  Identity and Access y Operational Catalog, fuera de este corte.
- Cualquier otro bounded context (Customers, Operational Catalog, Discounts, etc.):
  se referencian solo por id (`customerId`), sin validarlos contra un servicio real.
- MongoDB (RC-001): esta spec solo cubre PostgreSQL; el motor Mongo se resuelve cuando
  una spec necesite un contexto documental (p. ej. Reports and Dashboard).
- CI/CD, despliegue a AWS, y cualquier configuración de GitHub (regla 6).

## Nota de reconciliación (2026-09-03)

Esta spec nunca se marcó `TERMINADA` en su momento, y sus criterios describen un
contrato que ya no existe: `tenantId` como `UUID` recibido por header `X-Tenant-Id`, y
`POST /api/reservations` sin tenant en la URL. La spec 006 (`reservation-query`)
reemplazó ambas cosas explícitamente (tenantId pasa a `String`/slug en la URL, la ruta
se reubica a `POST /api/tenants/{tenantId}/reservations`) y sus propios criterios de
aceptación ya verifican el walking skeleton bajo el contrato nuevo. Los checkboxes de
abajo se marcan como cumplidos en su momento bajo el contrato original (evidencia: specs
006 y 007 construyen sobre este walking skeleton y sus tests pasan), no porque el
contrato original siga vigente hoy — para el contrato vigente, ver `spec.md` de 006.

## Criterios de aceptación

- [x] Con `docker-compose up`, un contenedor PostgreSQL queda disponible y accesible
      desde la aplicación Spring Boot.
- [x] `GET /health` responde `200 OK` con la app corriendo contra ese contenedor.
- [x] `POST` de creación de reserva con `tenantId`, `customerId`, `projectedValue` y al
      menos un servicio reservado (dato mínimo) devuelve la reserva creada con
      `reservationId` generado y `reservationStatus = Pendiente de pago`, y el registro
      queda verificable directamente en la tabla `reservations` de Postgres.
      (Contrato original vía header `X-Tenant-Id`; reemplazado por spec 006.)
- [x] La misma operación sin `tenantId`, sin `customerId` o sin ningún servicio
      reservado es rechazada sin persistir nada (HU-RES-001 Escenario 2).
- [x] La tabla `reservations` tiene `tenant_id` como columna `NOT NULL` desde su primera
      migración — no existe una versión de la tabla sin esa columna en el historial de
      migraciones.
- [x] Dos reservas con el mismo `customerId` pero distinto `tenantId` quedan como filas
      independientes sin ninguna relación cruzada (prueba manual o automatizada mínima
      de que el filtro por `tenantId` está presente en la consulta).
- [x] El proyecto compila y el test por defecto de Spring Initializr sigue pasando.

## Impacto en multitenencia

Este corte es, en la práctica, la primera vez que ADR-003 se implementa como código en
lugar de quedar solo documentado. `Reservation` lleva `tenantId` obligatorio desde su
primera migración de base de datos; el repositorio/adaptador de persistencia debe recibir
y aplicar el filtro por `tenantId` en toda operación, no solo al escribir. No se
implementa todavía un mecanismo centralizado (interceptor/base repository) que fuerce el
filtro automáticamente en todos los repositorios futuros — ese refuerzo es una decisión
de diseño para `/plan-tareas` o una spec posterior si el equipo lo ve necesario; aquí se
exige el filtro explícito y verificado únicamente para `Reservation`.

## Decisiones (resueltas 2026-09-02, priorizando velocidad de entrega)

1. **`tenantId` de la petición en este corte (sin JWT todavía):** header HTTP
   `X-Tenant-Id` obligatorio, documentado explícitamente como un valor de confianza
   temporal hasta que exista autenticación real (spec futura de JWT, capa transversal
   aparte — sigue fuera de alcance de este corte).
2. **Generación de `reservationId`:** UUID v4 generado en el dominio antes de persistir,
   no identidad autogenerada por Postgres.
3. **Herramienta de migración de esquema:** Flyway. Deja un historial de migraciones
   verificable (a diferencia de `ddl-auto` de Hibernate), necesario para el criterio de
   aceptación sobre `tenant_id NOT NULL` desde la primera migración, con configuración
   más simple que Liquibase para este alcance.

## Evidencia para materia

- Primer código real y ejecutable del Backend: sirve como evidencia de arranque de
  implementación en el Weekly correspondiente.
- Demuestra en código dos ADRs ya aceptados (ADR-002: monolito modular; ADR-003:
  aislamiento por `tenantId`), no solo en documentación — material directo para la
  sustentación ("esto no es solo un diagrama, corre").
- Base reutilizable (hexagonal, por bounded context) para que las siguientes HU de
  Reservations (HU-RES-002 a HU-RES-009) y los demás bounded contexts se agreguen sin
  reestructurar lo ya construido.
