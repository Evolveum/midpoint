/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.common.expression;

import com.evolveum.midpoint.common.expression.script.ScriptEvaluator;
import com.evolveum.midpoint.common.expression.script.jsr223.Jsr223ScriptEvaluator;
import com.evolveum.midpoint.prism.PrismContext;

import java.io.File;

/**
 * @author Radovan Semancik
 */
public class TestJavaScriptExpressions extends AbstractScriptTest {

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.common.expression.AbstractExpressionTest#createEvaluator()
	 */
	@Override
	protected ScriptEvaluator createEvaluator(PrismContext prismContext) {
		return new Jsr223ScriptEvaluator("JavaScript", prismContext);
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.common.expression.AbstractExpressionTest#getTestDir()
	 */
	@Override
	protected File getTestDir() {
		return new File(BASE_TEST_DIR, "javascript");
	}


}
