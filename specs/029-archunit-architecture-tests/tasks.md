# 029 — Tareas

- [ ] T01 — Agregar dependencia `archunit-junit5` (scope test) al `pom.xml`  · repo: backend · ~10 min
- [ ] T02 — Crear esqueleto de `ArchitectureRulesTest` con el `JavaClasses` importado del paquete base completo  · repo: backend · ~15 min · depende de T01
- [ ] T03 — Regla Hexagonal 1: `domain` no depende de `infrastructure`, en los 7 módulos  · repo: backend · ~20 min · depende de T02
- [ ] T04 — Regla Hexagonal 2: `application` solo depende de `domain` vía `*Port`, nunca de `*Adapter`  · repo: backend · ~20 min · depende de T02
- [ ] T05 — Regla DDD: aislamiento entre módulos (clases internas de `domain`/`application` de un módulo no se importan desde otro), excluyendo `common`  · repo: backend · ~25 min · depende de T02
- [ ] T06 — Regla SOLID-DIP: clases `*Service` dependen solo de `*Port`, nunca de `*Adapter` concreto  · repo: backend · ~20 min · depende de T02
- [ ] T07 — Regla multitenencia (INV-TEN-001): `Reservation` y `Customer` deben tener campo `tenantId`  · repo: backend · ~15 min · depende de T02
- [ ] T08 — Correr `mvn test`, confirmar las 5 reglas en verde contra el código actual; si aparece una violación real, documentarla aparte en vez de corregirla aquí sin decisión explícita  · repo: backend · ~15 min · depende de T03-T07
- [ ] T09 — Romper una regla a propósito en local, confirmar que falla, revertir sin comitear (verificación manual del criterio de aceptación correspondiente)  · repo: backend · ~10 min · depende de T08
- [ ] T10 — Verificar el resto de criterios de aceptación de la spec 029 y actualizar `PLAN-VERIFICACION.md` con esta sección cerrada  · repo: backend · ~15 min · depende de T09
