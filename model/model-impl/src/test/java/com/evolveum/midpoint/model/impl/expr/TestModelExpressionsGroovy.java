/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.expr;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-model-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestModelExpressionsGroovy extends AbstractModelExpressionsTest {

    @Override
    protected File getTestDir() {
        return new File(BASE_TEST_DIR, "groovy");
    }
}
