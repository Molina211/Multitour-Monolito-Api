package com.corhuila.errorcapa8.travesia_natural.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

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

    @ArchTest
    static final ArchRule application_should_not_depend_on_adapters =
            noClasses()
                    .that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().haveSimpleNameEndingWith("Adapter")
                    .because("Hexagonal architecture: application must depend on ports (*Port), "
                            + "never directly on adapters (*Adapter) (CLAUDE.md section 7)");

    @ArchTest
    static final ArchRule modules_should_not_depend_on_each_other =
            slices().matching("..travesia_natural.(*)..")
                    .should().notDependOnEachOther()
                    .ignoreDependency(alwaysTrue(), resideInAPackage("..common.."))
                    .because("DDD: business modules are separate bounded contexts and must not "
                            + "depend on each other's internals; 'common' is intentionally shared "
                            + "cross-cutting code, excluded from this rule (CLAUDE.md section 7)");

    @ArchTest
    static final ArchRule services_should_not_depend_on_adapters =
            noClasses()
                    .that().haveSimpleNameEndingWith("Service")
                    .should().dependOnClassesThat().haveSimpleNameEndingWith("Adapter")
                    .because("SOLID-DIP: *Service classes must depend only on *Port abstractions, "
                            + "never on concrete *Adapter implementations (CLAUDE.md section 7)");

    // Note: CLAUDE.md section 5 and the Docs domain model name this entity "Customer", but no
    // such class exists in code yet - the implemented aggregate is "Membership" (tenants module,
    // "Identity and Access bounded context"). This rule targets the real class name.
    @ArchTest
    static final ArchRule tenant_scoped_entities_should_declare_tenant_id =
            classes()
                    .that().haveSimpleName("Reservation")
                    .or().haveSimpleName("Membership")
                    .should(new ArchCondition<JavaClass>("declare a tenantId field") {
                        @Override
                        public void check(JavaClass item, ConditionEvents events) {
                            boolean hasTenantId = item.getFields().stream()
                                    .anyMatch(field -> field.getName().equals("tenantId"));
                            if (!hasTenantId) {
                                events.add(SimpleConditionEvent.violated(item,
                                        item.getFullName() + " does not declare a tenantId field"));
                            }
                        }
                    })
                    .because("Multitenancy invariant INV-TEN-001: tenant-scoped entities must "
                            + "declare a tenantId field (CLAUDE.md section 5)");

}
