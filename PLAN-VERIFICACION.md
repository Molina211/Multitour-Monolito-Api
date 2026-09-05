# Plan de verificación — 001 Walking Skeleton + Reservation

Corresponde a `specs/001-walking-skeleton-reservation/`. Se completa por lotes a medida
que avanza la implementación (ver `tasks.md`).

## 1. Levantar Postgres local

```bash
docker compose up -d
docker compose ps
```

Se espera el contenedor `multitour-postgres` en estado `healthy`.

## 2. Verificar que Flyway aplicó el esquema

```bash
docker exec -it multitour-postgres psql -U multitour -d multitour -c "\dt"
docker exec -it multitour-postgres psql -U multitour -d multitour -c "\d reservations"
```

Se esperan las tablas `reservations`, `reserved_services` y `flyway_schema_history`, con
`tenant_id` como `NOT NULL` en ambas tablas de negocio.

## 4. Arrancar la aplicación

```bash
./mvnw spring-boot:run
```

## 5. Verificar `/health`

```bash
curl -i http://localhost:8080/health
```

Se espera `200 OK` y `{"status":"UP"}`.

## 6. Crear una reserva

**Nota (spec 006):** `tenantId` ya no se resuelve vía header `X-Tenant-Id`, sino en la
URL (`/api/tenants/{tenantId}/reservations`), mismo patrón que `tenants`/`catalog`. Los
pasos 6-8 de esta sección se actualizaron para reflejar el contrato vigente — ver
sección "006" más abajo para el detalle completo de la migración.

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/reservations \
  -H "Content-Type: application/json" \
  -d '{
        "customerId": "cliente-001",
        "projectedValue": 250000,
        "reservedServices": [
          { "serviceReference": "tour-laguna-verde", "partySize": 2, "scheduledDate": "2026-10-01" }
        ]
      }'
```

Se espera `201 Created` con `reservationStatus: "Pendiente de pago"` y `paymentStatus:
"Sin pago"`. Confirmar la fila con:

```bash
docker exec -it multitour-postgres psql -U multitour -d multitour -c "SELECT reservation_id, tenant_id, customer_id, reservation_status FROM reservations;"
```

## 7. Rechazo por datos incompletos

Repetir el `curl` anterior, cada vez quitando un dato distinto, y confirmar
`400 Bad Request` sin que quede fila nueva en `reservations`:

- Sin `customerId` en el body.
- Con `reservedServices: []` (lista vacía).

## 8. Aislamiento entre tenants

Repetir el `curl` del paso 6 dos veces con el mismo `customerId` pero un `{tenantId}`
distinto en la URL en cada llamada (por ejemplo `travesia-natural` y otro tenant creado
en la sección "002"). Confirmar con la consulta `psql` del paso 6 que quedan dos filas
con `tenant_id` distinto y sin ninguna relación cruzada entre ellas.

## 9. Compilación y test por defecto

```bash
./mvnw test
```

---

# Plan de verificación — 002 Tenant lifecycle

Corresponde a `specs/002-tenant-lifecycle/`. Requiere la app corriendo (paso 4 de la
sección anterior) y Postgres arriba (paso 1). Verifica cada criterio de aceptación de
`spec.md`.

## 1. Verificar el esquema V2

```bash
docker exec -it multitour-postgres psql -U multitour -d multitour -c "\dt"
docker exec -it multitour-postgres psql -U multitour -d multitour -c "\d tenants"
docker exec -it multitour-postgres psql -U multitour -d multitour -c "\d memberships"
docker exec -it multitour-postgres psql -U multitour -d multitour -c "\d audit_records"
```

Se esperan las tablas `tenants`, `memberships` y `audit_records` además de las de
spec 001, con `flyway_schema_history` mostrando `V1` y `V2` aplicadas.

## 2. Crear un tenant con su primer Administrador

```bash
curl -i -X POST http://localhost:8080/api/tenants \
  -H "Content-Type: application/json" \
  -d '{
        "tenantId": "travesia-natural",
        "commercialName": "Travesia Natural",
        "actorId": "platform-admin@multitour.dev",
        "administrator": {
          "name": "Admin Travesia",
          "email": "admin@travesia-natural.dev",
          "password": "Sup3rSecreta!",
          "passwordConfirmation": "Sup3rSecreta!"
        }
      }'
```

Se espera `201 Created` con `tenantStatus: "ACTIVO"`. Confirmar que la membresía quedó
con la contraseña hasheada (no en texto plano):

```bash
docker exec -it multitour-postgres psql -U multitour -d multitour -c "SELECT tenant_id, tenant_status FROM tenants;"
docker exec -it multitour-postgres psql -U multitour -d multitour -c "SELECT tenant_id, email, role, membership_status, password_hash FROM memberships;"
```

`password_hash` debe empezar con `$2a$` o `$2b$` (prefijo BCrypt), nunca
`Sup3rSecreta!` literal.

## 3. Rechazo por `tenantId` duplicado

Repetir el `curl` del paso 2 exactamente igual. Se espera `409 Conflict` y que la
consulta `SELECT count(*) FROM tenants WHERE tenant_id = 'travesia-natural';` siga
devolviendo `1`.

## 4. Rechazo por contraseñas que no coinciden

```bash
curl -i -X POST http://localhost:8080/api/tenants \
  -H "Content-Type: application/json" \
  -d '{
        "tenantId": "otro-tenant",
        "commercialName": "Otro Tenant",
        "actorId": "platform-admin@multitour.dev",
        "administrator": {
          "name": "Admin Otro",
          "email": "admin@otro-tenant.dev",
          "password": "Uno123!",
          "passwordConfirmation": "Distinta456!"
        }
      }'
```

Se espera `400 Bad Request` y que `otro-tenant` no quede creado (`SELECT * FROM
tenants WHERE tenant_id = 'otro-tenant';` sin filas).

## 5. Desactivar un tenant

Sin `reason`:

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/deactivate \
  -H "Content-Type: application/json" \
  -d '{ "actorId": "platform-admin@multitour.dev" }'
```

Se espera `400 Bad Request`. Con `reason`:

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/deactivate \
  -H "Content-Type: application/json" \
  -d '{ "reason": "Incumplimiento de pago", "actorId": "platform-admin@multitour.dev" }'
```

Se espera `200 OK` con `tenantStatus: "INACTIVO"`.

## 6. Reactivar el tenant

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/reactivate \
  -H "Content-Type: application/json" \
  -d '{ "reason": "Pago regularizado", "actorId": "platform-admin@multitour.dev" }'
```

Se espera `200 OK` con `tenantStatus: "ACTIVO"`.

## 7. El historial no se borra al desactivar (`INV-TEN-002`)

```bash
curl -i http://localhost:8080/api/tenants/travesia-natural
```

Debe devolver `200 OK` con los datos del tenant sin importar su estado actual (probar
también inmediatamente después del paso 5, antes de reactivar).

## 8. Auditoría de las tres acciones de ciclo de vida

```bash
curl -i http://localhost:8080/api/audit
```

Se esperan al menos tres registros para `travesia-natural`: `TENANT_CREATED`,
`TENANT_DEACTIVATED`, `TENANT_REACTIVATED`, cada uno con `actorId`, `tenantId`,
`action`, `reason` (nulo solo en `TENANT_CREATED`) y `recordedAt`. Confirmar también
por base de datos:

```bash
docker exec -it multitour-postgres psql -U multitour -d multitour -c "SELECT tenant_id, actor_id, action, reason, recorded_at FROM audit_records ORDER BY recorded_at;"
```

## 9. Listado de tenants

```bash
curl -i http://localhost:8080/api/tenants
```

Se espera `200 OK` con un array que incluye `travesia-natural`.

## 10. Compilación y tests (spec 001 + spec 002)

```bash
./mvnw test
```

Deben seguir en verde `contextLoads` y los tests de spec 001, sin regresiones.

---

# Plan de verificación — 003 End customer registration

Corresponde a `specs/003-end-customer-registration/`. Requiere la app corriendo (paso 4
de la primera sección) y Postgres arriba (paso 1). Reutiliza el tenant
`travesia-natural` creado en la sección "002 — Tenant lifecycle" (si no existe,
repetir el paso 2 de esa sección primero).

## 1. Verificar el esquema V3

```bash
docker exec -it multitour-postgres psql -U multitour -d multitour -c "\d memberships"
```

Se esperan las columnas nuevas `first_name`, `last_name`, `phone` y el índice único
`uq_memberships_tenant_email`, además de `flyway_schema_history` mostrando `V3`
aplicada.

## 2. Registrar un End Customer

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/customers \
  -H "Content-Type: application/json" \
  -d '{
        "firstName": "Laura",
        "lastName": "Gomez",
        "email": "laura.gomez@example.com",
        "phone": "3001234567",
        "password": "Cliente123!",
        "passwordConfirmation": "Cliente123!"
      }'
```

Se espera `201 Created` con `role: "END_CUSTOMER"` y `membershipStatus: "ACTIVA"`.
Confirmar que la contraseña quedó hasheada:

```bash
docker exec -it multitour-postgres psql -U multitour -d multitour -c "SELECT tenant_id, email, first_name, last_name, phone, role, password_hash FROM memberships WHERE email = 'laura.gomez@example.com';"
```

`password_hash` debe empezar con `$2a$` o `$2b$`, nunca `Cliente123!` literal.

## 3. Rechazo por email duplicado en el mismo tenant

Repetir el `curl` del paso 2 exactamente igual. Se espera `409 Conflict` y que
`SELECT count(*) FROM memberships WHERE tenant_id = 'travesia-natural' AND email =
'laura.gomez@example.com';` siga devolviendo `1`.

## 4. El mismo email en otro tenant sí se permite

Crear un segundo tenant (por ejemplo `otro-tenant`, con el paso 2 de la sección "002 —
Tenant lifecycle") y repetir el `curl` del paso 2 de esta sección apuntando a
`/api/tenants/otro-tenant/customers` con el mismo email. Se espera `201 Created`, y la
consulta `psql` debe mostrar dos filas con el mismo email y `tenant_id` distinto, sin
relación cruzada entre ellas.

## 5. Rechazo por contraseñas que no coinciden

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/customers \
  -H "Content-Type: application/json" \
  -d '{
        "firstName": "Carlos",
        "lastName": "Ruiz",
        "email": "carlos.ruiz@example.com",
        "password": "Uno123!",
        "passwordConfirmation": "Distinta456!"
      }'
```

Se espera `400 Bad Request` y que `carlos.ruiz@example.com` no quede creado.

## 6. Rechazo por política de contraseña incumplida

Repetir el `curl` del paso 2 con un email nuevo y `"password": "corta1", "passwordConfirmation": "corta1"`
(sin mayúscula ni carácter especial). Se espera `400 Bad Request` sin persistir nada.

## 7. Rechazo por tenant inexistente

```bash
curl -i -X POST http://localhost:8080/api/tenants/no-existe/customers \
  -H "Content-Type: application/json" \
  -d '{
        "firstName": "Ana",
        "lastName": "Diaz",
        "email": "ana.diaz@example.com",
        "password": "Cliente123!",
        "passwordConfirmation": "Cliente123!"
      }'
```

Se espera `404 Not Found`.

## 8. Rechazo por tenant `Inactivo`

Desactivar `travesia-natural` (paso 5 de la sección "002 — Tenant lifecycle") y
repetir el `curl` del paso 2 de esta sección con un email nuevo. Se espera `409
Conflict` sin persistir nada. Reactivar el tenant al terminar (paso 6 de esa sección)
para no dejar el ambiente en un estado inconsistente para pruebas futuras.

## 9. Compilación y tests (spec 001 + spec 002 + spec 003)

```bash
./mvnw test
```

Deben seguir en verde `contextLoads` y los tests existentes, sin regresiones.

---

# Plan de verificación — 004 End customer login

Corresponde a `specs/004-end-customer-login/`. Requiere la app corriendo (paso 4 de la
primera sección) y Postgres arriba (paso 1). Reutiliza el tenant `travesia-natural` y el
cliente `laura.gomez@example.com` creados en la sección "003 — End customer
registration" (si no existen, repetir esos pasos primero).

