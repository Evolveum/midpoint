/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.validator.processor;

import java.io.File;
import java.util.function.Consumer;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.validator.ObjectUpgradeValidator;

import com.evolveum.midpoint.schema.validator.UpgradeValidationResult;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.AbstractSchemaTest;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

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
    public void test100TestResource() throws Exception {
        testUpgradeValidator("resource.xml", result -> {
            Assertions.assertThat(result.getItems())
                    .isNotNull()
                    .hasSize(2);
        });
    }
}
