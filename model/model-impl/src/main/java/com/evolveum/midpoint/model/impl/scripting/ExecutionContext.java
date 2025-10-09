/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.scripting;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.evolveum.midpoint.model.api.BulkActionExecutionOptions;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.PipelineItem;
import com.evolveum.midpoint.model.api.BulkActionExecutionResult;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.QueryConverter;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ScriptingExpressionEvaluationOptionsType;

/**
 * Context of a command execution.
 */
public class ExecutionContext {
    private static final Trace LOGGER = TraceManager.getTrace(ExecutionContext.class);

    private final ScriptingExpressionEvaluationOptionsType options;
    private final Task task;
    private final BulkActionsExecutor bulkActionsExecutor;
    private final StringBuilder consoleOutput = new StringBuilder();
    /** will probably remain unused */
    private final Map<String, PipelineData> globalVariables = new HashMap<>();
    /** used e.g. when there are no data in a pipeline; these are frozen - i.e. made immutable if possible; to be cloned-on-use */
    private final VariablesMap initialVariables;
    /** used only when passing result to external clients (TODO do this more cleanly) */
    private PipelineData finalOutput;
    @NotNull private final BulkActionExecutionOptions executionOptions;

    /**
     * Used for all evaluations in this context. The whole bulk action shares the same origin (it is a property, not a container),
     * so everything has the same profile.
     */
    @NotNull private final ExpressionProfile expressionProfile;

    public ExecutionContext(
            ScriptingExpressionEvaluationOptionsType options,
            Task task,
            BulkActionsExecutor bulkActionsExecutor,
            @NotNull BulkActionExecutionOptions executionOptions,
            VariablesMap initialVariables,
            @NotNull ExpressionProfile expressionProfile) {
        this.options = options;
        this.task = task;
        this.bulkActionsExecutor = bulkActionsExecutor;
        this.initialVariables = initialVariables;
        this.executionOptions = executionOptions;
        this.expressionProfile = expressionProfile;
    }

    public Task getTask() {
        return task;
    }

    public ScriptingExpressionEvaluationOptionsType getOptions() {
        return options;
    }

    public boolean isContinueOnAnyError() {
        return options != null && Boolean.TRUE.equals(options.isContinueOnAnyError());
    }

    public boolean isHideOperationResults() {
        return options != null && Boolean.TRUE.equals(options.isHideOperationResults());
    }

    public PipelineData getGlobalVariable(String name) {
        return globalVariables.get(name);
    }

    public void setGlobalVariable(String name, PipelineData value) {
        globalVariables.put(name, value);
    }

    public VariablesMap getInitialVariables() {
        return initialVariables;
    }

    public String getConsoleOutput() {
        return consoleOutput.toString();
    }

    public void println(Object o) {
        consoleOutput.append(o).append("\n");
        if (o != null) {
            // temporary, until some better way of logging bulk action executions is found
            LOGGER.debug("Script console message: {}", o);
        }
    }

    public PipelineData getFinalOutput() {
        return finalOutput;
    }

    public void setFinalOutput(PipelineData finalOutput) {
        this.finalOutput = finalOutput;
    }

    public boolean isRecordProgressAndIterationStatistics() {
        return executionOptions.recordProgressAndIterationStatistics();
    }

    public BulkActionExecutionResult toExecutionResult() {
        List<PipelineItem> items = null;
        if (getFinalOutput() != null) {
            items = getFinalOutput().getData();
        }
        return new BulkActionExecutionResult(getConsoleOutput(), items);
    }

    public String getChannel() {
        return task != null ? task.getChannel() : null;
    }

    public boolean canRun() {
        return !(task instanceof RunningTask) || ((RunningTask) task).canRun();
    }

    public void checkTaskStop() {
        if (!canRun()) {
            // TODO do this is a nicer way
            throw new SystemException("Stopping execution of a script because the task is stopping: " + task);
        }
    }

    void computeResults() {
        if (finalOutput != null) {
            finalOutput.getData().forEach(i -> i.computeResult());
        }
    }

    public ModelService getModelService() {
        return bulkActionsExecutor.getModelService();
    }

    public PrismContext getPrismContext() {
        return bulkActionsExecutor.getPrismContext();
    }

    public QueryConverter getQueryConverter() {
        return getPrismContext().getQueryConverter();
    }

    public @NotNull ExpressionProfile getExpressionProfile() {
        return expressionProfile;
    }

    public AuthorizationPhaseType getExecutionPhase() {
        if (executionOptions.executionPhase()) {
            return AuthorizationPhaseType.EXECUTION;
        } else {
            return null;
        }
    }
}
