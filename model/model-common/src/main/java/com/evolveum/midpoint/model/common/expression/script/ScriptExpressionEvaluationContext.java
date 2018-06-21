/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.model.common.expression.script;

import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

/**
 * @author semancik
 *
 */
public class ScriptExpressionEvaluationContext {

	private static final ThreadLocal<ScriptExpressionEvaluationContext> threadLocalContext = new ThreadLocal<>();

	private final ExpressionVariables variables;
	private final String contextDescription;
	private final OperationResult result;
	private final Task task;
	private final ScriptExpression scriptExpression;
	private boolean evaluateNew = false;

	ScriptExpressionEvaluationContext(ExpressionVariables variables, String contextDescription,
			OperationResult result, Task task, ScriptExpression scriptExpression) {
		super();
		this.variables = variables;
		this.contextDescription = contextDescription;
		this.result = result;
		this.task = task;
		this.scriptExpression = scriptExpression;
	}

	public ExpressionVariables getVariables() {
		return variables;
	}

	public String getContextDescription() {
		return contextDescription;
	}

	public OperationResult getResult() {
		return result;
	}

	public Task getTask() {
		return task;
	}

	public ScriptExpression getScriptExpression() {
		return scriptExpression;
	}

	public boolean isEvaluateNew() {
		return evaluateNew;
	}

	public void setEvaluateNew(boolean evaluateNew) {
		this.evaluateNew = evaluateNew;
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
