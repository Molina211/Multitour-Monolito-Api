# 014 — Registro y consulta de colaboradores operativos

**Estado:** TERMINADA
**Fecha:** 2026-09-04
**Repos afectados:** backend
**HU relacionada:** Gestión de colaboradores del operador (PDR sección 9, "Gestión de
colaboradores del operador", líneas 121-126)

## Problema

`MembershipRole.OPERATIONAL_COLLABORATOR` existe en el enum del dominio (`tenants`)
desde spec 002, pero ningún caso de uso lo asigna — hoy solo se crea el primer
Administrator al dar de alta un tenant. El Frontend (rama `develop`, push de Fernanda
del 2026-09-03) ya tiene pantallas reales para que el Administrador registre, liste y
vea el detalle de colaboradores (`collaborators`, `collaborators/new`,
`collaborators/detail`), pero corren en simulación local (`localStorage`) porque no
existe backend que las respalde.

## Alcance

- Nuevo caso de uso: registrar un Colaborador operativo dentro del tenant activo
  (nombre, correo, contraseña inicial + confirmación), reutilizando `PasswordPolicy`
  y el patrón `Membership.createX(...)` ya existente.
- Rol fijo: `OPERATIONAL_COLLABORATOR` — no se permite elegir otro rol al registrar.
- Aislamiento por tenant: el colaborador queda asociado únicamente al tenant que lo
  registra (mismo patrón que Administrator/End Customer).
- Endpoint de consulta: listar los colaboradores del tenant y consultar el detalle de
  uno por id — mismo patrón `listAll`/`getById` de `TenantController`.
- Rechazo de correo duplicado dentro del mismo tenant (mismo patrón de
  `EmailAlreadyRegisteredException`, ya usado en spec 003).
- Registro de auditoría de la creación (mismo patrón `AuditRecorder` ya usado en
  `CreateTenantService`).
- El colaborador registrado puede iniciar sesión de inmediato: `LoginService` ya es
  agnóstico al rol, no requiere cambios.

## Fuera de alcance

- Administración de roles o permisos personalizados por colaborador (PDR línea 126:
  "no habilita administración avanzada de roles ni permisos personalizados").
- Editar, desactivar o eliminar un colaborador ya registrado — el Frontend actual no
  tiene esas pantallas.
- El toggle "el colaborador puede validar soportes de transferencia" (PDR línea 115)
  — ver decisión abierta 2.
- Enforcement de JWT/roles sobre los endpoints de este spec — sigue `permitAll()`
  como el resto del proyecto (deuda conocida; spec 007 solo protegió creación de
  reservas).
- El selector de rol staff/colaborador en el login del Frontend — es una simulación
  de UI ya resuelta ahí, no requiere nada del Backend.

## Criterios de aceptación

- [ ] Dado un tenant activo con un Administrador, cuando se registra un colaborador
      con nombre, correo y contraseña válidos (política de 8+ caracteres, mayúscula,
      minúscula, número y carácter especial), entonces se crea una `Membership` con
      `role=OPERATIONAL_COLLABORATOR` asociada a ese tenant y queda auditada.
- [ ] Dado un correo ya registrado en ese mismo tenant (con cualquier rol), cuando se
      intenta registrar un colaborador con ese correo, entonces la operación se
      rechaza sin crear un segundo registro.
- [ ] Dado un colaborador ya registrado, cuando el Administrador consulta la lista de
      colaboradores del tenant, entonces aparece con nombre, correo y rol.
- [ ] Dado un colaborador ya registrado, cuando se consulta su detalle por id,
      entonces se devuelve su información sin exponer el `passwordHash`.
- [ ] Dado un colaborador de otro tenant, cuando se listan los colaboradores del
      tenant activo, entonces no aparece (aislamiento).
- [ ] Dado un colaborador recién registrado, cuando inicia sesión con su correo y
      contraseña en el endpoint de login existente, entonces recibe un JWT válido con
      `role=OPERATIONAL_COLLABORATOR`.

## Impacto en multitenencia

Alto. El colaborador es una `Membership` más, sujeta a la misma invariante de
aislamiento por tenant que Administrator y End Customer (`INV-001`). La lista y el
detalle deben filtrar explícitamente por `tenantId`, igual que hace hoy el Frontend en
`operator-collaborator.service.ts` (comentario "PDR línea 95/1040" en ese archivo).

## Riesgos y decisiones abiertas

1. El Frontend pide un solo campo "nombre completo"; `Membership.java` separa
   `firstName`/`lastName`. Opciones: (a) reutilizar el split existente partiendo el
   nombre por el primer espacio, (b) agregar un modo de creación con un solo campo.
   Se resuelve en `/plan-tareas`.
2. El toggle "colaborador puede validar soportes de transferencia" (PDR línea 115, ya
   construido en el Frontend): ¿entra en el alcance de esta spec como un dato
   persistido sin enforcement (mismo estado que hoy: `DecidePaymentSupportService` no
   valida rol), o queda completamente fuera hasta que exista una spec de enforcement
   de JWT sobre pagos? Recomendación: dejarlo fuera — persistir un flag que nada lee
   todavía es trabajo a medias.
3. ¿El caso de uso vive en el módulo `tenants` (junto a `RegisterCustomerService`) o
   se separa? No cambia el problema ni los criterios de aceptación; se resuelve en
   `/plan-tareas`.

## Evidencia para materia

Cierra una de las 20 brechas confirmadas en el análisis Backend-vs-PDR del
2026-09-04, y destraba la simulación local del Frontend
(`operator-collaborator.service.ts`) para que pueda conectarse a un backend real
cuando el equipo decida activar `HttpClient`. Sirve como evidencia de HU-IAM
(colaboradores) para el corte de MVP.
