/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.scripting;

import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.api.ScriptExecutionException;
import com.evolveum.midpoint.model.api.ScriptExecutionResult;
import com.evolveum.midpoint.model.api.ScriptingService;
import com.evolveum.midpoint.model.impl.util.AbstractSearchIterativeModelTaskHandler;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.repo.common.task.AbstractSearchIterativeResultHandler;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ValueListType;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class IterativeScriptExecutionTaskHandler extends AbstractSearchIterativeModelTaskHandler<ObjectType, AbstractSearchIterativeResultHandler<ObjectType>> {

    @Autowired private TaskManager taskManager;
    @Autowired private ScriptingService scriptingService;

    private static final Trace LOGGER = TraceManager.getTrace(IterativeScriptExecutionTaskHandler.class);

    public IterativeScriptExecutionTaskHandler() {
        super("Execute script", OperationConstants.EXECUTE_SCRIPT);
        setLogFinishInfo(true);     // todo
    }

    @PostConstruct
    private void initialize() {
        taskManager.registerHandler(ModelPublicConstants.ITERATIVE_SCRIPT_EXECUTION_TASK_HANDLER_URI, this);
    }

    protected Class<? extends ObjectType> getType(Task task) {
        return getTypeFromTask(task, ObjectType.class);
    }

    @NotNull
    @Override
    protected AbstractSearchIterativeResultHandler<ObjectType> createHandler(TaskPartitionDefinitionType partition, TaskRunResult runResult, final RunningTask coordinatorTask,
            OperationResult opResult) {

        PrismProperty<ExecuteScriptType> executeScriptProperty = coordinatorTask.getExtensionPropertyOrClone(SchemaConstants.SE_EXECUTE_SCRIPT);
        if (executeScriptProperty == null || executeScriptProperty.getValue().getValue() == null ||
                executeScriptProperty.getValue().getValue().getScriptingExpression() == null) {
            throw new IllegalStateException("There's no script to be run in task " + coordinatorTask + " (property " + SchemaConstants.SE_EXECUTE_SCRIPT + ")");
        }
        ExecuteScriptType executeScriptRequestTemplate = executeScriptProperty.getRealValue();
        if (executeScriptRequestTemplate.getInput() != null && !executeScriptRequestTemplate.getInput().getValue().isEmpty()) {
            LOGGER.warn("Ignoring input values in executeScript data in task {}", coordinatorTask);
        }

        AbstractSearchIterativeResultHandler<ObjectType> handler = new AbstractSearchIterativeResultHandler<ObjectType>(
                coordinatorTask, IterativeScriptExecutionTaskHandler.class.getName(), "execute", "execute task", taskManager) {
            @Override
            protected boolean handleObject(PrismObject<ObjectType> object, RunningTask workerTask, OperationResult result) {
                try {
                    ExecuteScriptType executeScriptRequest = executeScriptRequestTemplate.clone();
                    executeScriptRequest.setInput(new ValueListType().value(object.asObjectable()));
                    ScriptExecutionResult executionResult = scriptingService.evaluateExpression(executeScriptRequest, VariablesMap.emptyMap(),
                            false, workerTask, result);
                    LOGGER.debug("Execution output: {} item(s)", executionResult.getDataOutput().size());
                    LOGGER.debug("Execution result:\n{}", executionResult.getConsoleOutput());
                    result.computeStatus();
                } catch (ScriptExecutionException | SecurityViolationException | SchemaException | ObjectNotFoundException | ExpressionEvaluationException | CommunicationException | ConfigurationException e) {
                    result.recordFatalError("Couldn't execute script: " + e.getMessage(), e);
                    LoggingUtils.logUnexpectedException(LOGGER, "Couldn't execute script", e);
                }
                return true;
            }
        };
        handler.setStopOnError(false);
        return handler;
    }

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.BULK_ACTIONS;
    }
}
