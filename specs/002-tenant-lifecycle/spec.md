# 002 — Ciclo de vida del Tenant + primer Administrador

**Estado:** APROBADA
**Fecha:** 2026-09-02
**Repos afectados:** backend
**HU relacionada:** HU-TEN-001 (principal, FR-019)

## Problema

Hoy el Backend solo tiene el módulo `reservations` (spec 001), y el aislamiento por
`tenantId` se confía a un header HTTP sin validar (`X-Tenant-Id`) porque no existe
ningún tenant real persistido contra el cual verificarlo. Tampoco existe ningún
Administrador de tenant que pueda operar el resto del sistema. El Frontend ya tiene
las pantallas (`admin-login`, `platform/operators`, `platform/operators/new`,
`platform/operators/:tenantId`, `platform/audit`) esperando un backend que las sirva.
Sin esto, cualquier spec de autenticación real (Identity and Access / JWT) no tiene
contra qué tenant o usuario validar credenciales.

## Alcance

- Nuevo módulo `tenants` (bounded context Tenant Management), hexagonal como
  `reservations`: dominio, puertos, aplicación, adaptador web y adaptador de
  persistencia.
- Agregado `Tenant`: `tenantId` (identificador único asignado por el Platform
  Administrator al crear, no autogenerado — así lo describe HU-TEN-001 escenario 1:
  "assign a unique identifier"), `commercialName`, `tenantStatus`
  (`Activo`/`Inactivo`), `createdAt`.
- Creación de tenant que exige y persiste, en la misma operación, su primer
  `Membership` con rol `Administrator` (`membershipId`, `tenantId`, `userId` o email,
  `role`, `membershipStatus`, `createdAt`) con contraseña con hash (`BCryptPasswordEncoder`,
  ya disponible por `spring-boot-starter-security`).
- Endpoints:
  - `POST /api/tenants` — crea el tenant en estado `Activo` junto con su primer
    Administrador.
  - `POST /api/tenants/{tenantId}/deactivate` — pasa a `Inactivo`, exige `reason`.
  - `POST /api/tenants/{tenantId}/reactivate` — pasa a `Activo`, exige `reason`.
  - `GET /api/tenants` y `GET /api/tenants/{tenantId}` — listado y detalle, para las
    pantallas `platform/operators` y `platform/operators/:tenantId`.
- Registro de auditoría mínimo (`audit_records`: `auditRecordId`, `tenantId`,
  `actorId`, `action`, `affectedRecordId`, `reason`, `recordedAt`) para las tres
  acciones de ciclo de vida (crear, desactivar, reactivar), y `GET /api/audit` para
  la pantalla `platform/audit`.
- Migración Flyway `V2` con las tablas `tenants`, `memberships` y `audit_records`.

## Fuera de alcance

- Autenticación real / JWT (Identity and Access, HU-IAM-001 a 003) — spec futura. En
  este corte `actorId` en el registro de auditoría y `POST /api/tenants` se reciben
  como dato de la petición, sin validar sesión, igual que el debt ya documentado en
  spec 001 (`SecurityConfig` sigue en `permitAll()`).
- Roles distintos de `Administrator` en la membresía inicial (`Operational
  Collaborator`, `Manager`, `Accountant`, `Analyst`, `End Customer`) — se agregan
  cuando exista una spec de gestión de equipo dentro del tenant.
- Identidad visual del tenant (`logo`, colores, imágenes comerciales) y
  `enabledPaymentMethods` — campos opcionales de `06-data/models.md` sin HU que los
  ejercite todavía.
- Edición de datos del tenant o del Administrador después de creado (solo
  activar/inactivar/reactivar entran en este corte).
- Consulta de auditoría con filtros avanzados (por rango de fecha, por tipo de
  acción) — `GET /api/audit` en este corte solo lista todo, sin paginar ni filtrar.

## Criterios de aceptación

- [ ] `POST /api/tenants` con `tenantId`, `commercialName` y los datos del primer
      Administrador (`name`, `email`, `password`, `passwordConfirmation`) devuelve
      `201 Created`, el tenant queda en `Activo`, y la membresía Administrator queda
      persistida con la contraseña hasheada (nunca en texto plano).
- [ ] `POST /api/tenants` con `tenantId` ya existente devuelve `400`/`409` sin crear
      nada ni duplicar el tenant.
- [ ] `POST /api/tenants` sin `password` igual a `passwordConfirmation` es rechazado
      sin persistir nada.
- [ ] `POST /api/tenants/{tenantId}/deactivate` sin `reason` es rechazado; con
      `reason` pasa el tenant a `Inactivo` y genera un registro en `audit_records`
      con actor, tenant, acción, motivo y fecha/hora.
- [ ] `POST /api/tenants/{tenantId}/reactivate` sobre un tenant `Inactivo` lo pasa a
      `Activo` únicamente por esta operación explícita (nunca como efecto colateral
      de otra acción) y genera su propio registro de auditoría.
- [ ] `GET /api/tenants/{tenantId}` sobre un tenant `Inactivo` sigue devolviendo sus
      datos e historial (`INV-TEN-002`: la desactivación no borra evidencia).
- [ ] `GET /api/audit` devuelve las tres acciones de ciclo de vida realizadas en las
      pruebas anteriores, cada una con actor, tenant, acción, motivo y fecha/hora.
- [ ] El proyecto compila y los tests existentes (incluido `contextLoads` y los de
      spec 001) siguen pasando.

## Impacto en multitenencia

Este corte crea la fuente de verdad de qué tenants existen y en qué estado, que hoy
no existe: el `X-Tenant-Id` de spec 001 sigue sin validarse contra esta tabla en este
corte (queda para la spec de JWT, que sí puede resolver el tenant desde un token en
vez de un header de confianza). `INV-TEN-002` se implementa explícitamente:
desactivar un tenant no borra ni oculta su historial, solo se probará aquí a nivel de
lectura (`GET` sigue funcionando); bloquear nuevas reservas de un tenant `Inactivo`
es responsabilidad del módulo `reservations` y no se toca en este corte al no
haber sido pedido explícitamente — se deja como riesgo abierto (ver abajo).

## Riesgos y decisiones abiertas

1. **`reservations` no valida `tenantStatus`:** con este corte, un tenant `Inactivo`
   podría seguir recibiendo reservas porque `CreateReservationService` no consulta el
   nuevo módulo `tenants`. Opciones: (a) dejarlo así y resolverlo en la spec de JWT
   cuando el tenant se resuelva de forma centralizada, o (b) agregar aquí mismo una
   verificación cruzada mínima. Recomiendo (a) para no acoplar los dos módulos antes
   de tiempo.
2. **Formato de `tenantId`:** HU-TEN-001 dice "assign a unique identifier" pero no
   especifica formato. Propongo: string corto tipo slug (`^[a-z0-9-]{3,50}$`),
   validado único, en vez de UUID — más legible para un identificador que un humano
   escribe a mano. A confirmar.
3. **Comunicación entre `tenants` y `reservations`:** ambos son módulos del mismo
   monolito; en este corte no comparten código entre sí (sin dependencia de un
   módulo hacia el otro), consistente con el aislamiento por bounded context de
   ADR-002.

## Evidencia para materia

- Segundo corte vertical del Backend, primero que involucra dos agregados
  relacionados (`Tenant` + `Membership`) y auditoría (`INV-AUD-001`), útil para la
  sustentación como evidencia de que el modelo de multitenencia no es solo un campo
  suelto sino un ciclo de vida completo con trazabilidad.
- Desbloquea directamente la siguiente spec (Identity and Access / JWT), que necesita
  un tenant y un usuario reales contra los cuales autenticar.
