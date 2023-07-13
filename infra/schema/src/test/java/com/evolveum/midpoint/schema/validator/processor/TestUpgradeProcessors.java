/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.validator.processor;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.AbstractSchemaTest;
import com.evolveum.midpoint.schema.validator.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PersonaConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

public class TestUpgradeProcessors extends AbstractSchemaTest {

    private static final Trace LOGGER = TraceManager.getTrace(TestUpgradeProcessors.class);

    private static final File RESOURCES = new File("./src/test/resources/validator/processor");

    private PrismContext getPrismContext() {
        return PrismTestUtil.getPrismContext();
    }

    private <O extends ObjectType> void testUpgradeValidator(String fileName, Consumer<UpgradeValidationResult> resultConsumer) throws Exception {
        File file = new File(RESOURCES, fileName);
        Assertions.assertThat(file)
                .exists()
                .isFile()
                .isNotEmpty();

        PrismObject<O> object = PrismTestUtil.parseObject(file);

        Assertions.assertThat(object).isNotNull();

        ObjectUpgradeValidator validator = new ObjectUpgradeValidator(getPrismContext());
        validator.showAllWarnings();

        UpgradeValidationResult result = validator.validate(object);

        Assertions.assertThat(result).isNotNull();
        LOGGER.info("Validation result:\n{}", result.debugDump());

        resultConsumer.accept(result);
    }

    @Test
    public void test00CheckIdentifierUniqueness() {
        Map<String, Class<?>> identifiers = new HashMap<>();
        UpgradeObjectsHandler.PROCESSORS.forEach(p -> {
            String identifier = p.getIdentifier();
            Class<?> existing = identifiers.get(identifier);
            if (existing != null) {
                Assertions.fail("Processor (" + p.getClass().getName() + ") identifier (" + identifier
                        + ") is not unique, collides with class " + existing.getName());
            } else {
                identifiers.put(identifier, p.getClass());
            }
        });

        identifiers.keySet().stream()
                .sorted()
                .forEach(identifier -> LOGGER.info(identifier + " -> " + identifiers.get(identifier).getName()));
    }

    @Test
    public void test10TestResource() throws Exception {
        testUpgradeValidator("resource.xml", result -> {
            Assertions.assertThat(result.getItems())
                    .isNotNull()
                    .hasSize(2);

            // todo assert items
        });
    }

    @Test
    public void test20TestCaseTaskRef() throws Exception {
        testUpgradeValidator("case.xml", result -> {
            Assertions.assertThat(result.getItems()).hasSize(1);

            UpgradeValidationItem item = assertGetItem(result, new ProcessorMixin() {
            }.getIdentifier(CaseTaskRefProcessor.class));
            Assertions.assertThat(item.getDelta().getModifiedItems()).hasSize(1);
            Assertions.assertThat(item.isChanged()).isTrue();
            // todo assert delta
        });
    }

    private String getProcessorIdentifier(Class<?> processorClass) {
        return new ProcessorMixin() {
        }.getIdentifier(processorClass);
    }

    @Test
    public void test30TestSystemConfig() throws Exception {
        testUpgradeValidator("system-configuration.xml", result -> {
            Assertions.assertThat(result.getItems()).hasSize(2);

            UpgradeValidationItem item = assertGetItem(result, getProcessorIdentifier(RoleCatalogCollectionsProcessor.class));
            Assertions.assertThat(item.getDelta().getModifiedItems()).hasSize(2);
            Assertions.assertThat(item.isChanged()).isTrue();

            item = assertGetItem(result, getProcessorIdentifier(RoleCatalogRefProcessor.class));
            Assertions.assertThat(item.getDelta().getModifiedItems()).hasSize(2);
            Assertions.assertThat(item.isChanged()).isTrue();

            // todo assert deltas
        });
    }

    @Test
    public void test40TestRole() throws Exception {
        testUpgradeValidator("role.xml", result -> {
            Assertions.assertThat(result.getItems()).hasSize(1);

            UpgradeValidationItem item = assertGetItem(result, getProcessorIdentifier(PersonaTargetSubtypeProcessor.class));
            UpgradeValidationItemAsserter asserter = new UpgradeValidationItemAsserter(item);
            asserter.assertUnchanged();
            asserter.assertPhase(UpgradePhase.BEFORE);
            asserter.assertPath(ItemPath.create(
                    RoleType.F_ASSIGNMENT, 1L, AssignmentType.F_PERSONA_CONSTRUCTION, PersonaConstructionType.F_TARGET_SUBTYPE));
            Assertions.assertThat(item.getDelta().getModifiedItems()).isEmpty();

            // todo assert deltas
        });
    }

    private UpgradeValidationItem assertGetItem(UpgradeValidationResult result, String identifier) {
        Assertions.assertThat(result).isNotNull();

        List<UpgradeValidationItem> items = result.getItems();
        UpgradeValidationItem item = items.stream()
                .filter(i -> identifier.equals(i.getIdentifier()))
                .findFirst()
                .orElse(null);
        Assertions.assertThat(item).isNotNull();

        return item;
    }
}
