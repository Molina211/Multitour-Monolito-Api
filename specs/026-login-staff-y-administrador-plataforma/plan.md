# 026 — Plan técnico

## Enfoque

Ninguna de las dos piezas necesita un endpoint nuevo: `POST /api/tenants/{tenantId}/login`
(spec 004) ya autentica cualquier rol de `Membership` sin filtrar. Todo el trabajo de
Backend se reduce a resolver **cómo llega a existir** una membership
`PLATFORM_ADMINISTRATOR`, dado que hoy solo `ADMINISTRATOR`/`OPERATIONAL_COLLABORATOR`/
`END_CUSTOMER` tienen un camino de alta (specs 002, 014, 003 respectivamente).

Se descartó modelar "sin tenant" como `tenantId` nulo en `Membership`: la tabla
`memberships` tiene `tenant_id` como FK obligatoria a `tenants` desde
`V2__create_tenants.sql` (columna discriminadora de `INV-TEN-001`/`ADR-003`), así que
permitir `tenantId` nulo exigiría relajar esa invariante para todo el módulo `tenants`
por un único caso de uso. La alternativa elegida — un tenant reservado sintético — no
toca el esquema ni la invariante: el Platform Administrator simplemente vive en un
tenant que nunca se ofrece a un operador real.

Para no depender de calcular un hash BCrypt fuera de la aplicación (no hay forma de
generarlo sin correr el proyecto, prohibido sin permiso por la regla 5 de `CLAUDE.md`
salvo autorización explícita), se usa un `ApplicationRunner` de Spring: corre una sola
vez al arrancar, con el `PasswordEncoder` ya inyectado por el propio framework, e
inserta directamente vía los puertos de salida ya existentes (`TenantRepositoryPort`,
`MembershipRepositoryPort`) — mismo patrón de puertos hexagonales que el resto del
módulo, sin abstracciones nuevas.

Del lado de Frontend, se extrae la llamada HTTP de login a un servicio nuevo
(`LoginApiService`) en vez de duplicarla en `login.component.ts` y
`admin-login.component.ts` — mismo criterio ya usado por `CatalogApiService`/
`CustomerApiService` en este repo.

## Cambios por repositorio

### Backend

- `tenants/domain/model/Membership.java`: nuevo factory `createPlatformAdministrator
  (tenantId, email, passwordHash)`, mismas validaciones de nulidad/blank que los demás
  factories.
- `tenants/domain/model/MembershipRole.java`: actualiza el Javadoc de la clase para
  reflejar que `PLATFORM_ADMINISTRATOR` ya tiene un camino de creación (el seed), a
  diferencia del estado anterior ("no se usa en ningún otro lugar").
- `tenants/infrastructure/PlatformAdministratorSeeder.java` (nuevo): `@Component`
  `ApplicationRunner`. Constante pública `RESERVED_TENANT_ID = "platform"` (para que
  el Frontend, o cualquier código futuro, no tenga que repetir el literal). Constantes
  privadas `SEED_EMAIL`/`SEED_PASSWORD`. En `run(...)`: si `tenantRepositoryPort
  .existsById(RESERVED_TENANT_ID)` ya es verdadero, no hace nada (idempotencia); si no,
  crea el tenant reservado vía `Tenant.create(...)`, lo guarda, hashea la contraseña
  sembrada con el `PasswordEncoder` inyectado y guarda la membership vía
  `Membership.createPlatformAdministrator(...)`.
- Sin cambios de esquema: no hay migración Flyway nueva — el seeder usa los mismos
  puertos JPA ya existentes, no una tabla nueva.
- `PLAN-VERIFICACION.md`: nueva sección "026 — Login staff y Platform Administrator".

### Frontend (`HU-03-MVP-Corte-1-Frontend`)

- `core/login-api.service.ts` (nuevo): `LoginApiService.login(tenantId, {email,
  password})` → `POST /api/tenants/{tenantId}/login`, tipado con `LoginRequest`/
  `LoginResponse` (espejo de `LoginRequest.java`/`LoginResponse.java`).
