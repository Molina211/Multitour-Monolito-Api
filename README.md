# Multitour — Backend (rama `qa`)

Backend del walking skeleton de **Multitour**, plataforma de turismo multitenant
(primer caso de uso: Travesía Natural). Spring Boot 4.1.1, Java 21, PostgreSQL,
arquitectura hexagonal por módulo de negocio sobre un monolito modular.

## Sobre esta rama

`qa` recibe el trabajo de `develop` mediante merges que siempre piden autorización
explícita — nunca se comitea directamente aquí. Es el punto de control antes de
promover a `main`: si estás leyendo esto en `qa`, este es el estado que se considera
listo para validar, no necesariamente lo último que existe en `develop`.

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
un servidor real — la misma evidencia que respalda que este estado está listo para QA.

## Más contexto

- `specs/` — una carpeta por funcionalidad (`spec.md`, `plan.md`, `tasks.md`), en orden
  correlativo.
- `PLAN-VERIFICACION.md` — evidencia de verificación manual, spec por spec.
