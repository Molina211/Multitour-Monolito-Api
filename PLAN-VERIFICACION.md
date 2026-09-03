# Plan de verificación — 001 Walking Skeleton + Reservation

Corresponde a `specs/001-walking-skeleton-reservation/`. Se completa por lotes a medida
que avanza la implementación (ver `tasks.md`).

## 1. Levantar Postgres local

```bash
docker compose up -d
docker compose ps
```

Se espera el contenedor `multitour-postgres` en estado `healthy`.

## 2. Arrancar la aplicación

_Pendiente — se agrega cuando exista el endpoint `/health` (T09)._

## 3. Verificar `/health`

_Pendiente (T09, T12)._

## 4. Crear una reserva

_Pendiente (T08, T12)._

## 5. Rechazo por datos incompletos

_Pendiente (T08, T12)._

## 6. Aislamiento entre tenants

_Pendiente (T12)._

## 7. Compilación y test por defecto

```bash
./gradlew test
```
