# 025 — Tareas

- [x] T01 — Crear dominio: `AssociatedEstablishment` (agregado), `EstablishmentKind`
      (enum), `InvalidEstablishmentException`, `EstablishmentNotFoundException` ·
      repo: backend · ~25 min
- [x] T02 — Crear puertos: comandos y casos de uso `in` (Create/Query/Deactivate/
      Reactivate) y `EstablishmentRepositoryPort` (`out`) · repo: backend · ~20 min ·
      depende de T01
- [x] T03 — Migración `V21__create_establishments.sql` + `EstablishmentEntity` +
      `EstablishmentJpaRepository` + `EstablishmentRepositoryAdapter` · repo: backend ·
      ~25 min · depende de T02
- [x] T04 — Servicios de aplicación: `CreateEstablishmentService` (valida tenant
      existente/activo), `EstablishmentQueryService`, `DeactivateEstablishmentService`,
      `ReactivateEstablishmentService` · repo: backend · ~25 min · depende de T03
- [x] T05 — `EstablishmentController` + `EstablishmentRequest`/`EstablishmentResponse` +
      manejo de excepciones (`400`/`404`/`409`) · repo: backend · ~25 min · depende de T04
- [x] T06 — Verificar los 7 criterios de aceptación de la spec con `curl` (arranque del
      servidor bajo autorización explícita) y confirmar que el proyecto compila sin
      afectar specs 001-024 · repo: backend · ~15 min · depende de T05
