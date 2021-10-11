/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ValueListType;
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
                // We need to apply rules from all the assignments - even those that were deleted.
                for (EvaluatedAssignment<?> assignment : triple.getAllValues()) {
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
            VariablesMap initialVariables = createInitialVariables(action, rule, context);
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

    private VariablesMap createInitialVariables(ScriptExecutionPolicyActionType action, EvaluatedPolicyRule rule,
            ModelContext<?> context) {
        VariablesMap rv = new VariablesMap();
        rv.put(ExpressionConstants.VAR_POLICY_ACTION, action, ScriptExecutionPolicyActionType.class);
        rv.put(ExpressionConstants.VAR_POLICY_RULE, rule, EvaluatedPolicyRule.class);
        rv.put(ExpressionConstants.VAR_MODEL_CONTEXT, context, ModelContext.class);
        return rv;
    }
}
