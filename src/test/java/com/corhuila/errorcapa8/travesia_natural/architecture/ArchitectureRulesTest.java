package com.corhuila.errorcapa8.travesia_natural.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Automated architecture conformance tests for the non-negotiable standards in
 * CLAUDE.md section 7 (Hexagonal, DDD, SOLID-DIP) and the multitenancy invariant
 * INV-TEN-001. See specs/029-archunit-architecture-tests/spec.md.
 */
@AnalyzeClasses(
        packages = "com.corhuila.errorcapa8.travesia_natural",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class ArchitectureRulesTest {

    @ArchTest
    static final ArchRule domain_should_not_depend_on_infrastructure =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                    .because("Hexagonal architecture: domain must not depend on infrastructure "
                            + "(CLAUDE.md section 7)");

    // Remaining rules added in T04-T07.

}
