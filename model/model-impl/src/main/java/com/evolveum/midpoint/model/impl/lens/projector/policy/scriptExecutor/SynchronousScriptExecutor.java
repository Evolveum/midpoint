/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy.scriptExecutor;

import static com.evolveum.midpoint.schema.util.ExecuteScriptUtil.createInputCloned;
import static com.evolveum.midpoint.schema.util.ExecuteScriptUtil.implantInput;

import com.evolveum.midpoint.util.exception.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExecutionPolicyActionType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ValueListType;

/**
 * Executes specified scripts synchronously (i.e. immediately).
 */
class SynchronousScriptExecutor {

    private static final Trace LOGGER = TraceManager.getTrace(SynchronousScriptExecutor.class);

    private static final String OP_EXECUTE_SCRIPT = SynchronousScriptExecutor.class.getName() + ".executeScript";

    @NotNull private final ActionContext actx;

    SynchronousScriptExecutor(@NotNull ActionContext actx) {
        this.actx = actx;
    }

    void executeScripts(OperationResult parentResult) {
        for (ExecuteScriptType executeScriptBean : actx.action.getExecuteScript()) {
            executeScript(executeScriptBean, parentResult);
        }
    }

    private void executeScript(ExecuteScriptType specifiedExecuteScriptBean, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(OP_EXECUTE_SCRIPT);
        try {
            ExecuteScriptType executeScriptBean = addInputIfNeeded(specifiedExecuteScriptBean, result);
            VariablesMap initialVariables = createInitialVariables();
            actx.beans.scriptingExpressionEvaluator.evaluateExpression(executeScriptBean, initialVariables,
                    false, actx.task, result);
        } catch (Throwable t) {
            result.recordFatalError("Couldn't execute script policy action: " + t.getMessage(), t);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't execute script with id={} in scriptExecution policy action '{}' (rule '{}'): {}",
                    t, actx.action.getId(), actx.action.getName(), actx.rule.getName(), t.getMessage());
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private ExecuteScriptType addInputIfNeeded(ExecuteScriptType specifiedExecuteScriptBean, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        if (specifiedExecuteScriptBean.getInput() == null) {
            FullDataBasedObjectSet objectSet = new FullDataBasedObjectSet(actx, result);
            objectSet.collect();
            ValueListType input = createInputCloned(objectSet.asObjectValues());
            return implantInput(specifiedExecuteScriptBean, input);
        } else {
            return specifiedExecuteScriptBean;
        }
    }

    private VariablesMap createInitialVariables() {
        VariablesMap rv = new VariablesMap();
        actx.putIntoVariables(rv);
        rv.put(ExpressionConstants.VAR_MODEL_CONTEXT, actx.context, ModelContext.class);
        return rv;
    }
}
