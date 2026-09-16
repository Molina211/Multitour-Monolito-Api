# 029 — Plan técnico

## Enfoque
Una sola clase de test ArchUnit (`ArchitectureRulesTest`), no una por módulo: las 5
reglas se escriben contra el paquete base completo
(`com.corhuila.errorcapa8.travesia_natural..`) usando los predicados genéricos de
ArchUnit sobre nombres de paquete/clase (`..domain..`, `..infrastructure..`,
`*Port`, `*Adapter`, `*Service`), así que aplican a los 7 módulos automáticamente sin
duplicar código por módulo. El paquete `common` (cross-cutting: audit, security, web)
se excluye explícitamente de la regla de aislamiento DDD, porque está pensado para ser
compartido por los 7 módulos.

## Cambios por repositorio

**Backend** (único repo afectado):
- `pom.xml` — agregar `com.tngtech.archunit:archunit-junit5` en `<scope>test</scope>`.
- `src/test/java/com/corhuila/errorcapa8/travesia_natural/architecture/ArchitectureRulesTest.java`
  (nuevo) — las 5 reglas.

Verificado antes de planear (no supuesto): estructura real de paquetes por módulo es
`<modulo>/{application,domain{/exception,/model,/port/in,/port/out},infrastructure{/in/web,/out/persistence}}`
para los 7 módulos (`cash`, `catalog`, `discounts`, `establishments`, `operations`,
`reservations`, `tenants`); `Reservation.java` ya tiene `private final String tenantId`.

## Decisiones técnicas
- **Una clase de test, no siete** — alternativa descartada: una clase ArchUnit por
  módulo. Motivo: las reglas son idénticas para los 7, duplicar 7 veces el mismo
  predicado es puro boilerplate sin beneficio.
- **Excluir `common` de la regla DDD de aislamiento** — alternativa descartada:
  incluirlo. Motivo: `common` es intencionalmente compartido (audit/security/web
  cross-cutting), incluirlo generaría falsos positivos en los 7 módulos.
- **Regla de multitenencia vía `ArchCondition` a medida** (verifica que la clase tenga
  un campo llamado `tenantId`) en vez de un predicado built-in de ArchUnit — alternativa
  descartada: no existe un predicado de una línea en la API de ArchUnit para "debe
  tener un campo con este nombre"; el condition a medida son ~5 líneas, no amerita una
  librería aparte.

## Modelo de datos
No aplica — no se toca esquema ni migraciones.

## Contratos
No aplica — no se agregan ni cambian endpoints.

## Cómo se verifica
- `mvn test -Dtest=ArchitectureRulesTest` corre solo las 5 reglas nuevas.
- `mvn test` corre la suite completa (existente + nueva) y debe quedar en verde.
- Para el criterio "una violación deliberada hace fallar la regla": se rompe una regla
  a propósito en local (ej. mover un import de `infrastructure` dentro de `domain`),
  se confirma que el test correspondiente falla, y se revierte sin comitear ese cambio.
