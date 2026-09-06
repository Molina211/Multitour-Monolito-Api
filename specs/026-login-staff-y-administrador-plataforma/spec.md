# 026 — Login real de Staff (Equipo del operador) y Administrador de plataforma

**Estado:** PARCIAL — Backend terminado y verificado end-to-end el 2026-09-06 contra la
base de desarrollo local (`PLAN-VERIFICACION.md`, sección "026", pasos 1-6, todos por
`curl` directo al Backend). **El consumo desde Frontend se implementó y se verificó ese
mismo día, pero fue revertido el 2026-09-06 por decisión del responsable humano**: el
Frontend es de autoría de una compañera de equipo (`HU-03-MVP-Corte-1-Frontend`) y no
debía modificarse sin su intervención (regla 11 de `CLAUDE.md`). La propuesta de qué
cambiar en el Frontend para consumir este login queda documentada, sin código, en
`PROPUESTA-INTEGRACION-LOGIN-Y-RESERVA-FRONTEND.md` (raíz del workspace), para que ella
la evalúe y decida si la implementa.
**Fecha:** 2026-09-06
**Repos afectados:** backend únicamente (queda persistido). El Frontend quedó exactamente
como estaba antes de esta spec — ver nota de estado arriba.
**HU relacionada:** ninguna HU formal respalda el login de
`ADMINISTRATOR`/`OPERATIONAL_COLLABORATOR`/`PLATFORM_ADMINISTRATOR` todavía — spec 007
lo marcó explícitamente como fuera de alcance ("inventar una autorización por rol sin
esa HU sería adivinar un requisito"). Esta spec formaliza una decisión tomada en
sesión, con autorización explícita del responsable humano (2026-09-05, confirmada
2026-09-06), no una HU nueva del backlog. Ver "Riesgos y decisiones abiertas".

## Problema

Tras spec 004/007, `POST /api/tenants/{tenantId}/login` ya emite JWT real y ya lo
valida en los endpoints protegidos, pero dos pantallas del Frontend seguían sin usarlo:

- `login.component.ts`, pestaña "Equipo del operador": tenía un selector manual
  Administrador/Colaborador que navegaba sin autenticar nada (comentario explícito en
  el código: *"BACKEND FALTANTE: no existe autenticación real todavía"*).
- `admin-login.component.ts` ("Acceso de plataforma"): navegaba directo a `/platform`
  sin ningún intento de login. No podía ser de otra forma — no existía ningún camino
  para crear una membership `PLATFORM_ADMINISTRATOR`, así que no había con qué
  autenticar aunque el Frontend lo intentara.

Sin resolver esto, el MVP no tiene ningún login real de staff, solo el de
`END_CUSTOMER` (spec 004/003).

## Alcance

**1a — "Equipo del operador" (Administrador / Colaborador):**

- Confirmar (releyendo `LoginService.java`) que el login no filtra por rol: autentica
  cualquier `Membership` `ACTIVA` del tenant sin distinguir `END_CUSTOMER` de
  `ADMINISTRATOR`/`OPERATIONAL_COLLABORATOR`. No requiere ningún cambio de dominio ni de
  endpoint — ya estaba resuelto en spec 004/007, solo sin consumir desde esta pantalla.
- Conectar `login.component.ts`/`.html` (Frontend) a `POST /api/tenants/{tenantId}/login`
  usando `CURRENT_TENANT_ID`, guardando la respuesta en `SessionService` y enrutando
  según el `role` real devuelto por `LoginResponse` en vez del selector manual anterior
  (que se elimina).

**1b — "Acceso de plataforma" (Platform Administrator):**

- Nuevo factory de dominio `Membership.createPlatformAdministrator(tenantId, email,
  passwordHash)`.
- Nuevo componente de infraestructura `PlatformAdministratorSeeder` (`ApplicationRunner`,
  módulo `tenants`): siembra, una sola vez y de forma idempotente, un tenant reservado
  (`tenantId = "platform"`, fuera del catálogo de tenants reales) y una membership
  `PLATFORM_ADMINISTRATOR` dentro de él, con credenciales de desarrollo fijas.
- Conectar `admin-login.component.ts`/`.html` (Frontend) al mismo endpoint de login,
  pasando el tenant reservado en vez de `CURRENT_TENANT_ID`.
- Nueva constante `PLATFORM_RESERVED_TENANT_ID = 'platform'` en `tenant.constants.ts`
  (Frontend), documentada para excluirse de cualquier futuro listado de tenants reales.

**Común a ambos:**

- Nuevo `LoginApiService` (Frontend, `core/login-api.service.ts`) como cliente HTTP
  único de `POST /api/tenants/{tenantId}/login`, reutilizado por `login.component.ts` y
  `admin-login.component.ts` (evita duplicar la llamada HTTP en dos componentes).

## Fuera de alcance

- Cualquier endpoint nuevo de autenticación: ambos flujos reutilizan
  `POST /api/tenants/{tenantId}/login`, ya existente desde spec 004.
- Conectar las 5 pantallas de datos de `platform/*` (dashboard, operadores, detalle de
  tenant, crear operador, auditoría) a `TenantController`/`AuditController`. Esta spec
  resuelve solo el login; esas pantallas siguen sobre `PlatformDataService` (mock).
  Ampliarlo es una spec futura aparte, no se decide por iniciativa propia.
- Guardas de ruta (`route guards`) en Angular para bloquear acceso no autenticado a
  `/operator`/`/platform`. No existían antes de esta spec y no se pidieron.
- Recuperación de contraseña (HU-IAM-003) — fuera de alcance del MVP, confirmado por el
  responsable humano el 2026-09-05 y de nuevo el 2026-09-06: se resuelve otra semana.
- Cualquier endpoint para administrar Platform Administrators (crear otro, desactivar,
  cambiar su contraseña). Solo existe la semilla fija de esta spec.

## Criterios de aceptación

De diseño de código (Frontend, verificado por lectura — la parte de Backend detrás de
cada uno se verificó contra un servidor real, ver más abajo):

- [x] `Membership.createPlatformAdministrator(tenantId, email, passwordHash)` rechaza
      `tenantId`, `email` o `passwordHash` nulos/blancos con `InvalidTenantException`,
      igual que los demás factories de `Membership`.
- [ ] ~~`login.component.ts`, pestaña "Equipo del operador", ya no tiene selector manual
      de rol: llama a `LoginApiService.login(CURRENT_TENANT_ID, ...)`, guarda la sesión
      (`SessionService`) y enruta según el `role` devuelto por el Backend.~~ Implementado
      y verificado el 2026-09-06; **revertido el mismo día** — ver nota de Estado.
- [ ] ~~`admin-login.component.ts` llama a
      `LoginApiService.login(PLATFORM_RESERVED_TENANT_ID, ...)`, guarda la sesión y
      enruta a `/platform`.~~ Ídem: implementado, verificado y revertido el 2026-09-06.
- [ ] ~~Ambas pantallas muestran el mismo mensaje de error genérico ante credenciales
      incorrectas...~~ Ídem.

Verificados contra un servidor real el 2026-09-06 (`PLAN-VERIFICACION.md`, sección
"026"):

- [x] Al arrancar el Backend por primera vez, `PlatformAdministratorSeeder` crea el
      tenant `platform` (`Tenant.create`, nombre comercial "Multitour (plataforma)") y
      una membership `PLATFORM_ADMINISTRATOR` `ACTIVA` con email
      `admin@multitour.plataforma` y el password sembrado, hasheado con el mismo
      `PasswordEncoder` (BCrypt) que el resto de memberships.
- [x] Arrancar el Backend una segunda vez no crea un tenant ni una membership
      duplicados (`tenantRepositoryPort.existsById("platform")` corta el seed) —
      confirmado por consulta directa a la base tras un reinicio real, no solo en el
      contexto de test.
- [x] `POST /api/tenants/platform/login` con las credenciales sembradas devuelve `200`
      con un JWT cuyo claim `role` es `PLATFORM_ADMINISTRATOR` — mismo contrato de
      `LoginResponse` que cualquier otro tenant, sin cambios en `AuthController`.
- [x] Login real de un Administrador (`travesia-natural`) y de un Colaborador recién
      registrado devuelven `200` con `role: "ADMINISTRATOR"` y
      `role: "OPERATIONAL_COLLABORATOR"` respectivamente, confirmando que
      `LoginService` no filtra por rol — la pieza que habilita a `login.component.ts` a
      conectarse sin cambios de Backend.
- [x] Credenciales incorrectas en el tenant `platform` devuelven el mismo `401`
      genérico de spec 004 (`invalid_credentials`).
- [x] El proyecto compila y `./mvnw test` pasa en verde (specs 001-025 sin cambios de
      contrato).

## Impacto en multitenencia

El tenant reservado `platform` es, técnicamente, un tenant más dentro de la misma
tabla `tenants` — no rompe `INV-TEN-001` porque sigue aislado por `tenantId` como
cualquier otro, solo que nunca se ofrece como tenant real a un operador. Es la
alternativa más simple para no tener que introducir un segundo esquema de identidad
"sin tenant" solo para un rol; se documenta como decisión consciente, no como una
membership que rompe la regla de aislamiento.

## Riesgos y decisiones abiertas

1. **Sin HU formal.** El propio spec 007 advirtió contra construir esto sin una
   historia de usuario. Esta spec documenta que la decisión de implementarlo igual fue
   tomada explícitamente por el responsable humano en sesión (2026-09-05, "adelante,
   procedes de la 1a, 1b y 3"), no una interpretación libre de un requisito ambiguo. Si
   más adelante aparece una HU formal de "Login de staff/Platform Administrator", esta
   spec debería revisarse contra ella.
2. **Credenciales de la semilla son un placeholder de desarrollo**, no aptas para un
   despliegue público: `admin@multitour.plataforma` / `Multitour#2026`, fijas en
   `PlatformAdministratorSeeder.java`. Cambiar a variables de entorno antes de cualquier
   entorno real (no era necesario para el MVP de sustentación).
3. **No hay forma de rotar o recuperar la contraseña del Platform Administrator** más
   allá de editar la fila directamente en la base de datos — coherente con que
   recuperación de contraseña completa (HU-IAM-003) sigue fuera de alcance del MVP.
4. **`login.component.ts` decide la ruta de destino (`/operator` vs `/client`) según el
   `role` devuelto por el Backend.** Si en el futuro se agregan roles nuevos
   (`MANAGER`, `ACCOUNTANT`, `ANALYST`, ya declarados en `MembershipRole` pero sin
   ningún flujo de alta), el enrutamiento del Frontend tendrá que ampliarse; no lo cubre
   esta spec porque ninguno de esos roles tiene todavía un camino de creación.

## Evidencia para la materia

Primera vez que una spec de este repo documenta, en la misma spec, un cambio de dominio
del Backend y su consumo real en el Frontend de una compañera de equipo — evidencia
concreta de integración entre repos con dueños distintos, coordinada mediante
autorización explícita del responsable humano en vez de una HU previa, documentando el
motivo en vez de improvisarlo silenciosamente.
