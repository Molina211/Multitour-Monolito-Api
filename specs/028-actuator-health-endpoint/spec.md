# 028 — Actuator Health Endpoint for Backend Container Healthcheck

**Status:** TERMINADA (2026-09-13)
**Date:** 2026-09-13
**Repos affected:** backend
**Related HU:** HU-04 (MVP — Corte 1 — Backend, `code-corhuila/projects/28`, issue #5 in
`Molina211/Multitour-Monolito-Api`). No new backlog item is created — a checklist item
was added to HU-04 instead, following the same pattern already used for the Docker work
from Semana 05, Sesión 1 (same issue, same "Configuración — rama `develop`" section).
This is infrastructure/tooling work, not a product User Story — branch uses the non-HU
`chore/` convention (`git-conventions.md`), not `hu-back-{N}-...`.

## Problem

The Backend service in `docker-compose.yml` has no way to confirm the application is
actually ready to receive traffic — only that its container process started. Because of
that, `frontend`'s `depends_on: backend` is a plain dependency (waits for the container
to start, not for the app to be healthy), which does not satisfy the Weekly Challenge
Semana 06 requirement of "health checks gating startup" for the full `docker compose up`.

## Scope

- Add `spring-boot-starter-actuator` to `pom.xml`.
- Expose `/actuator/health` and `/actuator/info` via
  `management.endpoints.web.exposure.include=health,info`.
- Set `management.endpoint.health.show-details=never` (health check returns only
  `{"status":"UP"}` / `{"status":"DOWN"}`, no component detail).
- Add a `healthcheck` to the `backend` service in `docker-compose.yml` against
  `/actuator/health`.
- Change `frontend`'s `depends_on: backend` to `condition: service_healthy` in
  `docker-compose.yml`.

## Out of scope

- No component-level detail exposed on `/actuator/health` (`show-details=never`) — the
  endpoint sits under the existing `anyRequest().permitAll()` rule (spec 007), so anyone
  reaching it would see that detail; keeping it minimal avoids leaking internal state
  (e.g. database connectivity) to unauthenticated callers.
- No `/actuator/metrics`, `/actuator/env`, or any other Actuator endpoint beyond
  `health` and `info`.
- No authentication/authorization changes to `SecurityConfig` — Actuator's default
  endpoints already fall under the existing `permitAll()` rule, nothing to add there.
- No changes to the `postgres` healthcheck in `docker-compose.yml` — already correct.
- No changes to the Frontend's own `docker-compose.yml`, `Dockerfile`, or `nginx.conf`
  (authored by Fernanda Robayo, read-only per rule 11). The network inconsistency
  already flagged to the human (nginx proxies to `host.docker.internal:8081` instead of
  the shared-network service name `backend:8080`) stays out of this spec.
- No custom `HealthIndicator` beans — Spring Boot Actuator already wires an automatic
  `DataSource` health check once `spring-boot-starter-data-jpa` + a `DataSource` are on
  the classpath; nothing custom is built.
- No updates to `04-requirements/user-stories.md` or new Docs epic — this work does not
  map to any of the 6 existing product epics and has no PDR source; it is tracked only
  via this spec and the HU-04 checklist item, not the 25-HU Gherkin registry.

## Acceptance criteria

- [x] `GET /actuator/health` returns `200` with `{"status":"UP"}` when the app and its
      Postgres connection are healthy (verified with `curl` against the running
      `docker compose` stack, 2026-09-13).
- [x] `GET /actuator/health` does not include component-level detail in its response
      body (`show-details=never` confirmed — response was
      `{"groups":["liveness","readiness"],"status":"UP"}`).
- [x] `GET /actuator/info` returns `200` (verified, empty body `{}`).
- [x] `docker-compose.yml` defines a `healthcheck` for the `backend` service using
      `/actuator/health`.
- [x] `docker-compose.yml`'s `frontend` service uses
      `depends_on: backend: condition: service_healthy`.
- [x] The project still compiles after the `pom.xml` change (`mvn compile`, verified).

## Multitenancy impact

Not applicable — this is a generic infrastructure health check, exposes no tenant data.

## Risks and open decisions

None open — all resolved for this spec:
1. Endpoint scope (health-only vs. health+info): resolved, both exposed.
2. Health detail level: resolved, `show-details=never` after the security concern raised
   (unauthenticated component detail was rejected).
3. Backlog tracking: resolved, HU-04 checklist item added, no new backlog item.
4. Branch: resolved, `chore/028-actuator-health-endpoint` → `develop`.

## Evidence for the course

Directly supports the "health checks gating startup" criterion of the Weekly Challenge
Semana 06, Sesión 1 ("Bring your whole system up with a single `docker compose up`").
