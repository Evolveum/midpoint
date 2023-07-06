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
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.AbstractSchemaTest;
import com.evolveum.midpoint.schema.validator.ObjectUpgradeValidator;
import com.evolveum.midpoint.schema.validator.UpgradeObjectsHandler;
import com.evolveum.midpoint.schema.validator.UpgradeValidationItem;
import com.evolveum.midpoint.schema.validator.UpgradeValidationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class TestUpgradeProcessors extends AbstractSchemaTest {

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

            UpgradeValidationItem item = assertGetItem(result, 0);
            Assertions.assertThat(item.getDelta().getModifiedItems()).hasSize(1);
            Assertions.assertThat(item.isChanged()).isTrue();
            // todo assert delta
        });
    }

    private UpgradeValidationItem assertGetItem(UpgradeValidationResult result, int index) {
        Assertions.assertThat(result).isNotNull();

        List<UpgradeValidationItem> items = result.getItems();
        Assertions.assertThat(items).hasSizeGreaterThan(index);

        return items.get(index);
    }
}
