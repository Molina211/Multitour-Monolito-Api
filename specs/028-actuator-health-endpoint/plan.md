# 028 — Plan técnico

## Enfoque

Agregar Spring Boot Actuator al Backend para exponer un healthcheck real
(`/actuator/health`) y usarlo en `docker-compose.yml` para que Docker espere a que el
backend esté realmente listo (no solo que el contenedor haya arrancado) antes de iniciar
el frontend.

## Cambios por repositorio

**Backend** (único repo afectado):
- `pom.xml`: agregar dependencia `spring-boot-starter-actuator`.
- `src/main/resources/application.properties`: agregar
  `management.endpoints.web.exposure.include=health,info` y
  `management.endpoint.health.show-details=never`.
- `docker-compose.yml` (raíz del repo): agregar `healthcheck` al servicio `backend`
  contra `/actuator/health`; cambiar `frontend.depends_on.backend` a
  `condition: service_healthy`.

No se toca Frontend ni Docs.

## Decisiones técnicas

- **Actuator vs. check manual:** se usa Actuator (opción A ya aprobada en la spec) en
  vez de un chequeo contra un endpoint de negocio existente — es el mecanismo estándar
  de Spring Boot para esto, y queda disponible para observabilidad futura.
- **`show-details=never`:** se descarta `always` porque el endpoint cae bajo
  `anyRequest().permitAll()` (spec 007) y expondría detalle de componentes (ej.
  conectividad a Postgres) sin autenticación.
- **Comando del `healthcheck` en Docker:** la imagen runtime es
  `eclipse-temurin:21-jre-alpine`, basada en Alpine (BusyBox), que normalmente ya trae
  `wget`. Se usa `wget --spider` como test. **Riesgo abierto:** esto se confirma recién
  cuando se autorice `docker compose up` (regla 5) — si `wget` no está disponible en la
  imagen, la tarea T04 se ajusta ahí mismo (agregar `curl`/`wget` al Dockerfile).

## Modelo de datos

No aplica — sin cambios de esquema ni migraciones.

## Contratos

Nuevos endpoints (provistos por el framework, sin código propio):
- `GET /actuator/health` → `200 {"status":"UP"}` o `503 {"status":"DOWN"}` si algún
  componente falla (ej. Postgres no responde). Sin detalle de componentes.
- `GET /actuator/info` → `200` (cuerpo vacío `{}`, no se agrega plugin de build-info en
  esta spec).

## Cómo se verifica

Cada verificación requiere autorización de ejecución explícita (regla 5), se pide en el
momento:

| Criterio de la spec | Cómo se comprueba |
|---|---|
| `/actuator/health` responde 200 sin detalle | `curl -s http://localhost:8080/actuator/health` con la app corriendo local |
| `/actuator/info` responde 200 | `curl -s http://localhost:8080/actuator/info` |
| `docker-compose.yml` con healthcheck en `backend` | Lectura del archivo (no requiere ejecución) |
| `frontend` espera `service_healthy` | Lectura del archivo (no requiere ejecución) |
| El proyecto compila | `mvn compile` o `mvn package -DskipTests` |
| Integración real end-to-end | `docker compose up` + `docker compose ps` mostrando `backend` como `healthy` |

Al cerrar todas las tareas: actualizar `PLAN-VERIFICACION.md` con la sección de la spec
028 (procedimiento probado del Backend, regla 4), y recién ahí proponer el commit.
