# 001 — Tareas

**Batching (excepción regla 4 de `CLAUDE.md` — Backend, implementación inicial):**
lotes de 2-3 tareas, commit + push sin pedir permiso en cada uno, siempre que
`PLAN-VERIFICACION.md` exista y esté al día en el repo al momento del push. Por eso
T02 crea una versión inicial y las tareas siguientes la actualizan en vez de dejarla
para el final.

- [x] T01 — Agregar dependencias de Postgres y Flyway a `build.gradle` (`org.postgresql:postgresql`, `org.flywaydb:flyway-core`, `org.flywaydb:flyway-database-postgresql`) · repo: backend · ~15 min
- [x] T02 — Crear `docker-compose.yml` con servicio Postgres para desarrollo local, configurar `application.properties` (datasource, `spring.jpa.hibernate.ddl-auto=validate`, `spring.flyway.enabled=true`) y crear `PLAN-VERIFICACION.md` inicial con el paso `docker compose up -d` · repo: backend · ~20 min
  **— fin lote 1 (T01-T02): commit + push —**
- [x] T03 — Crear migración `db/migration/V1__create_reservations.sql` con las tablas `reservations` y `reserved_services`, `tenant_id NOT NULL` en ambas; agregar el paso de verificación del esquema a `PLAN-VERIFICACION.md` · repo: backend · ~30 min · depende de T01, T02
- [x] T04 — Crear el modelo de dominio: `Reservation`, `ReservedService`, `ReservationStatus`, `PaymentStatus`, `InvalidReservationException`, con la regla "al menos un servicio reservado" validada al construir · repo: backend · ~30 min
  **— fin lote 2 (T03-T04): commit + push —**
- [ ] T05 — Definir puertos `CreateReservationUseCase` (entrada) y `ReservationRepositoryPort` (salida) en `reservations/domain/port` · repo: backend · ~15 min · depende de T04
- [ ] T06 — Implementar `CreateReservationService` (capa aplicación): genera `reservationId` (UUID v4), arma el agregado, fija estado inicial `Pendiente de pago` / `Sin pago`, invoca el puerto de salida · repo: backend · ~20 min · depende de T05
- [ ] T07 — Implementar adaptador de persistencia: `ReservationEntity`, `ReservedServiceEntity`, `ReservationJpaRepository`, `ReservationRepositoryAdapter` · repo: backend · ~30 min · depende de T03, T05
  **— fin lote 3 (T05-T07): commit + push —**
- [ ] T08 — Implementar adaptador web: `ReservationController` (`POST /api/reservations`), DTOs de request/response/error, lectura de `X-Tenant-Id`, mapeo de `InvalidReservationException` a 400 · repo: backend · ~30 min · depende de T06
- [ ] T09 — Implementar `HealthController` (`GET /health`) en `common/web` · repo: backend · ~10 min
- [ ] T10 — Crear `SecurityConfig` en `common/security` que permite todas las peticiones temporalmente, con comentario explicando que es deuda hasta la spec de JWT · repo: backend · ~15 min
  **— fin lote 4 (T08-T10): commit + push —**
- [ ] T11 — Verificar que el contexto de Spring levanta con todos los beans nuevos; ajustar el test por defecto si hace falta · repo: backend · ~15 min · depende de T07, T08, T09, T10
- [ ] T12 — Completar `PLAN-VERIFICACION.md` con los comandos exactos de `curl` (`/health`, creación/rechazo de reserva) y la consulta `psql` de aislamiento por tenant · repo: backend · ~20 min · depende de T11
- [ ] T13 — Verificar los criterios de aceptación de `spec.md` ejecutando `PLAN-VERIFICACION.md` de punta a punta · repo: backend · ~20 min · depende de T12
  **— fin lote 5 (T11-T13): commit + push —**
