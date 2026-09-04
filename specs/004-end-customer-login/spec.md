# 004 — Login de End Customer (emisión de JWT)

**Estado:** TERMINADA
**Fecha:** 2026-09-03
**Repos afectados:** backend (requiere un cambio futuro en frontend, documentado pero
no ejecutado aquí — ver "Riesgos y decisiones abiertas")
**HU relacionada:** HU-IAM-002 (principal, FR-017). Depende de HU-IAM-001 (spec 003,
ya implementada).

## Problema

Hoy no existe ninguna autenticación real: `SecurityConfig` sigue en `permitAll()`
(deuda documentada desde spec 001) y `login.component.ts` del Frontend es una
simulación sin llamada HTTP. Sin un login real, ningún cliente puede demostrar quién
es ante el Backend, lo que bloquea cualquier flujo futuro de reservas ligadas a una
cuenta autenticada y HU-IAM-003 (recuperación de contraseña, que depende de saber
identificar una cuenta primero).

## Alcance

- Endpoint `POST /api/tenants/{tenantId}/login` — recibe `email` y `password`.
  El `tenantId` va en la URL, igual que `POST /api/tenants/{tenantId}/customers`
  (spec 003): sin él, el Backend no tiene con qué filtrar, porque `ADR-003
  (tenant-isolation-strategy)` ya fijó `tenantId` como discriminador único de
  aislamiento y el email **no** es único a nivel global (spec 003, HU-IAM-001
  escenario 3: el mismo email puede existir en tenants distintos, como cuentas
  completamente separadas).
- Login exitoso (`200`): tenant existe y está `Activo`, la membership con ese email en
  ese tenant existe, está `ACTIVA`, y el `password` coincide con el hash guardado.
  Devuelve un JWT (HS256) más los datos básicos de la membership (sin `passwordHash`).
- El JWT lleva en sus claims: `sub` (membershipId), `tenantId`, `email`, `role`, y una
  expiración (no es indefinido).
- Un único mensaje de error genérico (`401`) para **todos** estos casos, sin
  distinguir cuál aplicó (HU-IAM-002 escenario 2 y 3 lo piden explícitamente):
  password incorrecto, email sin membership en ese tenant (incluye el caso de que el
  email sí exista pero en otro tenant), `tenantId` inexistente, tenant `Inactivo`,
  membership `INACTIVA`.
- Se documenta, en un comentario extenso dentro del código nuevo de esta spec (no en
  un archivo aparte), por qué el tenant debe ir en la URL y qué le falta al Frontend
  para poder invocar este endpoint — pensado para que quien mantiene el Frontend (sea
  persona o IA) lo lea ahí mismo y lo valide, sin tener que rastrear esta spec.

## Fuera de alcance

- Login de `Administrator`/`Platform Administrator` (los modos "Equipo del operador"
  de `login.component.ts` y `admin-login.component.ts`) — no existe ninguna HU que lo
  respalde todavía; no se inventa una.
- Recuperación de contraseña (HU-IAM-003) — spec futura.
- Aplicar el JWT emitido para proteger algún endpoint existente (`tenants`,
  `customers`, `reservations`): este corte solo lo emite. `SecurityConfig` sigue en
  `permitAll()` al terminar esta spec. Cambiar eso de golpe rompería los contratos
  actuales que reciben `actorId` y `X-Tenant-Id` sin sesión (deuda ya documentada en
  spec 001 y 002) — se resuelve en una spec futura dedicada, coordinada aparte.
- Refresh tokens, logout o cualquier forma de invalidar un JWT antes de su expiración.
- Cualquier cambio al repo Frontend: se documenta la necesidad, no se ejecuta.

## Criterios de aceptación

- [x] `POST /api/tenants/{tenantId}/login` con `email`/`password` correctos de una
      membership `ACTIVA` en un tenant `Activo` devuelve `200` con un JWT válido
      (`sub`, `tenantId`, `email`, `role`, expiración) y los datos básicos de la
      membership, nunca `passwordHash`.
- [x] `password` incorrecto devuelve `401` con el mensaje genérico.
- [x] Email sin membership en ese tenant devuelve `401` con el mismo mensaje genérico
      (cubre HU-IAM-002 escenario 3: mismo email con cuenta en un tenant distinto).
- [x] `tenantId` inexistente devuelve `401` con el mismo mensaje genérico — **no**
      `404`, a diferencia de los demás endpoints de `tenants`: revelar con un `404`
      que un `tenantId` no existe permitiría enumerar tenants válidos probando IDs
      contra el login, algo que ningún otro endpoint de esta spec necesita ocultar.
- [x] Tenant `Inactivo` devuelve `401` con el mismo mensaje genérico (consistente con
      el bloqueo ya aplicado al registro en spec 003).
- [x] Membership `INACTIVA` devuelve `401` con el mismo mensaje genérico.
- [x] El proyecto compila y los tests existentes (spec 001, 002 y 003) siguen pasando.

## Impacto en multitenencia

El tenant siempre se resuelve desde la URL, nunca inferido del email: es la pieza que
le faltaba a esta spec para poder aplicar `INV-TEN-001`/`ADR-003` en el login, ya que
sin un tenant explícito el mismo email podría existir en más de una membership y el
Backend no tendría forma de saber contra cuál validar la contraseña.

## Riesgos y decisiones abiertas

1. **El Frontend no tiene ningún campo de tenant en `login.component.html`/`.ts`** (ni
   tampoco en `signup.component.html`, mismo vacío heredado de spec 003 que no se
   corrigió porque esa spec no tocaba Frontend). Sin ese cambio, esta spec queda
   implementada pero inalcanzable desde la pantalla actual. Se documenta extensamente
   en el código, no se ejecuta el cambio de Frontend en esta spec.
2. **Sin HU para login de staff/Administrator/Platform Administrator:** los modos
   "Equipo del operador" y `admin-login` del Frontend ya existen visualmente pero no
   tienen historia de usuario que los respalde; quedan fuera hasta que exista una.
3. **Secreto de firma y expiración del JWT no vienen de ninguna HU:** se definen como
   configuración (`application.properties`) con un valor por defecto documentado en
   `plan.md`, ajustable sin tocar código.
4. **Este corte no protege nada con el JWT que emite:** es deuda conocida y explícita,
   no un olvido — aplicar JWT a los endpoints existentes es su propia spec futura.

## Evidencia para la materia

Cierra FR-017/HU-IAM-002, primera pieza de autenticación real del proyecto (hasta
ahora todo era `permitAll()`). Deja además, documentado dentro del propio código, un
ejemplo concreto de coordinación entre repos con dueños distintos (Backend propio,
Frontend de una compañera) sin tocar el repo ajeno — evidencia útil para explicar en
sustentación cómo se manejó el trabajo distribuido en equipo.
