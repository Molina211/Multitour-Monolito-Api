# 029 — ArchUnit Architecture Conformance Tests

**Estado:** APROBADA
**Fecha:** 2026-09-16
**Repos afectados:** backend
**HU relacionada:** ninguna

## Problema
Compliance with the project's non-negotiable standards (CLAUDE.md §7: Hexagonal,
DDD, SOLID-DIP, and the multitenant isolation invariant INV-TEN-001) is verified
manually today, by reading code by hand — exactly as CLAUDE.md §7's own audit table
does for `reservations`. There is no automated, regression-proof check: a future
change could silently break a hexagonal boundary, cross a bounded-context boundary,
invert a DIP dependency, or drop the `tenantId` field, and nothing in the build would
catch it.

## Alcance
- Add ArchUnit as a test-scope Maven dependency to Backend.
- Write ArchUnit rules (JUnit 5 tests) applied to all 7 business modules (`cash`,
  `catalog`, `discounts`, `establishments`, `operations`, `reservations`, `tenants`),
  since they already share the same `application`/`domain`/`infrastructure` structure:
  1. **Hexagonal:** `domain` packages must not depend on `infrastructure` packages.
  2. **Hexagonal:** `application` classes must depend on `domain` only through
     `*Port` interfaces, never directly on `*Adapter` classes.
  3. **DDD:** internal classes of one module's `domain`/`application` packages must
     not be imported by another module (bounded-context isolation); only public
     contracts (ports, DTOs) may cross.
  4. **SOLID-DIP:** `*Service` classes (application layer) depend only on `*Port`
     interfaces, never on concrete `*Adapter` implementations.
  5. **Multitenancy (INV-TEN-001):** `Reservation` and `Membership` must declare a
     `tenantId` field. (Note: CLAUDE.md §5 and the Docs domain model call this entity
     "Customer", but no such class exists in code — the implemented aggregate is
     `Membership`, tenants module, "Identity and Access bounded context". Verified
     while implementing T07; this spec targets the real class name.)
- Run as part of the existing test suite (`mvn test`) — no new pipeline, no CI/CD
  (none exists in any repo, per CLAUDE.md §7).

## Fuera de alcance
- Checkstyle / Spotless — already discarded in a separate review; style/formatting is
  not this spec's objective.
- OCP / LSP verification — no concrete, checkable ArchUnit rule was identified for
  these without inventing criteria; stays out until one exists.
- CI/CD integration — doesn't exist in any repo; these tests run locally as part of
  the existing suite, same as the other 344 `@Test`.
- Strategy pattern implementation for discounts — separate, already-known gap
  (CLAUDE.md §5), not part of this spec.
- Domain events dispatch verification — flagged as unverified in CLAUDE.md §7's audit
  table, but a different concern from the 5 rules above; not included here.

## Criterios de aceptación
- [ ] `mvn test` runs an ArchUnit test suite alongside the existing tests.
- [ ] There is at least one ArchUnit rule per non-negotiable listed in Alcance (5
      rules total: Hexagonal ×2, DDD, SOLID-DIP, multitenancy).
- [ ] Each rule applies to all 7 business modules, not only `reservations`.
- [ ] A deliberately introduced violation (e.g. a `domain` class importing
      `infrastructure`) makes the corresponding ArchUnit rule fail — verified by hand
      before closing the spec.
- [ ] The suite stays green against the current codebase; any real violation the
      rules surface gets documented as a separate finding, not silently fixed inside
      this spec without an explicit decision.

## Impacto en multitenencia
Direct: rule 5 automates and permanently enforces INV-TEN-001 (`tenantId` required on
`Reservation` and `Membership`), which today is only documented, not checked.

## Riesgos y decisiones abiertas
1. Build time: ArchUnit scans the full classpath — expected impact is low at this
   project's size, but unmeasured until implemented.
2. If the scan surfaces a real, pre-existing violation (not hypothetical): fix it
   inside this spec, or document it as a separate finding to decide later? Open until
   it actually happens.
3. Maintenance: any future module or package refactor must stay aligned with these
   rules; a deliberate, valid break requires deciding whether to adjust the rule or
   the code.

## Evidencia para la materia
Closes a gap CLAUDE.md §7's own audit table already documents explicitly (OCP/LSP
"no se declaran cumplidos ni incumplidos" for lack of evidence; DIP/SRP/ISP verified
by hand, once). Moves from a one-time manual audit to an automated, permanent check —
strong evidence for the defensa that the non-negotiable standards are enforced, not
just documented.
