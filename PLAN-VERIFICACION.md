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

_Pendiente — se agrega cuando exista el endpoint `/health` (T09)._

## 5. Verificar `/health`

_Pendiente (T09, T12)._

## 6. Crear una reserva

_Pendiente (T08, T12)._

## 7. Rechazo por datos incompletos

_Pendiente (T08, T12)._

## 8. Aislamiento entre tenants

_Pendiente (T12)._

## 9. Compilación y test por defecto

```bash
./gradlew test
```
