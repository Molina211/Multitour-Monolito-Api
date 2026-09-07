# 014 — Tareas

- [x] T01 — `Membership.createOperationalCollaborator(tenantId, name, email,
      passwordHash)` + `CollaboratorNotFoundException`  · repo: backend · ~15 min
- [x] T02 — Puertos `in`: `RegisterCollaboratorCommand`, `RegisterCollaboratorUseCase`,
      `CollaboratorQueryUseCase`  · repo: backend · ~15 min · depende de T01
- [x] T03 — `MembershipRepositoryPort`: agregar `findAllByTenantIdAndRole` y
      `findByTenantIdAndMembershipId`  · repo: backend · ~10 min · depende de T01
- [x] T04 — `MembershipJpaRepository` + `MembershipRepositoryAdapter`: implementar los
      dos métodos nuevos del puerto  · repo: backend · ~20 min · depende de T03
- [x] T05 — `RegisterCollaboratorService` (tenant activo, `PasswordPolicy`, email
      único, `AuditRecorder`)  · repo: backend · ~25 min · depende de T02, T04
- [x] T06 — `CollaboratorQueryService` (`listByTenant`, `getById` con aislamiento por
      tenant + rol)  · repo: backend · ~15 min · depende de T02, T04
- [x] T07 — DTOs `RegisterCollaboratorRequest`/`CollaboratorResponse`  · repo: backend
      · ~10 min · depende de T01
- [x] T08 — `CollaboratorController` (`POST`/`GET`/`GET /{membershipId}` +
      `@ExceptionHandler` locales)  · repo: backend · ~25 min · depende de T05, T06, T07
- [x] T09 — Verificar los 6 criterios de aceptación de la spec: `./mvnw test` +
      secuencia curl completa (registro, duplicado, password débil, listado, detalle
      sin passwordHash, login del colaborador, aislamiento entre tenants) +
      `PLAN-VERIFICACION.md`  · repo: backend · ~25 min · depende de T08
