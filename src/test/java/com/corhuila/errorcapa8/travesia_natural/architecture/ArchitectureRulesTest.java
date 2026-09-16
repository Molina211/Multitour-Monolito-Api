package com.corhuila.errorcapa8.travesia_natural.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
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

    @ArchTest
    static final ArchRule application_should_not_depend_on_adapters =
            noClasses()
                    .that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().haveSimpleNameEndingWith("Adapter")
                    .because("Hexagonal architecture: application must depend on ports (*Port), "
                            + "never directly on adapters (*Adapter) (CLAUDE.md section 7)");

    // A module's "domain" package (ports, model, exceptions) is its public contract -
    // other modules may depend on it freely, same as "common". Only a module's
    // "application" and "infrastructure" packages are internal implementation details;
    // depending on those from a DIFFERENT module is the real bounded-context leak.
    @ArchTest
    static final ArchRule modules_should_not_depend_on_each_others_internals =
            noClasses().should(new ArchCondition<JavaClass>(
                    "not depend on another module's application or infrastructure classes") {
                @Override
                public void check(JavaClass item, ConditionEvents events) {
                    String itemModule = moduleOf(item);
                    if (itemModule == null) {
                        return;
                    }
                    item.getDirectDependenciesFromSelf().forEach(dependency -> {
                        JavaClass target = dependency.getTargetClass();
                        String targetModule = moduleOf(target);
                        boolean crossesModuleBoundary = targetModule != null && !targetModule.equals(itemModule);
                        boolean touchesInternalLayer = target.getPackageName().contains(".application.")
                                || target.getPackageName().contains(".infrastructure.");
                        if (crossesModuleBoundary && touchesInternalLayer) {
                            events.add(SimpleConditionEvent.violated(item,
                                    item.getFullName() + " depends on " + target.getFullName()
                                            + ", an internal (application/infrastructure) class of "
                                            + "module '" + targetModule + "'"));
                        }
                    });
                }
            }).because("DDD: business modules are separate bounded contexts; a module's domain "
                    + "package (ports, model, exceptions) is its public contract, but its "
                    + "application/infrastructure packages are internal and must not be depended "
                    + "on by another module. 'common' is intentionally shared, excluded "
                    + "(CLAUDE.md section 7)");

    private static String moduleOf(JavaClass javaClass) {
        String marker = "travesia_natural.";
        String packageName = javaClass.getPackageName();
        int start = packageName.indexOf(marker);
        if (start < 0) {
            return null;
        }
        String rest = packageName.substring(start + marker.length());
        int dot = rest.indexOf('.');
        String segment = dot < 0 ? rest : rest.substring(0, dot);
        return "common".equals(segment) || segment.isEmpty() ? null : segment;
    }

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
