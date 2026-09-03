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
