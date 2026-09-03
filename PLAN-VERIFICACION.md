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

```bash
curl -i -X POST http://localhost:8080/api/reservations \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 11111111-1111-1111-1111-111111111111" \
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

Repetir el `curl` anterior tres veces, cada vez quitando un dato distinto, y confirmar
`400 Bad Request` en los tres casos sin que quede fila nueva en `reservations`:

- Sin el header `X-Tenant-Id`.
- Sin `customerId` en el body.
- Con `reservedServices: []` (lista vacía).

## 8. Aislamiento entre tenants

Repetir el `curl` del paso 6 dos veces con el mismo `customerId` pero un `X-Tenant-Id`
distinto en cada llamada (por ejemplo `22222222-2222-2222-2222-222222222222`). Confirmar
con la consulta `psql` del paso 6 que quedan dos filas con `tenant_id` distinto y sin
ninguna relación cruzada entre ellas.

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
