# 028 — Tareas

- [x] T01 — Agregar dependencia `spring-boot-starter-actuator` a `pom.xml` · repo: backend · ~5 min
- [x] T02 — Configurar `management.endpoints.web.exposure.include=health,info` y `management.endpoint.health.show-details=never` en `application.properties` · repo: backend · ~10 min (depende de T01)
- [x] T03 — Verificar que el proyecto compila (`mvn compile`, requiere autorización de ejecución) · repo: backend · ~5 min (depende de T02)
- [x] T04 — Agregar `healthcheck` al servicio `backend` en `docker-compose.yml` contra `/actuator/health` · repo: backend · ~15 min (depende de T02)
- [x] T05 — Cambiar `frontend.depends_on.backend` a `condition: service_healthy` en `docker-compose.yml` · repo: backend · ~5 min (depende de T04)
- [x] T06 — Verificar los criterios de aceptación de la spec 028 (curl local + `docker compose up`, requiere autorización de ejecución) y actualizar `PLAN-VERIFICACION.md` · repo: backend · ~20 min (depende de T01-T05)