Nota: este endpoint **no** se puede invocar hoy desde `login.component.ts` del Frontend
(no tiene campo de tenant) — ver el comentario extenso en `AuthController.java`. Toda
esta verificación es directa contra el Backend.

## 1. Login exitoso

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/login \
  -H "Content-Type: application/json" \
  -d '{ "email": "laura.gomez@example.com", "password": "Cliente123!" }'
```

Se espera `200 OK` con `accessToken` (JWT), `tenantId: "travesia-natural"`,
`role: "END_CUSTOMER"`, y sin `passwordHash` en ningún lado de la respuesta. Decodificar
el JWT (por ejemplo en https://jwt.io o `echo "<payload-base64>" | base64 -d`, usando
solo la parte del medio del token) y confirmar que trae `sub`, `tenantId`, `email`,
`role`, `iat` y `exp`.

## 2. Rechazo por password incorrecto

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/login \
  -H "Content-Type: application/json" \
  -d '{ "email": "laura.gomez@example.com", "password": "Incorrecta1!" }'
```

Se espera `401 Unauthorized` con `{"error":"invalid_credentials", ...}`.

## 3. Rechazo por email sin membership en ese tenant (incluye email de otro tenant)

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/login \
  -H "Content-Type: application/json" \
  -d '{ "email": "no-existe@example.com", "password": "Cliente123!" }'
```

Se espera `401 Unauthorized`, mismo cuerpo genérico que el paso 2 (sin diferencia
observable entre "no existe" y "password incorrecto").

## 4. Rechazo por `tenantId` inexistente (no debe ser 404)

```bash
curl -i -X POST http://localhost:8080/api/tenants/no-existe/login \
  -H "Content-Type: application/json" \
  -d '{ "email": "laura.gomez@example.com", "password": "Cliente123!" }'
```

Se espera `401 Unauthorized`, el mismo cuerpo genérico — nunca `404` (evita enumerar
tenants válidos, ver criterios de aceptación de `spec.md`).

## 5. Rechazo por tenant `Inactivo`

Desactivar `travesia-natural` (paso 5 de la sección "002 — Tenant lifecycle") y repetir
el `curl` del paso 1 de esta sección. Se espera `401 Unauthorized` con el mismo cuerpo
genérico. Reactivar el tenant al terminar (paso 6 de esa sección).

## 6. Rechazo por membership `INACTIVA`

No hay todavía un endpoint para desactivar una membership individual (fuera de alcance
de spec 002/003/004) — este paso queda pendiente de una spec futura que agregue esa
capacidad; de momento se puede simular manualmente por base de datos, solo en el
ambiente local de verificación:

```bash
docker exec -it multitour-postgres psql -U multitour -d multitour -c "UPDATE memberships SET membership_status = 'INACTIVA' WHERE email = 'laura.gomez@example.com';"
```

Repetir el `curl` del paso 1: se espera `401 Unauthorized` con el mismo cuerpo genérico.
Revertir el cambio para no dejar el ambiente inconsistente:

```bash
docker exec -it multitour-postgres psql -U multitour -d multitour -c "UPDATE memberships SET membership_status = 'ACTIVA' WHERE email = 'laura.gomez@example.com';"
```

## 7. Compilación y tests (spec 001 + spec 002 + spec 003 + spec 004)

```bash
./mvnw test
```

Deben seguir en verde `contextLoads` y los tests existentes, sin regresiones.

---

# Plan de verificación — 005 Catálogo operativo

Corresponde a `specs/005-catalog-item-management/`. Requiere la app corriendo (paso 4
de la primera sección) y Postgres arriba (paso 1). Reutiliza el tenant
`travesia-natural` creado en la sección "002 — Tenant lifecycle" (debe estar `Activo`
al iniciar; si quedó `Inactivo` de una verificación anterior, reactivarlo primero).

Nota: este bloque **no** se puede invocar hoy desde `operator/catalog` ni desde
`manage-catalog`/`manage-lodging`/`manage-food` del Frontend (siguen simulando todo en
`localStorage` vía `OperatorCatalogService`). Toda esta verificación es directa contra
el Backend.

## 1. Crear un ítem válido de cada tipo (`201`)

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/catalog-items \
  -H "Content-Type: application/json" \
  -d '{ "type": "TOUR", "name": "Caminata Cocora", "price": 120000 }'

curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/catalog-items \
  -H "Content-Type: application/json" \
  -d '{ "type": "LODGING", "name": "Cabaña El Roble", "price": 250000, "capacity": 4 }'

curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/catalog-items \
  -H "Content-Type: application/json" \
  -d '{ "type": "FOOD", "name": "Menú campesino", "price": 35000 }'
```

Se espera `201 Created` en los tres, cada uno con `active: true`. Guardar el
`catalogItemId` del `LODGING` (se reutiliza en los pasos 4-7).

## 2. Rechazo de `LODGING` sin capacidad válida (`400`)

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/catalog-items \
  -H "Content-Type: application/json" \
  -d '{ "type": "LODGING", "name": "Sin capacidad", "price": 100000 }'

curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/catalog-items \
  -H "Content-Type: application/json" \
  -d '{ "type": "LODGING", "name": "Capacidad cero", "price": 100000, "capacity": 0 }'
```

Se espera `400 Bad Request` en ambos, `{"error":"validation_error", ...}`, y ninguna
fila nueva persistida (verificar con el paso 3 que la lista no creció).

## 3. Listado del tenant (`200`)

```bash
curl -i http://localhost:8080/api/tenants/travesia-natural/catalog-items
```

Se espera `200 OK` con los 3 ítems creados en el paso 1 (y ninguno de los rechazados
en el paso 2).

## 4. Aislamiento entre tenants (`404`)

Usando el `catalogItemId` del `LODGING` del paso 1, consultarlo bajo un tenant distinto
(por ejemplo, el tenant `otro-operador` creado en la sección "002" si existe; si no,
crear uno temporal con `POST /api/tenants`):

```bash
curl -i http://localhost:8080/api/tenants/otro-operador/catalog-items/<catalogItemId-del-LODGING>
```

Se espera `404 Not Found` — nunca revela el ítem de otro tenant.

## 5. Actualización parcial (`200`)

```bash
curl -i -X PATCH http://localhost:8080/api/tenants/travesia-natural/catalog-items/<catalogItemId-del-LODGING> \
  -H "Content-Type: application/json" \
  -d '{ "price": 270000 }'
```

Se espera `200 OK` con `price: 270000` y el resto de los campos (`name`, `capacity`,
etc.) sin cambios.

## 6. Desactivar sin borrar (`200`, trazabilidad histórica)

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/catalog-items/<catalogItemId-del-LODGING>/deactivate

curl -i http://localhost:8080/api/tenants/travesia-natural/catalog-items/<catalogItemId-del-LODGING>
```

El primer `curl` espera `200 OK` con `active: false`; el segundo (`GET` directo)
confirma que la fila sigue existiendo y siendo consultable, solo con `active: false`.

## 7. Reactivar (`200`)

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/catalog-items/<catalogItemId-del-LODGING>/reactivate
```

Se espera `200 OK` con `active: true`.

## 8. Tenant inexistente (`404`) y tenant `Inactivo` (`409`)

```bash
curl -i http://localhost:8080/api/tenants/no-existe/catalog-items
```

Se espera `404 Not Found`. Luego, desactivar `travesia-natural` (paso 5 de la sección
"002 — Tenant lifecycle") y repetir la creación del paso 1:

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/catalog-items \
  -H "Content-Type: application/json" \
  -d '{ "type": "TOUR", "name": "No debería crearse", "price": 50000 }'
```

Se espera `409 Conflict` con `{"error":"tenant_inactive", ...}`. Reactivar el tenant al
terminar (paso 6 de esa sección) para no dejar el ambiente inconsistente.

## 9. Compilación y tests (spec 001 + spec 002 + spec 003 + spec 004 + spec 005)

```bash
./mvnw test
```

Deben seguir en verde `contextLoads` y los tests existentes, sin regresiones.

---

# Plan de verificación — 006 Consulta de reservas + alineación de `tenantId`

Corresponde a `specs/006-reservation-query/`. Requiere haber corrido el paso 1
("Levantar Postgres local") y tener al menos un tenant `Activo` de la sección "002"
(`travesia-natural`).

**Nota importante:** `V5__alter_reservations_tenant_id.sql` recrea las tablas
`reservations`/`reserved_services` desde cero (ver `plan.md`, "Decisiones técnicas",
riesgo 1). Cualquier reserva creada antes de esta migración (por ejemplo, en la sección
"001") se pierde al aplicar `V5`. Es un costo aceptado de datos de prueba, no de
producción.

**Nota (spec 007):** desde spec 007, `POST .../reservations` exige un header
`Authorization: Bearer <token>` y ya no acepta `customerId` en el body. Los `curl` de
los pasos 2 y 4 de esta sección quedaron desactualizados por eso — ver la sección "007"
más abajo para la versión vigente con autenticación. Los pasos 1, 3, 5, 6, 7 y 8 de esta
sección no cambian.

## 1. Arrancar la aplicación y confirmar la migración V5

```bash
./mvnw spring-boot:run
```

En el log de arranque debe verse `Successfully applied 1 migration` hacia
`5 - alter reservations tenant id` (o `Schema "public" up to date` si ya se había
aplicado antes).

## 2. Crear una reserva con `tenantId` en la URL (`201`)

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/reservations \
  -H "Content-Type: application/json" \
  -d '{
        "customerId": "cliente-006",
        "projectedValue": 300000,
        "reservedServices": [
          { "serviceReference": "tour-laguna-verde", "partySize": 2, "scheduledDate": "2026-10-15" }
        ]
      }'
```

Se espera `201 Created`, con `tenantId: "travesia-natural"` (string, no UUID) y
`reservedServices` en la respuesta. Guardar el `reservationId` devuelto para los pasos
siguientes.

## 3. El header `X-Tenant-Id` y la ruta vieja ya no existen

```bash
curl -i -X POST http://localhost:8080/api/reservations \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 11111111-1111-1111-1111-111111111111" \
  -d '{ "customerId": "cliente-x", "projectedValue": 1000, "reservedServices": [] }'
```

Se espera `404 Not Found` (ninguna ruta mapeada en `/api/reservations`), confirmando que
el contrato viejo fue reemplazado, no duplicado.

## 4. Tenant inexistente (`404`) y tenant `Inactivo` (`409`) al crear

```bash
curl -i -X POST http://localhost:8080/api/tenants/no-existe/reservations \
  -H "Content-Type: application/json" \
  -d '{ "customerId": "cliente-006", "projectedValue": 1000, "reservedServices": [{ "serviceReference": "x" }] }'
```

Se espera `404 Not Found`. Luego, desactivar `travesia-natural` (paso 5 de la sección
"002") y repetir el `curl` del paso 2: se espera `409 Conflict` con
`{"error":"tenant_inactive", ...}`. Reactivar el tenant al terminar (paso 6 de esa
sección).

## 5. Listar reservas del tenant

```bash
curl -i http://localhost:8080/api/tenants/travesia-natural/reservations
```

Se espera `200 OK` con un arreglo que incluye la reserva creada en el paso 2.

## 6. Detalle de una reserva

```bash
curl -i http://localhost:8080/api/tenants/travesia-natural/reservations/<reservationId-del-paso-2>
```

Se espera `200 OK` con los mismos campos del listado, incluidos `reservedServices` con
`serviceReference`, `partySize` y `scheduledDate`.

## 7. Aislamiento entre tenants (`404`, no 200 con datos ajenos)

Crear una segunda reserva bajo otro tenant `Activo` (por ejemplo, uno creado en la
sección "002"), y luego pedir su `reservationId` bajo `travesia-natural`:

```bash
curl -i http://localhost:8080/api/tenants/travesia-natural/reservations/<reservationId-del-otro-tenant>
```

Se espera `404 Not Found` (no filtra existencia cruzada).

## 8. Compilación y tests (spec 001 + spec 002 + spec 003 + spec 004 + spec 005 + spec 006)

```bash
./mvnw test
```

Deben seguir en verde `contextLoads` y los tests existentes, sin regresiones.

---

# Plan de verificación — 007 Enforcement de JWT en creación de reservas

Corresponde a `specs/007-jwt-enforcement/`. Requiere Postgres arriba (paso 1), la app
corriendo (paso 4 de la primera sección), el tenant `travesia-natural` `Activo`, y el
cliente `laura.gomez@example.com` / `Cliente123!` (creado en la sección "003").

**Nota:** `login.component.ts` del Frontend no guarda el JWT ni lo reenvía en llamadas
posteriores (mismo hueco documentado en spec 004 y en el comentario extenso de
`ReservationController.java`) — esta verificación obtiene el token a mano con `curl` y
lo reenvía manualmente, no hay integración real con el Frontend todavía.

## 1. Obtener un token válido

```bash
curl -s -X POST http://localhost:8080/api/tenants/travesia-natural/login \
  -H "Content-Type: application/json" \
  -d '{ "email": "laura.gomez@example.com", "password": "Cliente123!" }'
