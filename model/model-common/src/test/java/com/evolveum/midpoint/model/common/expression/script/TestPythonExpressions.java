/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression.script;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.common.configuration.api.ExpressionsConfigurationSection;
import com.evolveum.midpoint.model.common.expression.script.jsr223.Jsr223ScriptEvaluator;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;

import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

import static com.evolveum.midpoint.model.common.expression.ExpressionTestUtil.testingExpressionsConfiguration;

import static org.testng.AssertJUnit.fail;

/**
 * @author Radovan Semancik
 */
public class TestPythonExpressions extends AbstractScriptTest {

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.common.expression.AbstractExpressionTest#createEvaluator()
     */
    @Override
    protected ScriptEvaluator createEvaluator(PrismContext prismContext, Protector protector, Clock clock, boolean restrictedMode) {
        return new Jsr223ScriptEvaluator(
                "python", prismContext, protector, localizationService, testingExpressionsConfiguration(restrictedMode));
    }

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.common.expression.AbstractExpressionTest#getTestDir()
     */
    @Override
    protected File getTestDir() {
        return new File(BASE_TEST_DIR, "python");
    }

    /**
     * If {@link ExpressionsConfigurationSection#isSafeExpressionsOnly()} is set to {@code true}, then the script evaluator
     * should not be able to execute scripts that are not safe, like those in Python.
     */
    @Test
    public void testInRestrictedMode() throws CommonException, IOException {
        switchToRestrictedMode();
        try {
            executeSimpleScript();
            fail("unexpected success");
        } catch (SecurityViolationException e) {
            assertExpectedException(e)
                    .hasMessageContaining("is not considered safe; script execution prohibited");
        } finally {
            switchToUnrestrictedMode();
        }
    }
}
