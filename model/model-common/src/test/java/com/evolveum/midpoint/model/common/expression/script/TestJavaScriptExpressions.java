/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression.script;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.common.expression.script.jsr223.Jsr223ScriptEvaluator;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.Protector;

import org.testng.annotations.Test;

import java.io.File;

/**
 * @author Radovan Semancik
 */
public class TestJavaScriptExpressions extends AbstractScriptTest {

    @Override
    protected ScriptEvaluator createEvaluator(PrismContext prismContext, Protector protector, Clock clock) {
        return new Jsr223ScriptEvaluator("JavaScript", prismContext, protector, localizationService);
    }

    @Override
    protected File getTestDir() {
        return new File(BASE_TEST_DIR, "javascript");
    }

    @Test(enabled = false) // #11085
    public void testExpressionListLiteral() throws Exception {

    }
}