```

Guardar el `accessToken` de la respuesta en una variable para los pasos siguientes:

```bash
TOKEN="<accessToken del paso anterior>"
```

## 2. Crear una reserva sin header `Authorization` (`401`)

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/reservations \
  -H "Content-Type: application/json" \
  -d '{ "projectedValue": 300000, "reservedServices": [{ "serviceReference": "tour-laguna-verde" }] }'
```

Se espera `401 Unauthorized` con `{"error":"unauthorized", ...}`.

## 3. Crear una reserva con un token con firma alterada (`401`)

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/reservations \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}x" \
  -d '{ "projectedValue": 300000, "reservedServices": [{ "serviceReference": "tour-laguna-verde" }] }'
```

Se espera `401 Unauthorized` (firma inválida al alterar un carácter del token).

## 4. Crear una reserva con un token válido de otro tenant (`403`)

Requiere un segundo tenant `Activo` con su propio Administrator/Membership (por ejemplo,
uno creado en la sección "002"), y un login exitoso contra ese otro tenant para obtener
un segundo token. Con ese segundo token, llamar a la URL de `travesia-natural`:

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/reservations \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token-del-otro-tenant>" \
  -d '{ "projectedValue": 300000, "reservedServices": [{ "serviceReference": "tour-laguna-verde" }] }'
```

Se espera `403 Forbidden` con `{"error":"tenant_mismatch", ...}`. Nota: por el mismo
motivo, este endpoint ya no puede usarse para comprobar el `404` de "tenant inexistente"
de spec 006 — ver `plan.md`, "Decisiones técnicas".

## 5. Crear una reserva con un token válido del mismo tenant (`201`, `customerId` del token)

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/reservations \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{ "projectedValue": 300000, "reservedServices": [{ "serviceReference": "tour-laguna-verde", "partySize": 2, "scheduledDate": "2026-10-15" }] }'
```

Se espera `201 Created`. El `customerId` de la respuesta debe ser igual al `membershipId`
(claim `sub`) de `laura.gomez@example.com` decodificado del token del paso 1 — nunca un
valor inventado, porque el body ya no lleva `customerId`.

## 6. Tenant `Inactivo` con un token emitido antes de desactivarlo (`409`)

Con el mismo `TOKEN` del paso 1 (emitido cuando `travesia-natural` estaba `Activo`),
desactivar el tenant (paso 5 de la sección "002") y repetir el `curl` del paso 5 de esta
sección: se espera `409 Conflict` con `{"error":"tenant_inactive", ...}`. Reactivar el
tenant al terminar (paso 6 de esa sección).

## 7. Control: un endpoint no protegido sigue respondiendo igual sin token

```bash
curl -i http://localhost:8080/api/tenants
```

Se espera `200 OK`, igual que antes de esta spec — confirma que el resto de la API no
quedó bloqueada por el nuevo filtro.

## 8. Compilación y tests (spec 001 + spec 002 + spec 003 + spec 004 + spec 005 + spec 006 + spec 007)

```bash
./mvnw test
```

Deben seguir en verde `contextLoads` y los tests existentes, sin regresiones.

---

# Plan de verificación — 008 Gestión de descuentos operativos (catálogo)

Corresponde a `specs/008-discount-management/`. Requiere la app corriendo (paso 4 de la
primera sección) y Postgres arriba (paso 1). Reutiliza el tenant `travesia-natural`
(debe estar `Activo`) y al menos un `catalogItemId` de tipo `TOUR` creado en la sección
"005 — Catálogo operativo" (guardar ese id como `<catalogItemId>` para los pasos
siguientes; si no existe, repetir el paso 1 de esa sección primero).

Nota: este bloque **no** se puede invocar hoy desde `operator/discounts`,
`new-discount`/`edit-discount` del Frontend (siguen simulando todo en `localStorage` vía
`operator-discount.service.ts`). Toda esta verificación es directa contra el Backend.

## 1. Verificar el esquema V6

```bash
docker exec -it multitour-postgres psql -U multitour -d multitour -c "\d discounts"
```

Se esperan las columnas `discount_id`, `tenant_id`, `catalog_item_id`, `percentage`,
`valid_from`, `valid_to`, `priority`, `stackable`, `cap`, `base`, `active`,
`created_at`, y `flyway_schema_history` mostrando `V6` aplicada.

## 2. Crear un descuento válido (`201`)

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/discounts \
  -H "Content-Type: application/json" \
  -d '{
        "catalogItemId": "<catalogItemId>",
        "percentage": 15,
        "validFrom": "2026-09-01",
        "validTo": "2026-12-31",
        "priority": 1,
        "stackable": false,
        "base": "original"
      }'
```

Se espera `201 Created` con `active: true` y `base: "original"`. Guardar el
`discountId` devuelto para los pasos siguientes.

## 3. Rechazo por `catalogItemId` inexistente o de otro tenant (`404`)

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/discounts \
  -H "Content-Type: application/json" \
  -d '{ "catalogItemId": "00000000-0000-0000-0000-000000000000", "percentage": 10, "base": "original" }'
```

Se espera `404 Not Found`.

## 4. Rechazo por datos inválidos (`400`)

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/discounts \
  -H "Content-Type: application/json" \
  -d '{ "catalogItemId": "<catalogItemId>", "percentage": 0, "base": "original" }'

curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/discounts \
  -H "Content-Type: application/json" \
  -d '{ "catalogItemId": "<catalogItemId>", "percentage": 150, "base": "original" }'

curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/discounts \
  -H "Content-Type: application/json" \
  -d '{ "catalogItemId": "<catalogItemId>", "percentage": 10, "validFrom": "2026-12-31", "validTo": "2026-01-01", "base": "original" }'
```

Se espera `400 Bad Request` en los tres, `{"error":"validation_error", ...}`, sin fila
nueva persistida.

## 5. Segundo descuento solapado sobre el mismo `catalogItemId` (`201`, no se bloquea)

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/discounts \
  -H "Content-Type: application/json" \
  -d '{
        "catalogItemId": "<catalogItemId>",
        "percentage": 20,
        "validFrom": "2026-10-01",
        "validTo": "2026-10-31",
        "priority": 2,
        "stackable": true,
        "base": "subtotal"
      }'
```

Se espera `201 Created` a pesar de que su vigencia se solapa con el descuento del paso 2
sobre el mismo `catalogItemId` — confirma el criterio de aceptación "no se rechaza por
solape" (RF-005A/RF-005B, ver `spec.md`).

## 6. Listado del tenant (`200`)

```bash
curl -i http://localhost:8080/api/tenants/travesia-natural/discounts
```

Se espera `200 OK` con los dos descuentos creados en los pasos 2 y 5, y ninguno de los
rechazados en los pasos 3-4.

## 7. Obtener por id y aislamiento entre tenants (`404`)

```bash
curl -i http://localhost:8080/api/tenants/travesia-natural/discounts/<discountId-del-paso-2>
curl -i http://localhost:8080/api/tenants/otro-operador/discounts/<discountId-del-paso-2>
```

El primero espera `200 OK`; el segundo (bajo otro tenant) espera `404 Not Found` — nunca
revela el descuento de otro tenant.

## 8. Actualización parcial (`200`)

```bash
curl -i -X PATCH http://localhost:8080/api/tenants/travesia-natural/discounts/<discountId-del-paso-2> \
  -H "Content-Type: application/json" \
  -d '{ "percentage": 25 }'
```

Se espera `200 OK` con `percentage: 25` y el resto de los campos (`validFrom`,
`priority`, `base`, etc.) sin cambios.

## 9. Desactivar y reactivar sin borrar (`200`, trazabilidad histórica)

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/discounts/<discountId-del-paso-2>/deactivate
curl -i http://localhost:8080/api/tenants/travesia-natural/discounts/<discountId-del-paso-2>

curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/discounts/<discountId-del-paso-2>/reactivate
```

El primer `curl` espera `200 OK` con `active: false`; el segundo (`GET` directo)
confirma que la fila sigue existiendo y siendo consultable; el tercero espera
`200 OK` con `active: true`.

## 10. Tenant inexistente (`404`) y tenant `Inactivo` (`409`)

```bash
curl -i http://localhost:8080/api/tenants/no-existe/discounts
```

Se espera `404 Not Found`. Luego, desactivar `travesia-natural` (paso 5 de la sección
"002 — Tenant lifecycle") y repetir la creación del paso 2:

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/discounts \
  -H "Content-Type: application/json" \
  -d '{ "catalogItemId": "<catalogItemId>", "percentage": 10, "base": "original" }'
```

Se espera `409 Conflict` con `{"error":"tenant_inactive", ...}`. Reactivar el tenant al
terminar (paso 6 de esa sección) para no dejar el ambiente inconsistente.

## 11. Compilación y tests (spec 001 a spec 008)

```bash
./mvnw test
```

Deben seguir en verde `contextLoads` y los tests existentes, sin regresiones.

---

# Plan de verificación — 009 Registro de pago sobre una reserva

Corresponde a `specs/009-reservation-payment/`. Requiere Postgres arriba (paso 1), la
app corriendo (paso 4 de la primera sección), el tenant `travesia-natural` `Activo`, y
un token válido de `laura.gomez@example.com` (sección "007", pasos 1-2) para crear
reservas nuevas. Cada escenario usa su propia reserva porque los pagos son irreversibles
sobre el mismo agregado.

```bash
TOKEN="<accessToken de la sección 007, paso 1>"
```

## 1. Efectivo que cubre exactamente el saldo (`200`, `Confirmada`)

```bash
curl -s -X POST http://localhost:8080/api/tenants/travesia-natural/reservations \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{ "projectedValue": 300000, "reservedServices": [{ "serviceReference": "tour-laguna-verde" }] }'
```

Guardar el `reservationId` de la respuesta como `RES_A`, luego:

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/reservations/${RES_A}/payments \
  -H "Content-Type: application/json" \
  -d '{ "method": "EFECTIVO", "amount": 300000 }'
```

Se espera `200 OK` con `paymentStatus: "Pagado"`, `reservationStatus: "Confirmada"`,
`pendingBalance: 0`.

## 2. Efectivo insuficiente (`400`, sin cambios)

Repetir la creación del paso 1 para obtener `RES_B` (`projectedValue: 300000`), luego:

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/reservations/${RES_B}/payments \
  -H "Content-Type: application/json" \
  -d '{ "method": "EFECTIVO", "amount": 200000 }'
```

Se espera `400 Bad Request`. Confirmar con
`curl -s http://localhost:8080/api/tenants/travesia-natural/reservations/${RES_B}` que
`pendingBalance` sigue en `300000` y `paymentStatus` en `"Sin pago"`.

## 3. Abono parcial y abono que completa (`200` en ambos, `Confirmada` al completar)

Crear `RES_C` (`projectedValue: 300000`), luego:

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/reservations/${RES_C}/payments \
  -H "Content-Type: application/json" \
  -d '{ "method": "ABONO", "amount": 100000 }'
```

Se espera `200 OK`, `paymentStatus: "Parcial"`, `pendingBalance: 200000`,
`reservationStatus` sigue `"Pendiente de pago"`. Luego:

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/reservations/${RES_C}/payments \
  -H "Content-Type: application/json" \
  -d '{ "method": "ABONO", "amount": 200000 }'
```

