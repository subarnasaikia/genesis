package com.genesis.api.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * Module-boundary guard (ARCHITECTURE_AUDIT A-009).
 *
 * <p>Invariant: a feature module must not reach into another module's data
 * access — no production class may depend on a <em>different</em> module's
 * {@code ..repository..} or {@code ..entity..} package. Cross-module needs go
 * through events, DTOs, or an outbound port implemented in {@code genesis-api}
 * (the composition root) — see {@code RecipientDirectory}/A-004 and
 * {@code UserDetailsServiceImpl}/A-007 for the established pattern.
 *
 * <p>Exemptions:
 * <ul>
 *   <li>{@code com.genesis.api} — the composition root; it wires every module and
 *       hosts the adapters, so it is allowed to touch any repository/entity.</li>
 *   <li>{@code com.genesis.common} — the shared kernel ({@code BaseEntity}, value
 *       objects); depending on it is not cross-module coupling.</li>
 *   <li>A class depending on its own module's repository/entity.</li>
 * </ul>
 *
 * <p>This rule is <strong>frozen</strong>: the {@code archunit_store} baseline
 * records the cross-module reaches that exist today (A-001/A-002/A-005/A-006
 * debt) so the build stays green, but any <em>new</em> violation fails the build.
 * As each boundary fix lands, re-running this test prunes the now-absent entries
 * from the store, ratcheting the debt down. The store file is the live worklist.
 *
 * <p>Scope note: the audit's "service imports" half is intentionally NOT enforced
 * here. The dominant cross-module service dependency is {@code
 * WorkspaceAccessControl} (called by annotation services for membership checks —
 * a sanctioned cross-cutting security primitive, not debt). Banning service→service
 * wholesale would flag that intentional design. The one genuine service-tangle
 * (A-001, {@code MentionService → DocumentService}) is tracked as its own fix.
 */
class ModuleBoundaryTest {

    private static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.genesis");

    private static String moduleOf(String packageName) {
        // com.genesis.<module>....
        String[] parts = packageName.split("\\.");
        return parts.length >= 3 ? parts[2] : packageName;
    }

    @Test
    void featureModulesMustNotReachIntoAnotherModulesDataAccess() {
        ArchRule rule = classes()
                .that().resideOutsideOfPackages("com.genesis.api..", "com.genesis.common..")
                .should(respectModuleDataAccessBoundaries());

        FreezingArchRule.freeze(rule).check(PRODUCTION_CLASSES);
    }

    private static ArchCondition<JavaClass> respectModuleDataAccessBoundaries() {
        return new ArchCondition<>("not depend on another module's repository or entity package") {
            @Override
            public void check(JavaClass origin, ConditionEvents events) {
                String originModule = moduleOf(origin.getPackageName());
                origin.getDirectDependenciesFromSelf().forEach(dependency -> {
                    String targetPackage = dependency.getTargetClass().getPackageName();
                    if (!targetPackage.startsWith("com.genesis.")) {
                        return;
                    }
                    String targetModule = moduleOf(targetPackage);
                    if (targetModule.equals(originModule) || targetModule.equals("common")) {
                        return; // same module or shared kernel — allowed
                    }
                    boolean targetIsDataAccess =
                            targetPackage.contains(".repository") || targetPackage.contains(".entity");
                    if (targetIsDataAccess) {
                        events.add(SimpleConditionEvent.violated(origin, dependency.getDescription()));
                    }
                });
            }
        };
    }
}
