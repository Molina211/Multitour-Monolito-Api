# 004 — Plan técnico

## Enfoque

Se agrega un caso de uso de login dentro del módulo `tenants` existente (mismo bounded
context de `Membership`), sin crear un módulo `identity` separado — mismo criterio que
spec 003. El login reutiliza `MembershipRepositoryPort`, `TenantRepositoryPort` y el
`PasswordEncoder` (BCrypt) ya existentes. Lo único nuevo de infraestructura transversal
es un emisor de JWT en `common/security`, análogo a como `PasswordEncoder` ya vive ahí
como bean de Spring Security inyectado directo (sin puerto hexagonal propio) — se sigue
el mismo patrón para el emisor de JWT en vez de inventar una abstracción de puerto que
nadie más necesita todavía.

Todos los casos de rechazo (tenant inexistente, tenant `Inactivo`, email sin membership,
membership `INACTIVA`, password incorrecto) colapsan en una única excepción de dominio
(`InvalidCredentialsException`) con un mensaje genérico fijo. Esto no es solo una regla
de mapeo en el controller: al existir una sola excepción posible para todos los casos,
es imposible que un futuro cambio en el controller filtre accidentalmente por qué falló
un login, sin tener que recordar mantener sincronizados varios `@ExceptionHandler`.

## Cambios por repositorio

Solo backend.

- `pom.xml`: agrega `io.jsonwebtoken:jjwt-api`, `jjwt-impl` (runtime) y `jjwt-jackson`
  (runtime), versión `0.12.6`.
- `application.properties`: `app.jwt.secret` y `app.jwt.expiration-minutes`.
- `tenants/domain/model/Membership.java`: nuevo factory `reconstitute(...)` para
  reconstruir una `Membership` ya persistida sin re-ejecutar las validaciones de alta
  (que sí aplican a `createAdministrator`/`createEndCustomer`, pero no tienen sentido al
  leer de la base de datos).
- `tenants/domain/exception/InvalidCredentialsException.java`: nueva, mensaje genérico
  único.
- `tenants/domain/port/out/MembershipRepositoryPort.java`: agrega
  `findByTenantIdAndEmail(String tenantId, String email)`.
- `tenants/infrastructure/out/persistence/MembershipJpaRepository.java` y
  `MembershipRepositoryAdapter.java`: implementan la búsqueda anterior, mapeando
  `MembershipEntity` → `Membership` vía `reconstitute`.
- `common/security/JwtTokenProvider.java`: nuevo, componente Spring que genera el JWT
  (HS256) a partir de `membershipId`, `tenantId`, `email`, `role`, usando la
  configuración de `application.properties`.
- `tenants/domain/port/in/LoginCommand.java`, `LoginUseCase.java`, `LoginResult.java`:
  nuevos puertos de entrada.
- `tenants/application/LoginService.java`: nuevo, implementa `LoginUseCase`.
- `tenants/infrastructure/in/web/AuthController.java`: nuevo,
  `POST /api/tenants/{tenantId}/login`. **Aquí vive el comentario extenso exigido**
  explicando por qué el `tenantId` va en la URL y qué le falta al Frontend.
- `tenants/infrastructure/in/web/dto/LoginRequest.java`, `LoginResponse.java`: nuevos.
- `PLAN-VERIFICACION.md`: nueva sección "004 — End customer login".

## Decisiones técnicas

- **Librería JWT: `io.jsonwebtoken` (jjwt) 0.12.6**, alternativa descartada:
  `spring-security-oauth2-resource-server` + Nimbus — está pensado para *validar*
  tokens emitidos por un Identity Provider externo (Auth0, Keycloak, etc.), no para que
  la propia aplicación *emita* los suyos; usarlo aquí sería más pesado y menos directo
  que jjwt para un emisor propio simple. Como este corte no valida/protege nada con el
  JWT (ver "Fuera de alcance" en `spec.md`), no hace falta el starter de
  `resource-server` todavía — se evalúa cuando exista la spec que aplique el JWT a
  endpoints existentes.
- **Secreto y expiración del JWT como configuración con valor por defecto**: sin
  ninguna HU que lo especifique, se fija `app.jwt.expiration-minutes=480` (8 horas,
  razonable para una sesión de trabajo en el portal de un cliente) y
  `app.jwt.secret` con un valor de desarrollo generado localmente, documentado en el
  propio `application.properties` como *no apto para producción* — mismo criterio que
  ya se usa para credenciales de Postgres en ese archivo. Ajustable sin tocar código.
- **`tenantId` en la URL, no en el body ni en un header**: coherente con el precedente
  ya aprobado en spec 003 (`POST /api/tenants/{tenantId}/customers`) y con `ADR-003
  (tenant-isolation-strategy)`, que fija `tenantId` como discriminador obligatorio de
  aislamiento. Alternativas descartadas (detalladas también en el comentario del
  código, para quien no haya leído este plan): tenant en el body del login (se
  descartó por inconsistencia con el resto de la API de `tenants`, que siempre usa la
  URL) y resolver el tenant por subdominio (se descartó porque el proyecto no tiene
  todavía infraestructura de subdominios por tenant, y sería una decisión de
  despliegue, no de este endpoint).
- **Una sola excepción para todos los rechazos** (`InvalidCredentialsException`) en vez
  de excepciones específicas mapeadas todas a 401 en el controller: reduce el riesgo de
  que un cambio futuro filtre accidentalmente información al usar mensajes o códigos de
  error distintos por caso.
- **`Membership.reconstitute(...)` como factory nuevo**: los factories existentes
  (`createAdministrator`, `createEndCustomer`) validan invariantes de alta (campos
  obligatorios según el rol) que no aplican al leer una fila ya persistida — intentar
  reconstruir una `Membership` con rol `ADMINISTRATOR` vía `createEndCustomer` fallaría
  porque exige `firstName`/`lastName`. Mismo patrón que ya existe para `Tenant`
  (`Tenant.reconstitute`, ver `tenants/domain/model/Tenant.java` de spec 002).

## Modelo de datos

Sin cambios. No se agrega ninguna tabla ni columna: el login solo lee `memberships` y
`tenants`, ya existentes desde spec 002/003.

## Contratos

### `POST /api/tenants/{tenantId}/login`

Request:
```json
{ "email": "laura.gomez@example.com", "password": "Cliente123!" }
```

Response `200 OK`:
```json
{
  "accessToken": "<jwt>",
  "membershipId": "uuid",
  "tenantId": "travesia-natural",
  "firstName": "Laura",
  "lastName": "Gomez",
  "email": "laura.gomez@example.com",
  "role": "END_CUSTOMER"
}
```

Response `401 Unauthorized` (todos los casos de rechazo, mismo cuerpo):
```json
{ "error": "invalid_credentials", "message": "email o password incorrectos" }
```

No hay `404` para `tenantId` inexistente en este endpoint (a diferencia de los demás de
`tenants`) — ver criterios de aceptación en `spec.md`.

## Cómo se verifica

- Login exitoso: criterio 1 de `spec.md`, sección nueva de `PLAN-VERIFICACION.md`.
- Password incorrecto, email sin membership en el tenant, tenant inexistente, tenant
  `Inactivo`, membership `INACTIVA`: cada uno un `curl` en `PLAN-VERIFICACION.md`
  confirmando `401` y el mismo cuerpo de error genérico.
- `./mvnw test` en verde (spec 001, 002, 003 y cualquier test nuevo de 004).