Se espera `200 OK`, `paymentStatus: "Pagado"`, `reservationStatus: "Confirmada"`,
`pendingBalance: 0`.

## 4. Transferencia: registro, aprobación y doble decisión (`200`, `200`, `409`)

Crear `RES_D` (`projectedValue: 300000`), luego:

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/reservations/${RES_D}/payments \
  -H "Content-Type: application/json" \
  -d '{ "method": "TRANSFERENCIA", "amount": 300000, "supportReference": "comprobante-001.png" }'
```

Se espera `200 OK`, `paymentStatus: "En validacion"`, `pendingBalance` sigue en `300000`
(no se aplica todavía). Luego, aprobar sin `reason` (`400`):

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/reservations/${RES_D}/payments/decide-support \
  -H "Content-Type: application/json" \
  -d '{ "decision": "APPROVE", "actorId": "admin" }'
```

Se espera `400 Bad Request`. Ahora con `reason`:

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/reservations/${RES_D}/payments/decide-support \
  -H "Content-Type: application/json" \
  -d '{ "decision": "APPROVE", "reason": "Comprobante verificado en el banco", "actorId": "admin" }'
```

Se espera `200 OK`, `paymentStatus: "Pagado"`, `reservationStatus: "Confirmada"`,
`pendingBalance: 0` — igual que un Abono. Repetir el mismo `curl` una segunda vez: se
espera `409 Conflict` con `{"error":"payment_already_resolved", ...}`.

## 5. Transferencia rechazada (`200`, sin tocar el saldo)

Crear `RES_E` (`projectedValue: 300000`) y registrar una transferencia igual que en el
paso 4. Luego:

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/reservations/${RES_E}/payments/decide-support \
  -H "Content-Type: application/json" \
  -d '{ "decision": "REJECT", "reason": "Comprobante ilegible", "actorId": "admin" }'
```

Se espera `200 OK`, `paymentStatus: "Rechazado"`, `pendingBalance` sigue en `300000`.
Repetir el mismo `curl`: se espera `409 Conflict`.

## 6. Listado de soportes pendientes por tenant (`200`, aislado por tenant)

Crear `RES_F` (`projectedValue: 300000`) y registrar una transferencia sin decidirla
(igual que el paso 4, sin llamar a `decide-support`). Luego:

```bash
curl -s http://localhost:8080/api/tenants/travesia-natural/reservations/pending-support
```

Se espera `200 OK` con exactamente `RES_F` en la lista (`RES_D` y `RES_E` ya quedaron
resueltas en los pasos 4-5). Confirmar aislamiento con un segundo tenant `Activo`
(por ejemplo el creado en la sección "002"): su `GET .../pending-support` no debe
incluir `RES_F`.

## 7. Nota de seguimiento (`201`, consultable en orden cronológico)

Sobre `RES_C` (saldo ya en `0` tras el paso 3 — el criterio no exige saldo pendiente
para registrar la nota, solo que no cambie ningún estado):

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/reservations/${RES_C}/payments/followups \
  -H "Content-Type: application/json" \
  -d '{ "note": "Cliente confirma que el comprobante llega por correo", "actorId": "admin" }'
```

Se espera `201 Created`. Luego:

```bash
curl -s http://localhost:8080/api/tenants/travesia-natural/reservations/${RES_C}/payments/followups
```

Se espera `200 OK` con la nota del paso anterior, y `paymentStatus`/`reservationStatus`
de `RES_C` sin cambios respecto al paso 3.

## 8. Tenant inexistente (`404`) y tenant `Inactivo` (`409`)

```bash
curl -i -X POST http://localhost:8080/api/tenants/no-existe/reservations/${RES_C}/payments \
  -H "Content-Type: application/json" \
  -d '{ "method": "ABONO", "amount": 1000 }'
```

Se espera `404 Not Found`. Luego, desactivar `travesia-natural` (paso 5 de la sección
"002") y repetir cualquier operación de pago sobre `RES_C`: se espera
`409 Conflict` con `{"error":"tenant_inactive", ...}`. Reactivar el tenant al terminar
(paso 6 de esa sección).

## 9. Compilación y tests (spec 001 a spec 009)

```bash
./mvnw test
```

Deben seguir en verde `contextLoads` y los tests existentes, sin regresiones.

# Plan de verificación — 010 Ejecución de reservas y costos operacionales

Corresponde a `specs/010-execution-and-operational-costs/`. Requiere Postgres arriba, la
app corriendo, el tenant `travesia-natural` `Activo`, y un token válido de
`laura.gomez@example.com` (sección "007", pasos 1-2) para crear reservas nuevas. Cada
reserva usada se paga primero en efectivo (igual que sección "009", paso 1) para
llevarla a `Confirmada`, único estado desde el que se puede iniciar ejecución.

```bash
TOKEN="<accessToken de la sección 007, paso 1>"
```

## 1. Registrar ejecución prestada sobre una reserva `Confirmada` (`200`)

Crear una reserva y pagarla en efectivo (igual que sección "009", paso 1) para obtener
`RES_G` en estado `Confirmada`:

```bash
curl -s -X POST http://localhost:8080/api/tenants/travesia-natural/reservations \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{ "projectedValue": 300000, "reservedServices": [{ "serviceReference": "tour-laguna-verde" }] }'
```

Guardar el `reservationId` como `RES_G`, pagarla:

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/reservations/${RES_G}/payments \
  -H "Content-Type: application/json" \
  -d '{ "method": "EFECTIVO", "amount": 300000 }'
```

Y registrar la ejecución:

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/reservations/${RES_G}/execution \
  -H "Content-Type: application/json" \
  -d '{ "served": true, "executed": 4, "actorId": "guia-1" }'
```

Se espera `200 OK` con `served: true`, `executed: 4`, `causal: null`. Confirmar con
`curl -s http://localhost:8080/api/tenants/travesia-natural/reservations/${RES_G}` que
`reservationStatus` pasó a `"En ejecucion"`.

## 2. Ejecución no prestada: sin causal `400`, con causal `200`

Crear y pagar `RES_H` igual que en el paso 1. Luego, sin `causal`:

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/reservations/${RES_H}/execution \
  -H "Content-Type: application/json" \
  -d '{ "served": false, "actorId": "guia-1" }'
```

Se espera `400 Bad Request` con `{"error":"validation_error", ...}`. Ahora con
`causal`:

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/reservations/${RES_H}/execution \
  -H "Content-Type: application/json" \
  -d '{ "served": false, "causal": "Cliente no se presento", "actorId": "guia-1" }'
```

Se espera `200 OK` con `served: false`, `executed: null`,
`causal: "Cliente no se presento"`, y `reservationStatus: "En ejecucion"` (misma
transición que si se hubiera prestado).

## 3. Ejecución sobre una reserva no `Confirmada` (`409`)

Crear `RES_I` sin pagarla (queda `Pendiente de pago`):

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/reservations/${RES_I}/execution \
  -H "Content-Type: application/json" \
  -d '{ "served": true, "executed": 2, "actorId": "guia-1" }'
```

Se espera `409 Conflict` con `{"error":"reservation_not_executable", ...}`, y la reserva
sigue `Pendiente de pago` sin cambios.

## 4. Segunda ejecución sobre una reserva ya ejecutada (`409`)

Repetir sobre `RES_G` (ya ejecutada en el paso 1) el mismo `curl` del paso 1: se espera
`409 Conflict` con `{"error":"reservation_not_executable", ...}`.

## 5. Consultar la ejecución registrada de una reserva (`200`)

```bash
curl -s http://localhost:8080/api/tenants/travesia-natural/reservations/${RES_G}/execution
```

Se espera `200 OK` con los mismos datos guardados en el paso 1
(`served`, `executed`, `causal`, `actorId`, `recordedAt`).

## 6. Listado de reservas pendientes de ejecución (`200`, aislado por tenant)

Crear y pagar `RES_J` igual que en el paso 1, pero sin ejecutarla. Luego:

```bash
curl -s http://localhost:8080/api/tenants/travesia-natural/reservations/pending-execution
```

Se espera `200 OK` con exactamente `RES_J` en la lista: `RES_G` y `RES_H` ya pasaron a
`En ejecucion` (pasos 1-2) y `RES_I` sigue `Pendiente de pago` (paso 3), ninguna de las
dos aplica. Confirmar aislamiento con un segundo tenant `Activo` (por ejemplo el creado
en la sección "002"): su `GET .../pending-execution` no debe incluir `RES_J`.

## 7. Costo operacional sin ejecución iniciada (`409`)

Sobre `RES_J` (`Confirmada`, sin ejecución todavía):

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/reservations/${RES_J}/costs \
  -H "Content-Type: application/json" \
  -d '{ "concept": "Combustible", "amount": 50000, "actorId": "guia-1" }'
```

Se espera `409 Conflict` con `{"error":"execution_not_started", ...}`.

## 8. Costo operacional con concepto/monto inválido (`400`)

Sobre `RES_G` (ya en `En ejecucion` desde el paso 1):

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/reservations/${RES_G}/costs \
  -H "Content-Type: application/json" \
  -d '{ "concept": "", "amount": 50000, "actorId": "guia-1" }'
```

Se espera `400 Bad Request`. Repetir con `"amount": 0` y concepto no vacío: también
`400 Bad Request`.

## 9. Costo operacional válido y segundo costo acumulado (`201`)

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/reservations/${RES_G}/costs \
  -H "Content-Type: application/json" \
  -d '{ "concept": "Combustible", "amount": 50000, "actorId": "guia-1" }'
```

Se espera `201 Created`. Luego un segundo costo:

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/reservations/${RES_G}/costs \
  -H "Content-Type: application/json" \
  -d '{ "concept": "Almuerzo del grupo", "amount": 30000, "actorId": "guia-1" }'
```

Se espera `201 Created`, sin reemplazar el costo anterior.

## 10. Listado de costos en orden cronológico (`200`)

```bash
curl -s http://localhost:8080/api/tenants/travesia-natural/reservations/${RES_G}/costs
```

Se espera `200 OK` con los dos costos del paso 9, en el orden en que se registraron
(`Combustible` antes que `Almuerzo del grupo`).

## 11. Tenant inexistente (`404`) y tenant `Inactivo` (`409`)

```bash
curl -i -X POST http://localhost:8080/api/tenants/no-existe/reservations/${RES_G}/execution \
  -H "Content-Type: application/json" \
  -d '{ "served": true, "executed": 1, "actorId": "guia-1" }'
```

Se espera `404 Not Found`. Luego, desactivar `travesia-natural` (paso 5 de la sección
"002") y repetir cualquier operación de ejecución/costos: se espera `409 Conflict` con
`{"error":"tenant_inactive", ...}`. Reactivar el tenant al terminar (paso 6 de esa
sección).

## 12. Compilación y tests (spec 001 a spec 010)

```bash
./mvnw test
```

Deben seguir en verde `contextLoads` y los tests existentes, sin regresiones.

## Hallazgos de esta verificación

La ejecución manual de los pasos 1-11 encontró tres fallos reales, corregidos antes de
cerrar la spec:

1. `RegisterExecutionService` validaba `Execution.create(...)` (que puede lanzar
   `IllegalArgumentException`) *después* de guardar la transición de la reserva a
   `EN_EJECUCION`. Una ejecución no prestada sin causal (paso 2) devolvía `400` pero
   dejaba la reserva corrompida en `EN_EJECUCION`. Se reordenó: validar primero,
   mutar/guardar después.
2. La migración `V8` declaró `executed INTEGER NOT NULL`, pero el dominio fija
   `executed = null` cuando `served = false`. Causaba `500` al registrar una ejecución
   no prestada. Se agregó `V9__make_execution_executed_nullable.sql` (no se edita `V8`
   ya aplicada).
3. El guardado de `Reservation` y de `Execution` no estaba en una única transacción: si
   el segundo guardado fallaba (como en el bug 2), la reserva quedaba en
   `EN_EJECUCION` sin ejecución asociada, sin forma de reintentar. Se agregó
   `@Transactional` a `RegisterExecutionService.registerExecution()`.

# Plan de verificación — 011 Cancelación de reserva antes de ejecución

Corresponde a `specs/011-reservation-cancellation/`. Requiere Postgres arriba, la app
corriendo, el tenant `travesia-natural` `Activo`, y un token válido de
`laura.gomez@example.com` (sección "007", pasos 1-2) para crear reservas nuevas.

```bash
TOKEN="<accessToken de la sección 007, paso 1>"
```

## 1. Cancelar una reserva `Pendiente de pago` sin pagos (`200`)

```bash
curl -s -X POST http://localhost:8080/api/tenants/travesia-natural/reservations \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{ "projectedValue": 200000, "reservedServices": [{ "serviceReference": "tour-laguna-verde" }] }'
```

Guardar el `reservationId` como `RES_K`, sin registrar ningún pago. Cancelarla:

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/reservations/${RES_K}/cancel \
  -H "Content-Type: application/json" \
  -d '{ "reason": "Cliente desistio del viaje", "actorId": "operador-1" }'
```