- `core/tenant.constants.ts`: agrega `PLATFORM_RESERVED_TENANT_ID = 'platform'`, con
  comentario explicando que es el tenant reservado del seeder.
- `pages/login/login.component.ts`/`.html`: elimina `staffRole`/`selectStaffRole` (el
  selector manual Administrador/Colaborador) y el `onSubmit` simulado. Nuevo `onSubmit`
  real: llama a `loginApiService.login(CURRENT_TENANT_ID, {email, password})`, en éxito
  guarda la sesión (`SessionService.setSession(response)`) y enruta a `/operator`
  (fijando el rol en `OperatorRoleService` desde `response.role`) o `/client` según el
  rol devuelto; en error muestra "Correo o contraseña incorrectos.".
- `pages/admin-login/admin-login.component.ts`/`.html`: mismo patrón que
  `login.component.ts`, pero contra `PLATFORM_RESERVED_TENANT_ID`, y enruta siempre a
  `/platform` en éxito.

## Decisiones técnicas

- **Tenant reservado en vez de `tenantId` nulo o un módulo de identidad aparte**: ya
  justificado en "Enfoque". Alternativa descartada: crear un `AuthController` paralelo
  solo para Platform Administrator — se descartó porque `LoginService` ya soporta
  cualquier rol sin cambios, así que un segundo endpoint sería una duplicación exacta
  del primero sin ninguna diferencia de comportamiento.
- **Semilla vía `ApplicationRunner`, no vía migración Flyway con un `INSERT` fijo**: un
  `INSERT` en una migración obligaría a calcular el hash BCrypt fuera de la aplicación
  (con una herramienta externa) y a mantenerlo sincronizado si algún día cambia el
  `PasswordEncoder`; el `ApplicationRunner` genera el hash con el encoder real de la
  aplicación en el momento del arranque, sin esa fragilidad.
- **Credenciales fijas en código, no en `application.properties`**: mismo criterio ya
  aceptado en spec 004 para `app.jwt.secret` (constante con aviso explícito de que es
  solo para desarrollo) — evita inventar un mecanismo de configuración nuevo para un
  valor que de todas formas hay que cambiar manualmente antes de cualquier despliegue
  real.
- **Un solo `LoginApiService` compartido entre `login.component.ts` y
  `admin-login.component.ts`**: evita dos implementaciones de la misma llamada HTTP con
  el mismo contrato.

## Modelo de datos

Sin migración nueva. El seeder inserta una fila en `tenants` (`tenant_id = 'platform'`)
y una en `memberships` (`role = 'PLATFORM_ADMINISTRATOR'`, `tenant_id = 'platform'`)
usando las tablas ya creadas en `V2__create_tenants.sql`.

## Contratos

Sin contrato nuevo. `POST /api/tenants/{tenantId}/login` mantiene exactamente el mismo
request/response de spec 004 (`LoginRequest`/`LoginResponse`); lo único distinto es el
valor de `tenantId` en la URL (`platform` en vez de un tenant real) y el `role` que
viene en la respuesta (`PLATFORM_ADMINISTRATOR`).

## Cómo se verifica

- Arranque del Backend (bajo autorización explícita, regla 5 de `CLAUDE.md`) y
  confirmación de que el seed corrió una sola vez: `curl` a
  `POST /api/tenants/platform/login` con las credenciales sembradas → `200` con
  `role: "PLATFORM_ADMINISTRATOR"`.
- Reinicio del Backend y confirmación de que no se duplica el tenant ni la membership
  (consulta directa a la base o segundo intento de seed sin error de clave duplicada).
- `login.component.ts` con un Administrador/Colaborador real de `travesia-natural` (ya
  sembrados o creados en specs 002/014) → sesión real, enruta a `/operator`.
- `admin-login.component.ts` con las credenciales sembradas → sesión real, enruta a
  `/platform`.
- `./mvnw test` en verde (specs 001-025 sin romperse).
- Detalle línea por línea en `PLAN-VERIFICACION.md`, sección "026".
