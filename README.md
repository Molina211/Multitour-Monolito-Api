# Multitour — Backend (rama `develop`)

Backend del walking skeleton de **Multitour**, plataforma de turismo multitenant
(primer caso de uso: Travesía Natural). Spring Boot 4.1.1, Java 21, PostgreSQL,
arquitectura hexagonal por módulo de negocio sobre un monolito modular.

## Sobre esta rama

`develop` es donde vive el trabajo diario del Backend: aquí se implementa y se comitea
cada spec cerrada (`specs/001-...` en adelante) antes de promoverla a `qa` y luego a
`main`, mediante merges que siempre piden autorización explícita por separado. Si estás
leyendo esto en `develop`, tienes el estado más reciente del Backend — puede incluir
trabajo que todavía no pasó el corte de QA.

## Cómo desplegar (con Docker, recomendado)

Requiere Docker y Docker Compose. Todo el stack (Postgres + Backend, y opcionalmente el
Frontend) se levanta con un solo comando desde la raíz de este repo.

```bash
cp .env.example .env   # credenciales de desarrollo, ya vienen con valores por defecto
docker compose up --build postgres backend
```

Esto expone:
- Backend en `http://localhost:8081` (contenedor escucha en 8080 internamente).
- Postgres en `localhost:5433` (puerto 5432 interno, remapeado para no chocar con una
  instancia local existente).

Para levantar también el Frontend (`multitour-frontend`, puerto `8080`), el repo
`Repositorio Monolito/Frontend` debe estar clonado como carpeta hermana de este repo
(`docker-compose.yml` referencia `../Frontend`); en ese caso, omite los nombres de
servicio y corre `docker compose up --build` a secas.

Variables de entorno relevantes (ver `.env.example`): `POSTGRES_DB`, `POSTGRES_USER`,
`POSTGRES_PASSWORD`, `APP_JWT_SECRET`. Los valores por defecto son solo para desarrollo
local — **no aptos para producción**.

## Cómo correrlo sin Docker (Postgres local + Maven)

```bash
# Postgres 16 escuchando en localhost:5433, BD "multitour", usuario/clave "multitour"
./mvnw spring-boot:run
```

`spring.flyway.enabled=true` aplica las migraciones automáticamente al arrancar.
`PlatformAdministratorSeeder` siembra, también al arrancar, un tenant reservado
`platform` con un Platform Administrator de desarrollo (`admin@multitour.plataforma` /
`Multitour#2026`) — ver `specs/026-login-staff-y-administrador-plataforma/`.

## Verificar que funciona

```bash
./mvnw test
```

Cada spec cerrada tiene su sección correspondiente en `PLAN-VERIFICACION.md`, con los
pasos exactos (`curl`, SQL) para comprobar manualmente que la funcionalidad sirve contra
un servidor real.

## Más contexto

- `specs/` — una carpeta por funcionalidad (`spec.md`, `plan.md`, `tasks.md`), en orden
  correlativo.
- `PLAN-VERIFICACION.md` — evidencia de verificación manual, spec por spec.