Se espera `200 OK` con `reservationStatus: "Cancelada"`, `creditBalance: 0` y
`paymentStatus: "Sin pago"` (sin cambios).

## 2. Cancelar una reserva `Confirmada` con el valor completo pagado (`200`, saldo a favor)

Crear `RES_L` igual que en el paso 1 y pagarla en efectivo (igual que sección "009",
paso 1) para llevarla a `Confirmada`:

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/reservations/${RES_L}/payments \
  -H "Content-Type: application/json" \
  -d '{ "method": "EFECTIVO", "amount": 200000 }'
```

Cancelarla:

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/reservations/${RES_L}/cancel \
  -H "Content-Type: application/json" \
  -d '{ "reason": "Cliente desistio del viaje", "actorId": "operador-1" }'
```

Se espera `200 OK` con `reservationStatus: "Cancelada"`, `creditBalance: 200000` y
`paymentStatus: "Saldo a favor pendiente"`.

## 3. Cancelar una reserva con un abono parcial (`200`, saldo a favor parcial)

Crear `RES_M` igual que en el paso 1 (`projectedValue: 200000`) y abonar solo una
parte:

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/reservations/${RES_M}/payments \
  -H "Content-Type: application/json" \
  -d '{ "method": "ABONO", "amount": 80000 }'
```

Cancelarla:

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/reservations/${RES_M}/cancel \
  -H "Content-Type: application/json" \
  -d '{ "reason": "Cliente desistio del viaje", "actorId": "operador-1" }'
```

Se espera `200 OK` con `creditBalance: 80000` (solo lo efectivamente abonado, no los
200000 del valor total).

## 4. Cancelar sin motivo (`400`)

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/reservations/${RES_K}/cancel \
  -H "Content-Type: application/json" \
  -d '{ "actorId": "operador-1" }'
```

(Usar una reserva nueva sin cancelar todavía si `RES_K` ya quedó `Cancelada`.) Se
espera `400 Bad Request` con `{"error":"validation_error", ...}`.

## 5. Cancelar sobre un estado inválido (`409`)

Repetir sobre `RES_K` (ya `Cancelada` desde el paso 1) el mismo `curl` del paso 1: se
espera `409 Conflict` con `{"error":"reservation_not_cancellable", ...}`, y la reserva
no cambia. Repetir el mismo caso llevando otra reserva hasta `En ejecucion` (igual que
sección "010", paso 1) e intentando cancelarla: mismo resultado.

## 6. Cancelar con una transferencia pendiente de decidir (`409`)

Crear `RES_N` igual que en el paso 1 y registrar una transferencia con soporte (igual
que sección "009"):

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/reservations/${RES_N}/payments \
  -H "Content-Type: application/json" \
  -d '{ "method": "TRANSFERENCIA", "amount": 200000, "supportReference": "comprobante-001" }'
```

Intentar cancelarla sin resolver la transferencia:

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/reservations/${RES_N}/cancel \
  -H "Content-Type: application/json" \
  -d '{ "reason": "Cliente desistio del viaje", "actorId": "operador-1" }'
```

Se espera `409 Conflict` con `{"error":"reservation_not_cancellable", ...}`, y la
reserva no cambia.

## 7. Consultar una reserva cancelada (`200`, motivo y actor visibles)

```bash
curl -s http://localhost:8080/api/tenants/travesia-natural/reservations/${RES_L}
```

Se espera `200 OK` con `reservationStatus: "Cancelada"`, `creditBalance: 200000`,
`paymentStatus: "Saldo a favor pendiente"`, `cancellationReason: "Cliente desistio del
viaje"` y `cancelledBy: "operador-1"`.

## 8. Tenant inexistente (`404`) y tenant `Inactivo` (`409`)

```bash
curl -i -X POST http://localhost:8080/api/tenants/no-existe/reservations/${RES_K}/cancel \
  -H "Content-Type: application/json" \
  -d '{ "reason": "Cliente desistio del viaje", "actorId": "operador-1" }'
```

Se espera `404 Not Found`. Luego, desactivar `travesia-natural` (paso 5 de la sección
"002") y repetir la cancelación: se espera `409 Conflict` con
`{"error":"tenant_inactive", ...}`. Reactivar el tenant al terminar (paso 6 de esa
sección).

## 9. Compilación y tests (spec 001 a spec 011)

```bash
./mvnw test
```

Deben seguir en verde `contextLoads` y los tests existentes, sin regresiones.

# Plan de verificación — 012 Ejecución de devolución sobre saldo a favor

Corresponde a `specs/012-reservation-refund-execution/`. Requiere Postgres arriba, la
app corriendo, el tenant `travesia-natural` `Activo`, y un token válido de
`laura.gomez@example.com` (sección "007", pasos 1-2) para crear reservas nuevas. Usa
`RES_L`, `RES_M` y `RES_K` ya generadas en la sección "011" (paga/cancela igual que ahí
si se ejecuta esta sección de forma aislada).

```bash
TOKEN="<accessToken de la sección 007, paso 1>"
```

## 1. Ejecutar devolución total (`200`, `creditBalance` en cero)

`RES_L` viene de la sección "011" paso 2: `Cancelada`, `creditBalance: 200000`,
`paymentStatus: "Saldo a favor pendiente"`. Ejecutar la devolución completa:

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/reservations/${RES_L}/refund \
  -H "Content-Type: application/json" \
  -d '{ "amount": 200000, "reason": "Devolucion acordada con el cliente", "actorId": "admin-1", "method": "Transferencia" }'
```

Se espera `200 OK` con `creditBalance: 0`, `paymentStatus: "Devuelto parcial o
total"`, `refundedAmount: 200000`, `refundReason`, `refundedBy: "admin-1"`,
`refundMethod: "Transferencia"` y `refundedAt` presentes.

## 2. Ejecutar devolución parcial (`200`, `creditBalance` reducido, no en cero)

`RES_M` viene de la sección "011" paso 3: `Cancelada`, `creditBalance: 80000`,
`paymentStatus: "Saldo a favor pendiente"`. Devolver solo una parte:

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/reservations/${RES_M}/refund \
  -H "Content-Type: application/json" \
  -d '{ "amount": 30000, "reason": "Devolucion parcial acordada", "actorId": "admin-1", "method": "Efectivo" }'
```

Se espera `200 OK` con `creditBalance: 50000` (no en cero) y `paymentStatus: "Devuelto
parcial o total"`.

## 3. Devolución con monto mayor al `creditBalance` disponible (`400`)

Crear `RES_P` igual que en la sección "011" paso 2 (pagar en efectivo el valor
completo y cancelarla), quedando `creditBalance: 200000`. Intentar devolver más de lo
disponible:

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/reservations/${RES_P}/refund \
  -H "Content-Type: application/json" \
  -d '{ "amount": 999999, "reason": "Devolucion acordada", "actorId": "admin-1", "method": "Efectivo" }'
```

Se espera `400 Bad Request` con `{"error":"validation_error", ...}`, y la reserva no
cambia (`creditBalance` sigue en `200000`).

## 4. Devolución sin motivo o sin actor (`400`)

Sobre la misma `RES_P` (sin modificar por el paso anterior):

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/reservations/${RES_P}/refund \
  -H "Content-Type: application/json" \
  -d '{ "amount": 200000, "actorId": "admin-1", "method": "Efectivo" }'
```

Se espera `400 Bad Request` con `{"error":"validation_error", ...}` (falta `reason`).
Repetir omitiendo `actorId` en su lugar: mismo resultado.

## 5. Devolución sobre una reserva sin `Saldo a favor pendiente` (`409`)

Tres casos, todos deben devolver `409 Conflict` con
`{"error":"reservation_not_refundable", ...}` sin modificar la reserva:

- `RES_K` (sección "011" paso 1: `Cancelada` sin pagos, `paymentStatus: "Sin pago"`).
- Una reserva `Confirmada` sin cancelar (ej. crear una nueva y pagarla en efectivo,
  igual que sección "009" paso 1, sin cancelarla).
- `RES_L`, ya `Devuelto parcial o total` desde el paso 1 de esta sección (repetir el
  mismo `curl` del paso 1).

## 6. Consultar una reserva con devolución ejecutada (`200`, campos visibles)

```bash
curl -s http://localhost:8080/api/tenants/travesia-natural/reservations/${RES_L}
```

Se espera `200 OK` con `paymentStatus: "Devuelto parcial o total"`,
`creditBalance: 0`, `refundedAmount: 200000`, `refundReason`,
`refundedBy: "admin-1"`, `refundMethod: "Transferencia"` y `refundedAt`.

## 7. Tenant inexistente (`404`) y tenant `Inactivo` (`409`)

```bash
curl -i -X POST http://localhost:8080/api/tenants/no-existe/reservations/${RES_M}/refund \
  -H "Content-Type: application/json" \
  -d '{ "amount": 1000, "reason": "Devolucion", "actorId": "admin-1", "method": "Efectivo" }'
```

Se espera `404 Not Found`. Luego, desactivar `travesia-natural` (paso 5 de la sección
"002") y repetir la devolución: se espera `409 Conflict` con
`{"error":"tenant_inactive", ...}`. Reactivar el tenant al terminar (paso 6 de esa
sección).

## 8. Compilación y tests (spec 001 a spec 012)

```bash
./mvnw test
```

Deben seguir en verde `contextLoads` y los tests existentes, sin regresiones.

# Plan de verificación — 013 Caja diaria

Todos los endpoints cuelgan de `/api/tenants/{tenantId}/cash`, usando `travesia-natural`
(debe estar `Activo`). Guardar el `cashRegisterId` devuelto en `${CASH_ID}` para los
pasos siguientes.

## 1. Abrir caja del día (`201`)

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/cash \
  -H "Content-Type: application/json" \
  -d '{ "businessDate": "2026-09-04", "baseAmount": 50000, "actorId": "admin-1" }'
```

Se espera `201 Created` con `status: "ABIERTA"`, `baseAmount: 50000`, `movements: []`,
`corrections: []`, `totalAmount: null`.

## 2. Abrir una segunda caja para la misma fecha (`409`)

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/cash \
  -H "Content-Type: application/json" \
  -d '{ "businessDate": "2026-09-04", "baseAmount": 10000, "actorId": "admin-1" }'
```

Se espera `409 Conflict` con `{"error":"conflict", ...}` (`CashRegisterAlreadyOpenException`).

## 3. Registrar movimientos `INGRESO`, `PAGO` y `GASTO` (`201`)

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/cash/${CASH_ID}/movements \
  -H "Content-Type: application/json" \
  -d '{ "type": "INGRESO", "amount": 100000, "concept": "Venta de tour", "actorId": "admin-1" }'

curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/cash/${CASH_ID}/movements \
  -H "Content-Type: application/json" \
  -d '{ "type": "PAGO", "amount": 20000, "concept": "Pago a guía", "actorId": "admin-1" }'

curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/cash/${CASH_ID}/movements \
  -H "Content-Type: application/json" \
  -d '{ "type": "GASTO", "amount": 5000, "concept": "Combustible", "actorId": "admin-1" }'
