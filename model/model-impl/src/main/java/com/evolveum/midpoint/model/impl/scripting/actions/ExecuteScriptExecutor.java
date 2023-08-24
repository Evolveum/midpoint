/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.scripting.actions;

import static com.evolveum.midpoint.model.impl.scripting.actions.ExecuteScriptExecutor.ScriptExecutionParameters;
import static com.evolveum.midpoint.util.MiscUtil.configCheck;

import java.util.List;

import jakarta.annotation.PostConstruct;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.BulkAction;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionFactory;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.impl.scripting.PipelineData;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptActionExpressionType;

/**
 * Executes "execute-script" (s:execute) actions.
 */
@Component
public class ExecuteScriptExecutor extends AbstractExecuteExecutor<ScriptExecutionParameters> {

    private static final String PARAM_SCRIPT = "script";

    @Autowired private ScriptExpressionFactory scriptExpressionFactory;

    @PostConstruct
    public void init() {
        actionExecutorRegistry.register(this);
    }

    @Override
    public @NotNull BulkAction getActionType() {
        return BulkAction.EXECUTE_SCRIPT;
    }

    @Override
    public PipelineData execute(
            ActionExpressionType action, PipelineData input, ExecutionContext context, OperationResult globalResult)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException, SecurityViolationException,
            PolicyViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {

        ScriptExpressionEvaluatorType script = expressionHelper.getActionArgument(
                ScriptExpressionEvaluatorType.class, action,
                ExecuteScriptActionExpressionType.F_SCRIPT, PARAM_SCRIPT, input, context, null,
                PARAM_SCRIPT, globalResult);

        configCheck(script != null, "No script provided");

        ScriptExecutionParameters parameters =
                getParameters(action, input, context, globalResult, p -> new ScriptExecutionParameters(script, p));

        return executeInternal(input, parameters, context, globalResult);
    }

    @Override
    <I> Object doSingleExecution(ScriptExecutionParameters parameters, TypedValue<I> inputTypedValue,
            VariablesMap externalVariables, ExecutionContext context, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException {

        var scriptExpression = scriptExpressionFactory.createScriptExpression(
                parameters.script,
                parameters.outputDefinition,
                context.getExpressionProfile(),
                "script", result);

        VariablesMap variables = createVariables(externalVariables);

        variables.put(ExpressionConstants.VAR_INPUT, inputTypedValue);

        LensContext<?> lensContext = getLensContext(externalVariables);
        List<?> rv = ModelImplUtils.evaluateScript(
                scriptExpression, lensContext, variables, true,
                "in '" + getName() + "' action", context.getTask(), result);

        if (rv.isEmpty()) {
            return null;
        } else if (rv.size() == 1) {
            return rv.get(0);
        } else {
            return rv; // shouldn't occur; would cause problems
        }
    }

    static class ScriptExecutionParameters extends Parameters {

        final ScriptExpressionEvaluatorType script;

        ScriptExecutionParameters(ScriptExpressionEvaluatorType script, Parameters p) {
            super(p.outputDefinition, p.forWholeInput, p.quiet);
            this.script = script;
        }
    }
}
