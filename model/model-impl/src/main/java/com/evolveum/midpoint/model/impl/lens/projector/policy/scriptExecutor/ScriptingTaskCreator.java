/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy.scriptExecutor;

import static com.evolveum.midpoint.model.api.util.ReferenceResolver.Source.MODEL;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createObjectRef;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStateType.RUNNABLE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.TaskSchedulingStateType.READY;

import com.evolveum.midpoint.model.api.util.ReferenceResolver;

import com.evolveum.midpoint.model.common.expression.ModelExpressionEnvironment;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.repo.common.expression.ExpressionEnvironmentThreadLocalHolder;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import java.util.List;

/**
 * Creates tasks of given type (single-run, iterative) for given (specified) executeScript beans.
 */
abstract class ScriptingTaskCreator {

    @NotNull final ActionContext actx;
    @NotNull final PolicyRuleScriptExecutor beans;
    @NotNull private final AsynchronousScriptExecutionType asynchronousScriptExecution;

    private static final String VAR_PREPARED_TASK = "preparedTask";

    ScriptingTaskCreator(@NotNull ActionContext actx) {
        this.actx = actx;
        this.beans = actx.beans;
        this.asynchronousScriptExecution = actx.action.getAsynchronousExecution();
    }

    /**
     * Main entry point. Creates a task.
     */
    abstract TaskType createTask(ExecuteScriptType executeScript, OperationResult result) throws CommunicationException,
            ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException,
            ExpressionEvaluationException;

    /**
     * Creates empty task of given type (single run, iterative), not related to any specific script.
     */
    TaskType createEmptyTask(OperationResult result)
            throws SecurityViolationException, ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException {
        MidPointPrincipal principal = beans.securityContextManager.getPrincipal();
        if (principal == null) {
            throw new SecurityViolationException("No current user");
        }

        AsynchronousScriptExecutionType asynchronousExecution = actx.action.getAsynchronousExecution();
        TaskType newTask;
        if (asynchronousExecution.getTaskTemplateRef() != null) {
            List<PrismObject<? extends ObjectType>> tasks =
                    beans.referenceResolver.resolve(
                            asynchronousExecution.getTaskTemplateRef(), null,
                            MODEL, createTaskFilterEvaluator(), actx.task, result);
            if (tasks.isEmpty()) {
                throw new ObjectNotFoundException("Task template was specified but was not found", TaskType.class, null);
            } else if (tasks.size() > 1) {
                throw new IllegalStateException("Task template reference resolution lead to more than one task template: " + tasks);
            } else {
                newTask = (TaskType) tasks.get(0).asObjectable();
            }
        } else {
            newTask = new TaskType();
            newTask.setName(PolyStringType.fromOrig("Execute script"));
        }
        newTask.setName(PolyStringType.fromOrig(newTask.getName().getOrig() + " " + (int) (Math.random() * 10000)));
        newTask.setOid(null);
        newTask.setTaskIdentifier(null);
        newTask.setOwnerRef(createObjectRef(principal.getFocus()));
        newTask.setExecutionState(RUNNABLE);
        newTask.setSchedulingState(READY);
        return newTask;
    }

    private ReferenceResolver.FilterExpressionEvaluator createTaskFilterEvaluator() {
        return (rawFilter, result1) -> {
            ExpressionEnvironmentThreadLocalHolder.pushExpressionEnvironment(
                    new ModelExpressionEnvironment<>(actx.context, null, actx.task, result1));
            try {
                VariablesMap variables = actx.createVariables();
                ExpressionProfile expressionProfile = MiscSchemaUtil.getExpressionProfile();
                return ExpressionUtil.evaluateFilterExpressions(
                        rawFilter, variables, expressionProfile,
                        beans.expressionFactory,
                        "evaluating task template filter expression ", actx.task, result1);
            } finally {
                ExpressionEnvironmentThreadLocalHolder.popExpressionEnvironment();
            }
        };
    }

    TaskType customizeTask(TaskType preparedTask, OperationResult result) throws SchemaException,
            ObjectNotFoundException, SecurityViolationException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException {
        ExpressionType customizer = asynchronousScriptExecution.getTaskCustomizer();
        if (customizer == null) {
            return preparedTask;
        } else {
            ExpressionEnvironmentThreadLocalHolder.pushExpressionEnvironment(
                    new ModelExpressionEnvironment<>(actx.context, null, actx.task, result));
            try {
                PrismObjectDefinition<TaskType> taskDefinition = preparedTask.asPrismObject().getDefinition();

                VariablesMap variables = actx.createVariables();
                variables.addVariableDefinition(VAR_PREPARED_TASK, preparedTask, taskDefinition);
                ExpressionProfile expressionProfile = MiscSchemaUtil.getExpressionProfile();
                PrismValue customizedTaskValue = ExpressionUtil.evaluateExpression(variables, taskDefinition,
                        customizer, expressionProfile, beans.expressionFactory, "task customizer",
                        actx.task, result);
                if (customizedTaskValue == null) {
                    throw new IllegalStateException("Task customizer returned no value");
                }
                if (!(customizedTaskValue instanceof PrismObjectValue)) {
                    throw new IllegalStateException("Task customizer returned a value that is not a PrismObjectValue: " + customizedTaskValue);
                }
                Objectable customizedTaskBean = ((PrismObjectValue<?>) customizedTaskValue).asObjectable();
                if (!(customizedTaskBean instanceof TaskType)) {
                    throw new IllegalStateException("Task customizer returned a value that is not a TaskType: " + customizedTaskBean);
                }
                return (TaskType) customizedTaskBean;
            } finally {
                ExpressionEnvironmentThreadLocalHolder.popExpressionEnvironment();
            }
        }
    }
}