```

Cada uno responde `201 Created` con el movimiento añadido a la lista `movements` y
`totalAmount` sigue `null` (caja `ABIERTA`, todavía sin congelar).

## 4. Registrar un movimiento `DEVOLUCION` (`400`)

Las devoluciones nunca se registran como movimiento manual: se calculan en vivo desde
`reservations` (paso 5). `CashMovementType` deliberadamente no incluye `DEVOLUCION`:

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/cash/${CASH_ID}/movements \
  -H "Content-Type: application/json" \
  -d '{ "type": "DEVOLUCION", "amount": 1000, "concept": "Devolucion manual", "actorId": "admin-1" }'
```

Se espera `400 Bad Request` con `{"error":"validation_error", ...}`
(`CashMovementType.valueOf("DEVOLUCION")` lanza `IllegalArgumentException`).

## 5. Consultar la caja abierta con total en vivo (`200`)

```bash
curl -s "http://localhost:8080/api/tenants/travesia-natural/cash?businessDate=2026-09-04"
```

Se espera `200 OK` con `status: "ABIERTA"` y `totalAmount` calculado en vivo como
`baseAmount + ingresos - pagos - gastos - devoluciones del día` (sin persistirse:
`GET` sucesivos recalculan, no guardan). Las devoluciones se traen de reservas reales
con `refundedAt` ese mismo `businessDate` (sección "012"): si hay reservas devueltas
ese día, el total las descuenta aunque no exista ningún movimiento `DEVOLUCION`
persistido — es la integración cruzada `cash` → `reservations` (`RefundsTotalCalculator`).

## 6. Cerrar la caja (`200`, total congelado)

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/cash/${CASH_ID}/close \
  -H "Content-Type: application/json" \
  -d '{ "actorId": "admin-1" }'
```

Se espera `200 OK` con `status: "CERRADA"`, `closedBy: "admin-1"`, `closedAt` presente y
`totalAmount` fijado (igual al total en vivo del paso 5, ya congelado). Repetir el mismo
`GET` del paso 5: el total ya no cambia aunque cambien las reservas de ese día.

## 7. Cerrar una caja ya cerrada (`409`)

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/cash/${CASH_ID}/close \
  -H "Content-Type: application/json" \
  -d '{ "actorId": "admin-1" }'
```

Se espera `409 Conflict` con `{"error":"conflict", ...}` (`CashRegisterClosedException`).

## 8. Corrección sobre caja `CERRADA` (`201`) y sobre caja `ABIERTA` (`409`)

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/cash/${CASH_ID}/corrections \
  -H "Content-Type: application/json" \
  -d '{ "justification": "Ajuste por diferencia de arqueo", "actorId": "admin-1" }'
```

Se espera `201 Created` con la corrección añadida a `corrections`. Abrir una caja nueva
en otra fecha (repetir el paso 1 con `"businessDate": "2026-09-05"`) e intentar corregirla
sin cerrarla primero:

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/cash/${CASH_ID_2}/corrections \
  -H "Content-Type: application/json" \
  -d '{ "justification": "Ajuste prematuro", "actorId": "admin-1" }'
```

Se espera `409 Conflict` con `{"error":"conflict", ...}` (`CashRegisterNotClosedException`).
Cerrar esa segunda caja al terminar (paso 6) para no dejarla `ABIERTA`.

## 9. Histórico de cajas cerradas (`200`)

```bash
curl -s http://localhost:8080/api/tenants/travesia-natural/cash/history
```

Se espera `200 OK` con la lista de cajas `CERRADA` del tenant, incluyendo las dos
abiertas/cerradas en los pasos anteriores, cada una con su `totalAmount` congelado.

## 10. Consolidación mensual (`200`)

```bash
curl -s "http://localhost:8080/api/tenants/travesia-natural/cash/consolidation?period=2026-09"
```

Se espera `200 OK` con una lista de un elemento: `period: "2026-09"`, `ingresos`,
`pagosOperacionales`, `gastos`, `devoluciones`, `cancelaciones` y `costosOperacionales`
agregados sobre las cajas cerradas del mes. `total` = `ingresos - pagosOperacionales -
gastos - devoluciones`, **sin sumar `baseAmount`** de cada caja (RN-CAJ-001: "sin sumar
repetidamente cada base diaria").

## 11. Tenant inexistente (`404`) y tenant `Inactivo` (`409`)

```bash
curl -i http://localhost:8080/api/tenants/no-existe/cash?businessDate=2026-09-04
```

Se espera `404 Not Found`. Desactivar `travesia-natural` (paso 5 de la sección "002") y
repetir cualquier operación de caja (ej. el `GET` anterior con el tenant real): se
espera `409 Conflict` con `{"error":"tenant_inactive", ...}` — aplica a las 6 operaciones
de caja, no solo a la apertura. Reactivar el tenant al terminar (paso 6 de esa sección).

## 12. Fecha sin caja abierta (`404`)

```bash
curl -i "http://localhost:8080/api/tenants/travesia-natural/cash?businessDate=2020-01-01"
```

Se espera `404 Not Found` con `{"error":"not_found", ...}` (`CashRegisterNotFoundException`).

## 13. Compilación y tests (spec 001 a spec 013)

```bash
./mvnw test
```

Deben seguir en verde `contextLoads` y los tests existentes, sin regresiones.

# Plan de verificación — Corrección de persistencia (agregados con hijos)

Hallazgo de `/code-review` sobre spec 013: `CashRegisterRepositoryAdapter.save()`
reconstruía el agregado completo (todos los movimientos/correcciones como entidades
nuevas sin id) en cada guardado; como `orphanRemoval = true` no puede emparejarlas con
las filas ya persistidas, cada guardado borraba y reinsertaba todo el historial. El
mismo patrón existía en `ReservationRepositoryAdapter` desde spec 001 (reescribía
`reservedServices` en cada pago/cancelación aunque nunca cambian tras la creación).
Corregido en `cash` y `reservations`: se carga la entidad existente por id y solo se
actualizan los campos escalares / se insertan los hijos nuevos, sin tocar los hijos ya
persistidos. Se agregó `@OrderBy("id ASC")` a las colecciones `@OneToMany` de ambos
módulos para que el orden de lectura sea determinista (necesario para poder distinguir
"hijos ya persistidos" de "hijos nuevos" por posición en la lista).

`operations` (`OperationCostRepositoryAdapter`, `ExecutionRepositoryAdapter`) se evaluó
para uniformidad pero se dejó como inserción directa: `OperationCost` y `Execution` son
entidades planas sin colecciones hijas y sin caso de uso de edición (se crean una sola
vez), así que el mismo patrón de "cargar antes de escribir" ahí solo agrega un `SELECT`
que siempre falla, sin corregir ningún bug real. Verificado además que el Frontend
(repo `Multitour-Monolito-Portal`) hoy solo consume `reservations` — no tiene ninguna
pantalla que toque `cash` ni `operations` — por lo que no hay necesidad funcional de
tocar ese módulo ahora.

## 1. Caja: movimientos incrementales no se pierden ni se duplican

```bash
CASH_ID=$(curl -s -X POST http://localhost:8080/api/tenants/travesia-natural/cash \
  -H "Content-Type: application/json" \
  -d '{ "businessDate": "2026-09-07", "baseAmount": 10000, "actorId": "admin-1" }' \
  | grep -o '"cashRegisterId":"[^"]*"' | cut -d'"' -f4)

curl -s -X POST http://localhost:8080/api/tenants/travesia-natural/cash/${CASH_ID}/movements \
  -H "Content-Type: application/json" \
  -d '{ "type": "INGRESO", "amount": 1000, "concept": "m1", "actorId": "admin-1" }'

curl -s -X POST http://localhost:8080/api/tenants/travesia-natural/cash/${CASH_ID}/movements \
  -H "Content-Type: application/json" \
  -d '{ "type": "PAGO", "amount": 200, "concept": "m2", "actorId": "admin-1" }'

curl -s "http://localhost:8080/api/tenants/travesia-natural/cash?businessDate=2026-09-07"
```

Cada respuesta debe traer los movimientos anteriores intactos (mismo `recordedAt`, sin
duplicarse) más el nuevo agregado al final. La consulta final debe traer los 2
movimientos, en el mismo orden en que se registraron.

## 2. Caja: cerrar y corregir no tocan los movimientos ya guardados

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/cash/${CASH_ID}/close \
  -H "Content-Type: application/json" -d '{ "actorId": "admin-1" }'

curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/cash/${CASH_ID}/corrections \
  -H "Content-Type: application/json" \
  -d '{ "justification": "ajuste", "actorId": "admin-1" }'
```

Ambas respuestas deben seguir trayendo los 2 movimientos originales sin cambios, más
(en el segundo caso) la corrección agregada.

## 3. Reserva: pagos y cancelación no afectan `reservedServices`

Sobre una reserva `Pendiente de pago` con un solo servicio reservado:

```bash
curl -s -X POST http://localhost:8080/api/tenants/travesia-natural/reservations/${RES_ID}/payments \
  -H "Content-Type: application/json" \
  -d '{ "amount": 100000, "method": "ABONO", "actorId": "admin-1" }'

curl -s -X POST http://localhost:8080/api/tenants/travesia-natural/reservations/${RES_ID}/cancel \
  -H "Content-Type: application/json" \
  -d '{ "reason": "prueba", "actorId": "admin-1" }'

curl -s http://localhost:8080/api/tenants/travesia-natural/reservations/${RES_ID}
```

`reservedServices` debe seguir con el mismo único elemento en las tres respuestas
(mismo `serviceReference`/`partySize`/`scheduledDate`), mientras `pendingBalance`,
`paymentStatus`, `reservationStatus`, `cancelledAt`, etc. sí reflejan cada cambio.

## 4. Histórico e informes siguen leyendo correctamente

```bash
curl -s http://localhost:8080/api/tenants/travesia-natural/cash/history
curl -s "http://localhost:8080/api/tenants/travesia-natural/cash/consolidation?period=2026-09"
```

Ambos deben seguir devolviendo `200 OK` con los datos agregados de todas las cajas
cerradas del tenant, movimientos en orden, sin errores de mapeo.

## 5. Compilación y tests (spec 001 a spec 013, tras la corrección de persistencia)

```bash
./mvnw test
```

Deben seguir en verde `contextLoads` y los tests existentes, sin regresiones.

# Plan de verificación — NPE en movimientos sin `type` + doble consulta en consolidación

Dos hallazgos de `/code-review` sobre spec 013, pendientes desde la corrección de
persistencia:

1. `CashController.registerMovement` llamaba `CashMovementType.valueOf(request.type())`
   directamente sobre el valor crudo del DTO. Si `type` venía ausente o `null` en el
   JSON, `Enum.valueOf(_, null)` lanza `NullPointerException` (no
   `IllegalArgumentException`), sin `@ExceptionHandler` que la capture: la API devolvía
   `500` en vez del `400` documentado en la spec. `CashMovement` ya validaba
   `type == null` correctamente en su constructor compacto — el problema era que nunca
   llegaba a ejecutarse. Corregido dejando pasar `null` a `RegisterCashMovementCommand`
   en vez de invocar `valueOf` sobre un valor nulo, para que la validación de dominio
   (única fuente de verdad) haga su trabajo.
2. `MonthlyCashConsolidationService.getMonthlyConsolidation()` consultaba
   `reservationRepositoryPort.findAllByTenantId(tenantId)` dos veces en la misma
   petición: una directa (cancelaciones) y otra dentro de
   `RefundsTotalCalculator.totalForPeriod` (devoluciones). Corregido consultando una
   sola vez y reutilizando la misma lista para ambos cálculos;
   `RefundsTotalCalculator.totalForPeriod` ahora recibe la lista ya cargada en vez de
   `tenantId` (único llamador, sin otros call sites afectados).

## 1. Movimiento sin `type` devuelve 400, no 500

```bash
CASH_ID=$(curl -s -X POST http://localhost:8080/api/tenants/travesia-natural/cash \
  -H "Content-Type: application/json" \
  -d '{ "businessDate": "2026-09-09", "baseAmount": 5000, "actorId": "admin-1" }' \
  | grep -o '"cashRegisterId":"[^"]*"' | cut -d'"' -f4)

curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/cash/${CASH_ID}/movements \
  -H "Content-Type: application/json" \
  -d '{ "amount": 1000, "concept": "sin tipo", "actorId": "admin-1" }'

curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/cash/${CASH_ID}/movements \
  -H "Content-Type: application/json" \
  -d '{ "type": null, "amount": 1000, "concept": "tipo null", "actorId": "admin-1" }'
```

Ambas deben devolver `400` con `{"error":"validation_error","message":"Unknown
CashMovementType label: null"}` — mensaje actualizado tras el cambio a `fromLabel()`
(ver sección más abajo, "Alineación de `CashMovementType` con el contrato del
Frontend"). Un `type` inexistente (ej. `"FOO"`) debe seguir devolviendo `400` con el
mismo tipo de mensaje (`"Unknown CashMovementType label: FOO"`).

## 2. Consolidación mensual sigue devolviendo los mismos valores

```bash
curl -s "http://localhost:8080/api/tenants/travesia-natural/cash/consolidation?period=2026-09"
```

Debe seguir devolviendo `200 OK` con los mismos campos y valores que antes del cambio
(`ingresos`, `pagosOperacionales`, `gastos`, `devoluciones`, `total`, `cancelaciones`,
`costosOperacionales`) — la única diferencia es una consulta menos a `reservations` por
petición.

## 3. Compilación y tests

```bash
./mvnw test
```

Deben seguir en verde `contextLoads` y los tests existentes, sin regresiones.

# Plan de verificación — Alineación de `CashMovementType` con el contrato del Frontend

Hallazgo de `/code-review`: el enum `CashMovementType` (`INGRESO`/`PAGO`/`GASTO`) no
coincidía con los literales que ya usa `operator-cash.service.ts` en la rama `develop`
del Frontend (`'Ingreso'`/`'Pago operacional'`/`'Gasto'`/`'Devolución'`, sin integración
HTTP todavía — trabajo de Fernanda Robayo, ver regla 11 de CLAUDE.md, solo lectura).
Decisión: hasta que Backend y Frontend completen su implementación por separado, no debe
haber ningún llamado HTTP real entre ambos; mientras tanto, se alinea el Backend al
contrato ya definido en el Frontend para evitar el desfase cuando llegue la integración.

Se aplicó el mismo patrón `label()`/`fromLabel()` que ya usan `ReservationStatus` y
`PaymentStatus`: el enum interno de Java conserva sus constantes (`INGRESO`, `PAGO`,
`GASTO`), pero expone/acepta el literal en español como valor de intercambio (JSON y
columna `type` en `cash_movements`). No se agrega `DEVOLUCION`: sigue sin registrarse a
mano, se calcula en vivo desde las devoluciones ejecutadas (spec 012), igual que antes.
No se tocó ningún archivo del Frontend.

Efecto colateral: las filas de prueba de esta sesión en la base de desarrollo
(`cash_movements.type` en formato `INGRESO`/`PAGO`/`GASTO`) se migraron a mano a
`Ingreso`/`Pago operacional`/`Gasto` vía `UPDATE` directo (dato sintético de pruebas
propias, no hay entorno productivo ni datos de terceros afectados).

## 1. Movimiento con el literal del Frontend es aceptado

```bash
CASH_ID=$(curl -s -X POST http://localhost:8080/api/tenants/travesia-natural/cash \
  -H "Content-Type: application/json" \
  -d '{ "businessDate": "2026-09-11", "baseAmount": 5000, "actorId": "admin-1" }' \
  | grep -o '"cashRegisterId":"[^"]*"' | cut -d'"' -f4)

curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/cash/${CASH_ID}/movements \
  -H "Content-Type: application/json" \
  -d '{ "type": "Ingreso", "amount": 1000, "concept": "prueba", "actorId": "admin-1" }'

curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/cash/${CASH_ID}/movements \
  -H "Content-Type: application/json" \
  -d '{ "type": "Pago operacional", "amount": 200, "concept": "prueba", "actorId": "admin-1" }'

curl -s "http://localhost:8080/api/tenants/travesia-natural/cash?businessDate=2026-09-11"
```

Ambos `POST` devuelven `201`. La consulta final debe mostrar `movements[].type` como
`"Ingreso"` y `"Pago operacional"` (no `"INGRESO"`/`"PAGO"`).

## 2. El formato viejo (nombre del enum de Java) ya no es válido

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/cash/${CASH_ID}/movements \
  -H "Content-Type: application/json" \
  -d '{ "type": "INGRESO", "amount": 100, "concept": "formato viejo", "actorId": "admin-1" }'
```

Debe devolver `400` (`"Unknown CashMovementType label: INGRESO"`) — señal de que ya no
conviven dos formatos distintos para el mismo dato.

## 3. Historial y consolidación siguen leyendo sin error tras la migración de datos

```bash
curl -s http://localhost:8080/api/tenants/travesia-natural/cash/history
curl -s "http://localhost:8080/api/tenants/travesia-natural/cash/consolidation?period=2026-09"
```

Ambos deben seguir devolviendo `200 OK`, incluyendo las jornadas cerradas con
movimientos migrados al nuevo formato.

## 4. Compilación y tests

```bash
./mvnw test
```

Deben seguir en verde `contextLoads` y los tests existentes, sin regresiones.

# Plan de verificación — Spec 014: Registro y consulta de colaboradores operativos

Cierra una de las 20 brechas confirmadas en el análisis Backend-vs-PDR del 2026-09-04:
`MembershipRole.OPERATIONAL_COLLABORATOR` existía en el enum desde spec 002 sin ningún
caso de uso que lo asignara. El Frontend (rama `develop`, push de Fernanda Robayo del
2026-09-03/04, ver regla 11 de CLAUDE.md, solo lectura) ya tiene pantallas reales
(`collaborators`, `collaborators/new`, `collaborators/detail`) corriendo en simulación
local. No hay integración HTTP real todavía (decisión vigente: sin llamadas HTTP entre
Backend y Frontend hasta que ambos módulos estén completos); esta verificación es
enteramente vía `curl` contra el Backend en aislamiento.

Reutiliza el patrón ya existente de `Membership`/`RegisterCustomerService`/
`CreateTenantService`: mismo `PasswordPolicy`, mismo `EmailAlreadyRegisteredException`,
mismo `AuditRecorder`. El campo "nombre completo" del Frontend se guarda en
`firstName` (`lastName` queda `null`, igual que en Administrator) — sin migración de
esquema, la tabla `memberships` ya es genérica. El toggle "colaborador puede validar
soportes de transferencia" (PDR línea 115) quedó fuera de esta spec a propósito (ver
spec.md, "Fuera de alcance" y decisión abierta 2).

## 1. Crear un tenant + Administrator de prueba

```bash
curl -s -X POST http://localhost:8080/api/tenants \
  -H "Content-Type: application/json" \
  -d '{ "tenantId": "spec014-collab-demo", "commercialName": "Spec 014 Demo",
        "administrator": { "email": "admin@spec014.test", "password": "Admin123!",
        "passwordConfirmation": "Admin123!" }, "actorId": "platform-admin" }'
```

Debe devolver `201` con `tenantStatus: "ACTIVO"`.

## 2. Registrar un colaborador válido (`201`)

```bash
curl -i -X POST http://localhost:8080/api/tenants/spec014-collab-demo/collaborators \
  -H "Content-Type: application/json" \
  -d '{ "name": "Laura Perez", "email": "laura@spec014.test", "password": "Colab123!",
        "passwordConfirmation": "Colab123!", "actorId": "admin@spec014.test" }'
```

Debe devolver `201` con `role: "OPERATIONAL_COLLABORATOR"`, `name: "Laura Perez"` y sin
ningún campo de contraseña en la respuesta.

## 3. Rechazo por correo duplicado en el mismo tenant (`409`)

```bash
curl -i -X POST http://localhost:8080/api/tenants/spec014-collab-demo/collaborators \
  -H "Content-Type: application/json" \
  -d '{ "name": "Otra Laura", "email": "laura@spec014.test", "password": "Colab123!",
        "passwordConfirmation": "Colab123!", "actorId": "admin@spec014.test" }'
