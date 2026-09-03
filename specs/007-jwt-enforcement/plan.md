# 007 — Plan técnico

## Enfoque

`JwtTokenProvider` (spec 004) gana un método de parseo/validación. Un filtro nuevo
(`JwtAuthenticationFilter`) lo usa para poblar el `SecurityContext` de Spring Security
cuando el header `Authorization: Bearer <token>` es válido; si falta o es inválido, no
puebla nada (queda anónimo) — el rechazo real lo decide `SecurityConfig` según la ruta.
`SecurityConfig` exige autenticación solo para `POST
/api/tenants/{tenantId}/reservations` y agrega un `AuthenticationEntryPoint` propio para
devolver `401` en JSON (por defecto Spring Security devolvería `403` sin este bean, ya
que no hay `formLogin`/`httpBasic` configurado). La comparación "el tenant del token
coincide con el de la URL" no la puede hacer un matcher de URL (necesita comparar un
path variable contra un claim), así que se hace dentro del controller, con el mismo
patrón de excepción + `@ExceptionHandler` ya usado en todo el proyecto.

## Cambios por repositorio

Solo backend.

- `common/security/JwtTokenProvider.java` — agrega `parse(String token)` que valida
  firma+expiración con la misma `signingKey` ya usada para firmar, y devuelve un
  `JwtPrincipal` (ver abajo). Lanza `io.jsonwebtoken.JwtException` (o subclase, p. ej.
  `ExpiredJwtException`/`SignatureException`) si el token es inválido — no se captura
  aquí, la captura la hace el filtro.
- `common/security/JwtPrincipal.java` — nuevo `record JwtPrincipal(String membershipId,
  String tenantId, String email, String role)`, construido desde los claims (`sub`,
  `tenantId`, `email`, `role`).
- `common/security/JwtAuthenticationFilter.java` — nuevo `OncePerRequestFilter`. Lee
  `Authorization`, si empieza con `Bearer `, intenta `jwtTokenProvider.parse(...)`; si
  tiene éxito, arma un `UsernamePasswordAuthenticationToken(principal, null,
  List.of(new SimpleGrantedAuthority("ROLE_" + principal.role())))` y lo pone en
  `SecurityContextHolder`. Si `parse` lanza `JwtException` o el header no viene, no hace
  nada (sigue la cadena sin autenticar) — igual que documenta `spec.md`.
- `common/security/JwtAuthenticationEntryPoint.java` — nuevo, implementa
  `AuthenticationEntryPoint`; escribe `401` con `ErrorResponse("unauthorized", "missing
  or invalid token")` en JSON (mismo DTO ya usado en el resto del proyecto).
- `common/security/SecurityConfig.java` — registra el filtro nuevo antes de
  `UsernamePasswordAuthenticationFilter`; agrega
  `.authorizeHttpRequests(a -> a.requestMatchers(HttpMethod.POST,
  "/api/tenants/*/reservations").authenticated().anyRequest().permitAll())` y
  `.exceptionHandling(e -> e.authenticationEntryPoint(jwtAuthenticationEntryPoint))`.
- `reservations/domain/exception/TenantMismatchException.java` — nuevo, mismo rol que
  `TenantInactiveException`: se lanza cuando el `tenantId` del token autenticado no
  coincide con el `{tenantId}` de la URL.
- `reservations/infrastructure/in/web/ReservationController.java` — `create(...)` gana
  un parámetro `Authentication authentication` (lo inyecta Spring Security, ya puesto
  por el filtro); castea el principal a `JwtPrincipal`, compara `tenantId`, lanza
  `TenantMismatchException` si no coincide, y usa `principal.membershipId()` como
  `customerId` al construir `CreateReservationCommand` (ignora cualquier `customerId`
  que venga en el body). Agrega `@ExceptionHandler(TenantMismatchException.class) →
  403`.
- `reservations/infrastructure/in/web/dto/CreateReservationRequest.java` — pierde el
  campo `customerId` (ya no lo aporta el cliente).
- `reservations/domain/port/in/CreateReservationCommand.java` — sin cambio de forma
  (sigue con `customerId` como `String`), pero ahora ese valor siempre lo construye el
  controller desde el token, nunca desde el body.

## Decisiones técnicas

- **`AuthenticationEntryPoint` propio en vez de dejar el `403` por defecto de Spring
  Security**: sin `formLogin`/`httpBasic`, Spring Security 6 usa
  `Http403ForbiddenEntryPoint` para cualquier acceso denegado por falta de
  autenticación, lo que daría `403` tanto para "no autenticado" como para "tenant no
  coincide" — el criterio de aceptación exige distinguir `401` (sin token/token
  inválido) de `403` (token válido de otro tenant). Se separan con un entry point propio
  para el primer caso y una excepción de dominio para el segundo.
- **Comparación de tenant en el controller, no en `SecurityConfig`**: los matchers de
  Spring Security solo evalúan la ruta, no pueden comparar un path variable contra un
  claim del token autenticado; hacerlo en el controller reutiliza el mismo patrón de
  excepción + `@ExceptionHandler` que ya usa todo el proyecto (`TenantNotFoundException`,
  `TenantInactiveException`, etc.), en vez de inventar un mecanismo nuevo.
  Alternativa descartada: un segundo filtro que lea path variables — más código para el
  mismo resultado, y los filtros de Servlet no tienen acceso directo a `@PathVariable`
  resuelto por Spring MVC.
  - **Fallo silencioso del filtro ante token inválido (no lanza excepción)**: si el
  filtro lanzara la excepción directamente, tendría que conocer si la ruta actual
  requiere autenticación o no (rompería la separación de responsabilidades con
  `SecurityConfig`). Dejar el contexto vacío y que `SecurityConfig`/el
  `AuthenticationEntryPoint` decidan el `401` es el patrón estándar de Spring Security
  para filtros de autenticación custom.
- **No se crea un `UserDetailsService`/`AuthenticationProvider` completo**: no hace
  falta volver a autenticar contra la base de datos en cada request (el JWT ya es la
  prueba de identidad); construir el `Authentication` directamente desde los claims ya
  parseados es el patrón estándar para JWT stateless en Spring Security, evita una
  consulta a `memberships` por cada request.

## Contratos

- `POST /api/tenants/{tenantId}/reservations` — body: `{projectedValue,
  reservedServices: [...]}` (ya no lleva `customerId`). Requiere header `Authorization:
  Bearer <token>`.
  - `401` si falta el header o el token es inválido/expirado.
  - `403` si el token es válido pero su claim `tenantId` no coincide con el `{tenantId}`
    de la URL.
  - `404`/`409` sin cambios (tenant inexistente/inactivo, ya cubierto en spec 006).
  - `201` con la reserva creada, `customerId` igual al `sub` del token.
- Todos los demás endpoints existentes: sin cambio de contrato ni de código de estado.

## Cómo se verifica

- Cada criterio de aceptación de `spec.md` con un `curl` correspondiente en la nueva
  sección "007" de `PLAN-VERIFICACION.md`: sin token (`401`), token con firma alterada
  (`401`), token de un tenant distinto al de la URL (`403`), token válido (`201`), y un
  `curl` de control a un endpoint no protegido (p. ej. `GET /api/tenants`) para
  confirmar que sigue respondiendo igual sin token.
- `./mvnw test` en verde (specs 001-006 sin regresión).
