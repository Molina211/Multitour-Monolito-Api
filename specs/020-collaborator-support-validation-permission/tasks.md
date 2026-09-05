# 020 — Tareas

- [ ] T01 — Agregar `allowCollaboratorSupportValidation` a `Tenant`
      (create/reconstitute) + método `updateCollaboratorSupportValidationPermission(...)` ·
      repo: backend · ~25 min
- [ ] T02 — Puerto de entrada
      `UpdateCollaboratorSupportValidationPermissionCommand/UseCase` · repo: backend ·
      ~15 min · depende de T01
- [ ] T03 — Application service con validación de actor `ADMINISTRATOR` (vía
      `MembershipRepositoryPort`) + auditoría · repo: backend · ~30 min · depende de T02
- [ ] T04 — Endpoint `PATCH` en `TenantController` + DTO de request · repo: backend ·
      ~25 min · depende de T03
- [ ] T05 — `TenantEntity` + `TenantRepositoryAdapter`: columna nueva + migración
      `V17__add_tenant_collaborator_support_permission.sql` · repo: backend · ~25 min ·
      depende de T01
- [ ] T06 — Crear `SupportValidationNotAllowedException` y modificar
      `DecidePaymentSupportService` para resolver el `Membership` del actor y aplicar la
      regla (Administrador siempre / Colaborador solo si el tenant lo permite) · repo:
      backend · ~30 min · depende de T01
- [ ] T07 — Revisar el manejador de excepciones existente (si hay uno central) para que
      `SupportValidationNotAllowedException` devuelva `403`; agregarlo si no existe un
      caso similar ya mapeado · repo: backend · ~15 min · depende de T06
- [ ] T08 — Verificar los 7 criterios de aceptación de la spec (curl manual) y agregar la
      sección "020" a `PLAN-VERIFICACION.md` · repo: backend · ~20 min · depende de T04,
      T05, T07