```

Debe devolver `409` (`"email_already_registered"`).

## 4. Rechazo por política de contraseña incumplida (`400`)

```bash
curl -i -X POST http://localhost:8080/api/tenants/spec014-collab-demo/collaborators \
  -H "Content-Type: application/json" \
  -d '{ "name": "Otro Nombre", "email": "debil@spec014.test", "password": "123",
        "passwordConfirmation": "123", "actorId": "admin@spec014.test" }'
```

Debe devolver `400` (`"validation_error"`, mensaje de `PasswordPolicy`).

## 5. Listado y detalle del colaborador (`200`, sin `passwordHash`)

```bash
curl -s http://localhost:8080/api/tenants/spec014-collab-demo/collaborators

MEMBERSHIP_ID="<el membershipId devuelto en el paso 2>"
curl -s http://localhost:8080/api/tenants/spec014-collab-demo/collaborators/${MEMBERSHIP_ID}
```

El listado debe incluir a Laura con `name`/`email`/`role`. El detalle debe traer los
mismos datos, sin ningún campo `passwordHash` en el JSON.

## 6. Login del colaborador recién registrado (`200`, JWT con su rol)

```bash
curl -s -X POST http://localhost:8080/api/tenants/spec014-collab-demo/login \
  -H "Content-Type: application/json" \
  -d '{ "email": "laura@spec014.test", "password": "Colab123!" }'
```

Debe devolver `200` con `accessToken` y `role: "OPERATIONAL_COLLABORATOR"` — sin
cambios en `LoginService`, ya era agnóstico al rol.

## 7. Aislamiento entre tenants (`200` vacío, `404` cruzado)

```bash
curl -s -X POST http://localhost:8080/api/tenants \
  -H "Content-Type: application/json" \
  -d '{ "tenantId": "spec014-collab-demo-2", "commercialName": "Spec 014 Aislamiento",
        "administrator": { "email": "admin2@spec014.test", "password": "Admin123!",
        "passwordConfirmation": "Admin123!" }, "actorId": "platform-admin" }'

curl -s http://localhost:8080/api/tenants/spec014-collab-demo-2/collaborators

curl -i http://localhost:8080/api/tenants/spec014-collab-demo-2/collaborators/${MEMBERSHIP_ID}
```

El listado del segundo tenant debe venir vacío (`[]`). Consultar el detalle del
colaborador del primer tenant desde el segundo debe devolver `404`
(`"collaborator_not_found"`), no `200` con datos ajenos.

## 8. Compilación y tests

```bash
./mvnw test
```

Deben seguir en verde `contextLoads` y los tests existentes, sin regresiones.

Ejecutado el 2026-09-04 contra la base de desarrollo local (Postgres en `multitour-postgres`,
tenants `spec014-collab-demo` y `spec014-collab-demo-2`, datos sintéticos de prueba, sin
afectar tenants reales). `./mvnw test` en verde (`contextLoads`, 12 migraciones validadas)
antes de levantar la app. Los 7 pasos de `curl` (registro, duplicado, password débil,
listado, detalle sin `passwordHash`, login con `role: OPERATIONAL_COLLABORATOR`,
aislamiento cruzado con `404 collaborator_not_found`) devolvieron los códigos HTTP y
payloads exactos documentados arriba.

## Spec 015 — Tipo de catálogo Transporte

Agrega `TRANSPORT` a `CatalogItemType` y dos campos opcionales (`route`,
`operationalCost`) a `CatalogItem`, reutilizando el CRUD-lite de spec 005 sin
endpoints nuevos. Migración `V13__add_transport_fields_to_catalog_items.sql`.

### 1. Compilación y tests

```bash
./mvnw test
```

Debe migrar a la versión 13 (`add transport fields to catalog items`) y mantener
`contextLoads` en verde.

### 2. Crear un tenant activo para las pruebas

```bash
curl -X POST http://localhost:8080/api/tenants \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": "spec015-transport-demo",
    "commercialName": "Spec015 Transport Demo",
    "actorId": "system-test",
    "administrator": {
      "name": "Admin Spec015",
      "email": "admin@spec015-transport-demo.test",
      "password": "Passw0rd!123",
      "passwordConfirmation": "Passw0rd!123"
    }
  }'
