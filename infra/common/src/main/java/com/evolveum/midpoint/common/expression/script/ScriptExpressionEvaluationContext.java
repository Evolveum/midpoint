/**
 * Copyright (c) 2013 Evolveum
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
 * Portions Copyrighted 2013 [name of copyright owner]
 */
package com.evolveum.midpoint.common.expression.script;

import com.evolveum.midpoint.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.schema.result.OperationResult;

/**
 * @author semancik
 *
 */
public class ScriptExpressionEvaluationContext {
	
	private static ThreadLocal<ScriptExpressionEvaluationContext> threadLocalContext = new ThreadLocal<ScriptExpressionEvaluationContext>();
	
	private ScriptVariables variables;
	private String contextDescription;
	private OperationResult result;
	private ScriptExpression scriptExpression;

	ScriptExpressionEvaluationContext(ScriptVariables variables, String contextDescription,
			OperationResult result, ScriptExpression scriptExpression) {
		super();
		this.variables = variables;
		this.contextDescription = contextDescription;
		this.result = result;
		this.scriptExpression = scriptExpression;
	}

	public ScriptVariables getVariables() {
		return variables;
	}

	public String getContextDescription() {
		return contextDescription;
	}

	public OperationResult getResult() {
		return result;
	}

	public ScriptExpression getScriptExpression() {
		return scriptExpression;
	}

	public void setupThreadLocal() {
		threadLocalContext.set(this);
	}
	
	public void cleanupThreadLocal() {
		threadLocalContext.set(null);
	}
	
	public static ScriptExpressionEvaluationContext getThreadLocal() {
		return threadLocalContext.get();
	}

}
