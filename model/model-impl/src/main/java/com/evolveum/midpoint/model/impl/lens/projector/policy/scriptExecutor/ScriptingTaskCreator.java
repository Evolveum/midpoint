/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy.scriptExecutor;

import static com.evolveum.midpoint.model.api.util.ReferenceResolver.Source.MODEL;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createObjectRef;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStatusType.RUNNABLE;

import com.evolveum.midpoint.model.api.util.ReferenceResolver;

import com.evolveum.midpoint.model.common.expression.ExpressionEnvironment;
import com.evolveum.midpoint.model.common.expression.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
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

    ScriptingTaskCreator(@NotNull ActionContext actx) {
        this.actx = actx;
        this.beans = actx.beans;
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
            List<PrismObject<? extends ObjectType>> tasks = beans.referenceResolver.resolve(asynchronousExecution.getTaskTemplateRef(), null,
                    MODEL, createTaskFilterEvaluator(), actx.task, result);
            if (tasks.isEmpty()) {
                throw new ObjectNotFoundException("Task template was specified but was not found");
            } else if (tasks.size() > 1) {
                throw new IllegalStateException("Task template reference resolution lead to more than one task template: " + tasks);
            } else {
                newTask = (TaskType) tasks.get(0).asObjectable();
            }
        } else {
            newTask = new TaskType(beans.prismContext);
            newTask.setName(PolyStringType.fromOrig("Execute script"));
            newTask.setRecurrence(TaskRecurrenceType.SINGLE);
        }
        newTask.setName(PolyStringType.fromOrig(newTask.getName().getOrig() + " " + (int) (Math.random() * 10000)));
        newTask.setOid(null);
        newTask.setTaskIdentifier(null);
        newTask.setOwnerRef(createObjectRef(principal.getFocus(), beans.prismContext));
        newTask.setExecutionStatus(RUNNABLE);
        return newTask;
    }

    private ReferenceResolver.FilterEvaluator createTaskFilterEvaluator() {
        return (rawFilter, result1) -> {
            ModelExpressionThreadLocalHolder.pushExpressionEnvironment(
                    new ExpressionEnvironment<>(actx.context, null, actx.task, result1));
            try {
                ExpressionVariables variables = createVariables();
                ExpressionProfile expressionProfile = MiscSchemaUtil.getExpressionProfile();
                return ExpressionUtil.evaluateFilterExpressions(rawFilter, variables, expressionProfile,
                        beans.expressionFactory, beans.prismContext,
                        "evaluating task template filter expression ", actx.task, result1);
            } finally {
                ModelExpressionThreadLocalHolder.popExpressionEnvironment();
            }
        };
    }

    private ExpressionVariables createVariables() throws SchemaException {
        ExpressionVariables variables = ModelImplUtils.getDefaultExpressionVariables(actx.context, null);
        actx.putIntoVariables(variables);
        return variables;
    }

    /**
     * Inserts script into task.
     */
    void setScriptInTask(TaskType taskBean, ExecuteScriptType executeScript)
            throws SchemaException {
        //noinspection unchecked
        PrismPropertyDefinition<ExecuteScriptType> executeScriptDef = beans.prismContext.getSchemaRegistry()
                .findPropertyDefinitionByElementName(SchemaConstants.SE_EXECUTE_SCRIPT);
        PrismProperty<ExecuteScriptType> executeScriptProp = executeScriptDef.instantiate();
        executeScriptProp.setRealValue(executeScript.clone());
        taskBean.asPrismObject().addExtensionItem(executeScriptProp);
    }
}
