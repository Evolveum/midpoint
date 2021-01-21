/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.scripting;

import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.util.exception.ScriptExecutionException;
import com.evolveum.midpoint.model.api.ScriptExecutionResult;
import com.evolveum.midpoint.model.api.ScriptingService;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptType;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author mederly
 */
@Component
public class ScriptExecutionTaskHandler implements TaskHandler {

    private static final Trace LOGGER = TraceManager.getTrace(ScriptExecutionTaskHandler.class);

    private static final String DOT_CLASS = ScriptExecutionTaskHandler.class.getName() + ".";

    @Autowired private TaskManager taskManager;
    @Autowired private ScriptingService scriptingService;

    @NotNull
    @Override
    public StatisticsCollectionStrategy getStatisticsCollectionStrategy() {
        return new StatisticsCollectionStrategy()
                .fromZero()
                .maintainIterationStatistics()
                .maintainActionsExecutedStatistics();
    }

    @Override
    public TaskRunResult run(RunningTask task, TaskPartitionDefinitionType partition) {
        OperationResult result = task.getResult().createSubresult(DOT_CLASS + "run");
        TaskRunResult runResult = new TaskRunResult();

        ExecuteScriptType executeScriptRequest = IterativeScriptExecutionTaskHandler.getExecuteScriptRequest(task);
        try {
            ScriptExecutionResult executionResult = scriptingService.evaluateExpression(executeScriptRequest,
                    VariablesMap.emptyMap(), true, task, result);
            LOGGER.debug("Execution output: {} item(s)", executionResult.getDataOutput().size());
            LOGGER.debug("Execution result:\n{}", executionResult.getConsoleOutput());
            result.computeStatus();
            runResult.setRunResultStatus(TaskRunResult.TaskRunResultStatus.FINISHED);
        } catch (ScriptExecutionException | SecurityViolationException | SchemaException | ObjectNotFoundException | ExpressionEvaluationException | CommunicationException | ConfigurationException e) {
            result.recordFatalError("Couldn't execute script: " + e.getMessage(), e);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't execute script", e);
            runResult.setRunResultStatus(TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR);
        }

        task.getResult().computeStatus();
        runResult.setOperationResult(task.getResult());
        return runResult;
    }

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.BULK_ACTIONS;
    }

    @PostConstruct
    private void initialize() {
        taskManager.registerHandler(ModelPublicConstants.SCRIPT_EXECUTION_TASK_HANDLER_URI, this);
    }

    @Override
    public String getArchetypeOid() {
        return SystemObjectsType.ARCHETYPE_SINGLE_BULK_ACTION_TASK.value();
    }

    @Override
    public String getDefaultChannel() {
        return null; // The channel URI should be provided by the task creator.
    }
}
