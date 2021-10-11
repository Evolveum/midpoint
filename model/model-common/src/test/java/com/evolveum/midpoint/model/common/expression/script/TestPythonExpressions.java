/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.script;

import com.evolveum.midpoint.model.common.expression.script.jsr223.Jsr223ScriptEvaluator;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.Protector;

import java.io.File;

/**
 * @author Radovan Semancik
 */
public class TestPythonExpressions extends AbstractScriptTest {

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.common.expression.AbstractExpressionTest#createEvaluator()
     */
    @Override
    protected ScriptEvaluator createEvaluator(PrismContext prismContext, Protector protector) {
        return new Jsr223ScriptEvaluator("python", prismContext, protector, localizationService);
    }

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.common.expression.AbstractExpressionTest#getTestDir()
     */
    @Override
    protected File getTestDir() {
        return new File(BASE_TEST_DIR, "python");
    }

}
