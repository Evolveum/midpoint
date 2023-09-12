/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.scripting;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.PipelineItem;
import com.evolveum.midpoint.model.api.ScriptExecutionResult;
import com.evolveum.midpoint.model.common.expression.evaluator.ConstExpressionEvaluatorFactory;
import com.evolveum.midpoint.model.common.expression.evaluator.path.PathExpressionEvaluatorFactory;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.QueryConverter;
import com.evolveum.midpoint.repo.common.expression.evaluator.AsIsExpressionEvaluatorFactory;
import com.evolveum.midpoint.repo.common.expression.evaluator.LiteralExpressionEvaluatorFactory;
import com.evolveum.midpoint.schema.AccessDecision;
import com.evolveum.midpoint.schema.expression.ExpressionEvaluatorProfile;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ScriptingExpressionEvaluationOptionsType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Context of a command execution.
 *
 * @author mederly
 */
public class ExecutionContext {
    private static final Trace LOGGER = TraceManager.getTrace(ExecutionContext.class);

    private final boolean privileged;
    private final ScriptingExpressionEvaluationOptionsType options;
    private final Task task;
    private final ScriptingExpressionEvaluator scriptingExpressionEvaluator;
    private final StringBuilder consoleOutput = new StringBuilder();
    private final Map<String, PipelineData> globalVariables = new HashMap<>();      // will probably remain unused
    private final VariablesMap initialVariables;                             // used e.g. when there are no data in a pipeline; these are frozen - i.e. made immutable if possible; to be cloned-on-use
    private PipelineData finalOutput;                                        // used only when passing result to external clients (TODO do this more cleanly)
    private final boolean recordProgressAndIterationStatistics;
    /** Are we running under root principal? This is used to derive expression profiles in some cases. */
    private final boolean root;

    public ExecutionContext(ScriptingExpressionEvaluationOptionsType options, Task task,
            ScriptingExpressionEvaluator scriptingExpressionEvaluator,
            boolean privileged, boolean recordProgressAndIterationStatistics, VariablesMap initialVariables,
            boolean root) {
        this.options = options;
        this.task = task;
        this.scriptingExpressionEvaluator = scriptingExpressionEvaluator;
        this.privileged = privileged;
        this.initialVariables = initialVariables;
        this.recordProgressAndIterationStatistics = recordProgressAndIterationStatistics;
        this.root = root;
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
            LOGGER.debug("Script console message: {}", o);          // temporary, until some better way of logging bulk action executions is found
        }
    }

    public PipelineData getFinalOutput() {
        return finalOutput;
    }

    public void setFinalOutput(PipelineData finalOutput) {
        this.finalOutput = finalOutput;
    }

    public boolean isRecordProgressAndIterationStatistics() {
        return recordProgressAndIterationStatistics;
    }

    public ScriptExecutionResult toExecutionResult() {
        List<PipelineItem> items = null;
        if (getFinalOutput() != null) {
            items = getFinalOutput().getData();
        }
        return new ScriptExecutionResult(getConsoleOutput(), items);
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

    public void computeResults() {
        if (finalOutput != null) {
            finalOutput.getData().forEach(i -> i.computeResult());
        }
    }

    public ModelService getModelService() {
        return scriptingExpressionEvaluator.getModelService();
    }

    public PrismContext getPrismContext() {
        return scriptingExpressionEvaluator.getPrismContext();
    }

    public boolean isPrivileged() {
        return privileged;
    }

    public QueryConverter getQueryConverter() {
        return getPrismContext().getQueryConverter();
    }

    @Nullable
    public ExpressionProfile determineExpressionProfile()
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        if (root) {
            return MiscSchemaUtil.getExpressionProfile(); // currently null, i.e. allowing everything
        } else {
            // Profile for unprivileged users. Only "safe" evaluators are allowed.
            ExpressionProfile profile = new ExpressionProfile("##builtin-safe##");
            profile.setDecision(AccessDecision.DENY);
            profile.add(evaluatorAllowed(AsIsExpressionEvaluatorFactory.ELEMENT_NAME));
            profile.add(evaluatorAllowed(PathExpressionEvaluatorFactory.ELEMENT_NAME));
            profile.add(evaluatorAllowed(LiteralExpressionEvaluatorFactory.ELEMENT_NAME));
            profile.add(evaluatorAllowed(ConstExpressionEvaluatorFactory.ELEMENT_NAME));
            return profile;
        }
    }

    @NotNull
    private static ExpressionEvaluatorProfile evaluatorAllowed(QName elementName) {
        ExpressionEvaluatorProfile profile = new ExpressionEvaluatorProfile(elementName);
        profile.setDecision(AccessDecision.ALLOW);
        return profile;
    }
}