```

### 3. Crear TRANSPORT sin campos opcionales

```bash
curl -X POST http://localhost:8080/api/tenants/spec015-transport-demo/catalog-items \
  -H "Content-Type: application/json" \
  -d '{"type": "TRANSPORT", "name": "Ruta Neiva - San Agustin", "price": 45000}'
```

Debe devolver `201` con `capacity`, `route` y `operationalCost` en `null`.

### 4. Round-trip de route/operationalCost

Crear un ítem `TRANSPORT` con `route`/`operationalCost` y consultarlo por
`GET /{itemId}`: ambos valores deben devolverse tal cual se guardaron.

### 5. PATCH parcial

`PATCH` solo `route` sobre el ítem anterior: el `GET` posterior debe conservar
`operationalCost` y el resto de campos sin cambios.

### 6. Regresión de capacity en LODGING

```bash
curl -X POST http://localhost:8080/api/tenants/spec015-transport-demo/catalog-items \
  -H "Content-Type: application/json" \
  -d '{"type": "LODGING", "name": "Cabana Rio Magdalena", "price": 150000}'
```

Debe seguir devolviendo `400 validation_error` ("capacity is required and must be
positive for LODGING items") — TRANSPORT no debilita esa regla.

### 7. Criterios ya cubiertos por spec 005, contra un ítem TRANSPORT

- Tenant inexistente → `404 not_found`.
- Segundo tenant: el ítem del primero no aparece en su lista y su `GET /{itemId}`
  devuelve `404` (aislamiento).
- `deactivate` → `200`, `active: false`; `deactivate` de nuevo → `400
  validation_error` ("already inactive"); `reactivate` → `200`, `active: true`.
- Tenant `Inactivo` → `POST` de un ítem `TRANSPORT` nuevo devuelve `409
  tenant_inactive`.

Ejecutado el 2026-09-04 contra la base de desarrollo local (mismo Postgres
`multitour-postgres`, tenants `spec015-transport-demo` y
`spec015-transport-demo-2`, datos sintéticos, sin afectar tenants reales).
`./mvnw test` en verde (`contextLoads`, migración a versión 13 confirmada) antes de
levantar la app. Los 7 pasos de `curl` de esta sección (creación sin opcionales,
round-trip route/operationalCost, PATCH parcial, regresión LODGING, tenant
inexistente, aislamiento entre tenants, soft delete/reactivate, tenant inactivo)
devolvieron los códigos HTTP y payloads exactos documentados arriba.

## Spec 016 — Aislamiento de reservas por Cliente

Agrega `GET .../reservations/me` y `GET .../reservations/me/{reservationId}`,
protegidos con JWT, que filtran por el `customerId` (`membershipId` del token) del
llamante. No toca los endpoints públicos existentes `GET .../reservations` (Staff,
sin filtrar) ni `GET .../reservations/{reservationId}`.

### 1. Compilación y tests

```bash
./mvnw test
```

Debe mantener `contextLoads` en verde (no hay migración nueva en esta spec).

### 2. Crear un tenant activo y dos End Customers distintos

```bash
curl -X POST http://localhost:8080/api/tenants \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": "spec016-reservas-demo",
    "commercialName": "Spec016 Reservas Demo",
    "actorId": "system-test",
    "administrator": {
      "name": "Admin Spec016",
      "email": "admin@spec016-reservas-demo.test",
      "password": "Passw0rd!123",
      "passwordConfirmation": "Passw0rd!123"
    }
  }'

curl -X POST http://localhost:8080/api/tenants/spec016-reservas-demo/customers \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Cliente","lastName":"A","email":"clientea@spec016.test","phone":"3000000001","password":"Passw0rd!123","passwordConfirmation":"Passw0rd!123"}'

curl -X POST http://localhost:8080/api/tenants/spec016-reservas-demo/customers \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Cliente","lastName":"B","email":"clienteb@spec016.test","phone":"3000000002","password":"Passw0rd!123","passwordConfirmation":"Passw0rd!123"}'
```

Login de cada uno (`POST .../login`) para obtener `TOKEN_A`/`TOKEN_B` con
`membershipId` distinto.

### 3. Cada Cliente crea su propia reserva

```bash
curl -X POST http://localhost:8080/api/tenants/spec016-reservas-demo/reservations \
  -H "Content-Type: application/json" -H "Authorization: Bearer ${TOKEN_A}" \
  -d '{"projectedValue": 150000, "reservedServices": [{"serviceReference": "tour-demo-1", "partySize": 2, "scheduledDate": "2026-10-01"}]}'

curl -X POST http://localhost:8080/api/tenants/spec016-reservas-demo/reservations \
  -H "Content-Type: application/json" -H "Authorization: Bearer ${TOKEN_B}" \
  -d '{"projectedValue": 200000, "reservedServices": [{"serviceReference": "tour-demo-2", "partySize": 1, "scheduledDate": "2026-10-02"}]}'
```

Guardar `RES_A`/`RES_B` de cada respuesta.

### 4. `GET .../reservations/me` filtra por Cliente

```bash
curl -s http://localhost:8080/api/tenants/spec016-reservas-demo/reservations/me \
  -H "Authorization: Bearer ${TOKEN_A}"
```

Debe devolver únicamente `RES_A`, nunca `RES_B`.

### 5. Lista vacía para un Cliente sin reservas

Registrar y loguear un tercer Cliente (`clientec@spec016.test`, sin crear ninguna
reserva) y repetir el paso 4 con su token: debe devolver `[]`.

### 6. Sin token → `401`

```bash
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/tenants/spec016-reservas-demo/reservations/me
```

Debe devolver `401`.

### 7. Detalle de la reserva ajena por `/me/{id}` → `404`

```bash
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/tenants/spec016-reservas-demo/reservations/me/${RES_B} \
  -H "Authorization: Bearer ${TOKEN_A}"
```

Cliente A pidiendo la reserva de Cliente B debe devolver `404` (mismo patrón de
404 unificado que spec 014, nunca revela que la reserva existe).

### 8. Token de otro tenant → `403 tenant_mismatch`

```bash
curl -s -w "\nHTTP:%{http_code}\n" http://localhost:8080/api/tenants/otro-tenant-cualquiera/reservations/me \
  -H "Authorization: Bearer ${TOKEN_A}"
```

Debe devolver `403` con `{"error":"tenant_mismatch", ...}`.

### 9. Regresión: `GET .../reservations` (Staff, sin autenticación) sigue sin filtrar

```bash
curl -s http://localhost:8080/api/tenants/spec016-reservas-demo/reservations
```

Debe seguir devolviendo `200` con todas las reservas del tenant (`RES_A` y `RES_B`
juntas), sin exigir token — confirma que el endpoint público existente no quedó
afectado por esta spec.

Ejecutado el 2026-09-05 contra la base de desarrollo local (mismo Postgres
`multitour-postgres`, tenant `spec016-reservas-demo` con tres End Customers
sintéticos, sin afectar tenants reales). `./mvnw test` en verde (`contextLoads`,
sin migraciones nuevas) antes de levantar la app. Los 8 pasos de `curl` de esta
sección (listado propio, lista vacía, 401 sin token, 404 sobre reserva ajena, 403
tenant_mismatch, y la regresión del listado público sin filtrar) devolvieron los
códigos HTTP y payloads exactos documentados arriba.

## Spec 017 — Finalización de la ejecución de una reserva

Corresponde a `specs/017-reservation-execution-finalization/`. Agrega
`POST .../reservations/{reservationId}/finalize`, que cierra operativamente una
reserva `En ejecucion` (transición a `Finalizada`) y extiende
`GET .../reservations/{reservationId}/execution` con `finalized`/`finalizedBy`/
`finalizedAt`. Requiere Postgres arriba, la app corriendo, el tenant
`travesia-natural` `Activo`, y un token válido de `laura.gomez@example.com`
(sección "007", pasos 1-2) para crear reservas nuevas.

```bash
TOKEN="<accessToken de la sección 007, paso 1>"
```

### 1. Compilación y tests

```bash
./mvnw test
```

Debe mantener `contextLoads` en verde, con la migración a versión 14 confirmada.

### 2. Finalizar una reserva `En ejecucion` (`200`)

Crear una reserva, pagarla en efectivo (igual que sección "009", paso 1) y
registrar su ejecución (igual que sección "010", paso 1) para obtener `RES_Q` en
estado `En ejecucion`:

```bash
curl -s -X POST http://localhost:8080/api/tenants/travesia-natural/reservations \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{ "projectedValue": 300000, "reservedServices": [{ "serviceReference": "tour-laguna-verde" }] }'

curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/reservations/${RES_Q}/payments \
  -H "Content-Type: application/json" \
  -d '{ "method": "EFECTIVO", "amount": 300000 }'

curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/reservations/${RES_Q}/execution \
  -H "Content-Type: application/json" \
  -d '{ "served": true, "executed": 4, "actorId": "guia-1" }'
```

Y finalizarla:

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/reservations/${RES_Q}/finalize \
  -H "Content-Type: application/json" \
  -d '{ "actorId": "guia-1" }'
```

Se espera `200 OK` con `reservationStatus: "Finalizada"`, `finalizedBy: "guia-1"` y
`finalizedAt` presente.

### 3. Doble finalización (`409`)

Repetir el mismo `curl` del paso 2 sobre `RES_Q` (ya finalizada):

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/reservations/${RES_Q}/finalize \
  -H "Content-Type: application/json" \
  -d '{ "actorId": "guia-1" }'
```

Se espera `409 Conflict` con `{"error":"reservation_not_finalizable", ...}`.

### 4. Finalizar sobre un estado que no es `En ejecucion` (`409`)

Crear `RES_R` sin ejecutarla (queda `Confirmada` tras pagarla, igual que
sección "009", paso 1):

```bash
curl -i -X POST http://localhost:8080/api/tenants/travesia-natural/reservations/${RES_R}/finalize \
  -H "Content-Type: application/json" \
  -d '{ "actorId": "guia-1" }'
```

Se espera `409 Conflict` con `{"error":"reservation_not_finalizable", ...}`, y la
reserva sigue `Confirmada` sin cambios.

### 5. `GET .../execution` refleja la finalización (`200`)

```bash
curl -s http://localhost:8080/api/tenants/travesia-natural/reservations/${RES_Q}/execution
```

Se espera `200 OK` con `finalized: true`, `finalizedBy: "guia-1"` y `finalizedAt`
presente, además de los campos ya existentes (`served`, `executed`, `causal`,
`actorId`, `recordedAt`). Repetir sobre una reserva `En ejecucion` sin finalizar
(ej. `RES_G` de la sección "010" si sigue disponible): se espera `finalized: false`,
`finalizedBy: null`, `finalizedAt: null`.

### 6. Tenant inexistente (`404`) y tenant `Inactivo` (`409`)

```bash
curl -i -X POST http://localhost:8080/api/tenants/no-existe/reservations/${RES_Q}/finalize \
  -H "Content-Type: application/json" \
  -d '{ "actorId": "guia-1" }'
```

Se espera `404 Not Found` con `{"error":"not_found", ...}`. Desactivar el tenant
(sección "002") y repetir la finalización: se espera `409 Conflict` con
`{"error":"tenant_inactive", ...}`.

Ejecutado el 2026-09-05 contra la base de desarrollo local (mismo Postgres
`multitour-postgres`, tenant `travesia-natural`, cliente `laura.gomez@example.com`
existente). `./mvnw test` en verde (`contextLoads`, migración a versión 14
confirmada) antes de levantar la app. Los 6 pasos de `curl` de esta sección
(finalización exitosa con `finalizedBy`/`finalizedAt`, doble finalización `409`,
finalización sobre `Confirmada` `409`, `GET .../execution` reflejando
`finalized`/`finalizedBy`/`finalizedAt`, tenant inexistente `404` y tenant
`Inactivo` `409`, reactivando el tenant al final para no dejar el ambiente
alterado) devolvieron los códigos HTTP y payloads exactos documentados arriba. El
tercer criterio de aceptación de `spec.md` (finalizar sin `Execution` registrada)
no se probó en runtime: no existe forma de llegar a ese estado por el flujo
normal, por la garantía de atomicidad documentada en `plan.md`.
