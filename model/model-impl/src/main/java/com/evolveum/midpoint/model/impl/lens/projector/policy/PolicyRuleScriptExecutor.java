/*
 * Copyright (c) 2010-2018 Evolveum
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
package com.evolveum.midpoint.model.impl.lens.projector.policy;

import com.evolveum.midpoint.model.api.ScriptExecutionException;
import com.evolveum.midpoint.model.api.context.*;
import com.evolveum.midpoint.model.impl.lens.EvaluatedAssignmentImpl;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.scripting.ScriptingExpressionEvaluator;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_4.*;
import com.evolveum.midpoint.xml.ns._public.model.scripting_4.ExecuteScriptType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_4.ValueListType;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Executes scripts defined in scriptExecution policy action.
 * Designed to be called during FINAL stage, just like notification action.
 *
 * HIGHLY EXPERIMENTAL
 *
 * @author mederly
 *
 */
@Component
public class PolicyRuleScriptExecutor {

	private static final Trace LOGGER = TraceManager.getTrace(PolicyRuleScriptExecutor.class);

	private static final String EXECUTE_SCRIPT_OPERATION = PolicyRuleScriptExecutor.class.getName() + ".executeScript";

	@Autowired private ScriptingExpressionEvaluator scriptingExpressionEvaluator;

	public <O extends ObjectType> void execute(@NotNull ModelContext<O> context, Task task, OperationResult result) {
		LensFocusContext<?> focusContext = (LensFocusContext<?>) context.getFocusContext();
		if (focusContext != null) {
			for (EvaluatedPolicyRule rule : focusContext.getPolicyRules()) {
				executeRuleScriptingActions(rule, context, task, result);
			}
			DeltaSetTriple<EvaluatedAssignmentImpl<?>> triple = ((LensContext<?>) context).getEvaluatedAssignmentTriple();
			if (triple != null) {
				for (EvaluatedAssignment<?> assignment : triple.getNonNegativeValues()) {
					for (EvaluatedPolicyRule rule : assignment.getAllTargetsPolicyRules()) {
						executeRuleScriptingActions(rule, context, task, result);
					}
				}
			}
		}
	}

	private void executeRuleScriptingActions(EvaluatedPolicyRule rule, ModelContext<?> context, Task task, OperationResult result) {
		if (rule.isTriggered()) {
			for (ScriptExecutionPolicyActionType action : rule.getEnabledActions(ScriptExecutionPolicyActionType.class)) {
				executeScriptingAction(action, rule, context, task, result);
			}
		}
	}

	private void executeScriptingAction(ScriptExecutionPolicyActionType action, EvaluatedPolicyRule rule, ModelContext<?> context, Task task, OperationResult parentResult) {
		LOGGER.debug("Executing policy action scripts ({}) in action: {}\non rule:{}",
				action.getExecuteScript().size(), action, rule.debugDumpLazily());
		List<ExecuteScriptType> executeScript = action.getExecuteScript();
		for (ExecuteScriptType executeScriptBean : executeScript) {
			executeScript(action, rule, context, task, parentResult, executeScriptBean);
		}
	}

	private void executeScript(ScriptExecutionPolicyActionType action, EvaluatedPolicyRule rule, ModelContext<?> context,
			Task task, OperationResult parentResult, ExecuteScriptType executeScriptBean) {
		OperationResult result = parentResult.createSubresult(EXECUTE_SCRIPT_OPERATION);
		try {
			Map<String, Object> initialVariables = createInitialVariables(action, rule, context);
			if (executeScriptBean.getInput() == null && context.getFocusContext() != null) {
				PrismObject objectAny = ((LensFocusContext) context.getFocusContext()).getObjectAny();
				if (objectAny != null) {
					ValueListType input = new ValueListType();
					input.getValue().add(objectAny.getValue().clone());
					executeScriptBean.setInput(input);
				}
			}
			scriptingExpressionEvaluator.evaluateExpression(executeScriptBean, initialVariables, false, task, result);
		} catch (ScriptExecutionException | RuntimeException e) {
			result.recordFatalError("Couldn't execute script policy action: " + e.getMessage(), e);
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't execute script with id={} in scriptExecution policy action '{}' (rule '{}'): {}",
					e, action.getId(), action.getName(), rule.getName(), e.getMessage());
		} finally {
			result.computeStatusIfUnknown();
		}
	}

	private Map<String, Object> createInitialVariables(ScriptExecutionPolicyActionType action, EvaluatedPolicyRule rule,
			ModelContext<?> context) {
		Map<String, Object> rv = new HashMap<>();
		rv.put(ExpressionConstants.VAR_POLICY_ACTION.getLocalPart(), action);
		rv.put(ExpressionConstants.VAR_POLICY_RULE.getLocalPart(), rule);
		rv.put(ExpressionConstants.VAR_MODEL_CONTEXT.getLocalPart(), context);
		return rv;
	}
}
