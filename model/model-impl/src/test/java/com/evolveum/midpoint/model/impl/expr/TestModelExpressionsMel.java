/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.expr;

import java.io.File;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrimitiveType;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.schema.expression.VariablesMap;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

/**
 * @author semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-model-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestModelExpressionsMel extends AbstractModelExpressionsTest {

    @Override
    protected File getTestDir() {
        return new File(BASE_TEST_DIR, "mel");
    }


    @Test
    public void testLibHello0Simple() throws Exception {
        PrismContainerValue<Containerable> customPcv = createCustomValue();

        assertExecuteScriptExpressionString(VariablesMap.create(prismContext), "Hello world!");
    }

    @Test
    public void testLibHello1Simple() throws Exception {
        PrismContainerValue<Containerable> customPcv = createCustomValue();

        VariablesMap variables = VariablesMap.create(prismContext,
                "foo", "Foobar", PrimitiveType.STRING);

        assertExecuteScriptExpressionString(variables, "Hello Foobar");
    }
}
