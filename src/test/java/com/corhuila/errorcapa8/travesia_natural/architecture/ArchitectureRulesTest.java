package com.corhuila.errorcapa8.travesia_natural.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;

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

    // Rules added in T03-T07.

}
