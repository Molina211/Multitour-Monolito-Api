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
./gradlew bootRun
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
./gradlew test
```
