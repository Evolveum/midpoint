/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression.script;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.common.configuration.api.ExpressionsConfigurationSection;
import com.evolveum.midpoint.model.common.expression.ExpressionTestUtil;
import com.evolveum.midpoint.model.common.expression.script.velocity.VelocityScriptExecutor;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;

import org.testng.annotations.Test;

import java.io.IOException;

import static org.testng.AssertJUnit.fail;

public class TestVelocityExpressions extends AbstractVelocityExpressionsTest {

    @Override
    protected ScriptExecutor createExecutor(PrismContext prismContext, Protector protector, Clock clock, boolean restrictedMode) {
        return new VelocityScriptExecutor(
                prismContext,
                protector,
                localizationService,
                ExpressionTestUtil.testingExpressionsConfiguration(restrictedMode),
                ExpressionTestUtil.testingExpressionsConfigurationView());
    }

    /**
     * If {@link ExpressionsConfigurationSection#safeExpressionsOnly()} is set to {@code true}, then the script evaluator
     * should not be able to execute scripts that are not safe, like those in Velocity.
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
