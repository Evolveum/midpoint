/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.scripting;

import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.ScriptExecutionException;
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.model.impl.scripting.expressions.FilterContentEvaluator;
import com.evolveum.midpoint.model.impl.scripting.expressions.SearchEvaluator;
import com.evolveum.midpoint.model.impl.scripting.expressions.SelectEvaluator;
import com.evolveum.midpoint.model.impl.scripting.helpers.ScriptingJaxbUtil;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.*;
import com.evolveum.prism.xml.ns._public.types_3.RawType;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main entry point for evaluating scripting expressions.
 *
 * @author mederly
 */
@Component
public class ScriptingExpressionEvaluator {

    private static final Trace LOGGER = TraceManager.getTrace(ScriptingExpressionEvaluator.class);
    private static final String DOT_CLASS = ScriptingExpressionEvaluator.class + ".";

    @Autowired private TaskManager taskManager;
    @Autowired private SearchEvaluator searchEvaluator;
    @Autowired private SelectEvaluator selectEvaluator;
    @Autowired private FilterContentEvaluator filterContentEvaluator;
    @Autowired private ModelService modelService;
    @Autowired private PrismContext prismContext;
    @Autowired private ModelObjectResolver modelObjectResolver;
    @Autowired private ExpressionFactory expressionFactory;

    private ObjectFactory objectFactory = new ObjectFactory();

    private Map<String, ActionExecutor> actionExecutors = new HashMap<>();

    // TODO implement more nicely
    public static ExecuteScriptType createExecuteScriptCommand(ScriptingExpressionType expression) {
        ExecuteScriptType executeScriptCommand = new ExecuteScriptType();
        executeScriptCommand.setScriptingExpression(ScriptingJaxbUtil.toJaxbElement(expression));
        return executeScriptCommand;
    }

    /**
     * Asynchronously executes any scripting expression.
     *
     * @param expression Expression to be executed.
     * @param task Task in context of which the script should execute. The task should be "clean", i.e.
     *             (1) transient, (2) without any handler. This method puts the task into background,
     *             and assigns ScriptExecutionTaskHandler to it, to execute the script.
     */
    public void evaluateExpressionInBackground(ScriptingExpressionType expression, Task task, OperationResult parentResult) throws SchemaException {
        evaluateExpressionInBackground(createExecuteScriptCommand(expression), task, parentResult);
    }

    public void evaluateExpressionInBackground(ExecuteScriptType executeScriptCommand, Task task, OperationResult parentResult) throws SchemaException {
        if (!task.isTransient()) {
            throw new IllegalStateException("Task must be transient");
        }
        if (task.getHandlerUri() != null) {
            throw new IllegalStateException("Task must not have a handler");
        }
        OperationResult result = parentResult.createSubresult(DOT_CLASS + "evaluateExpressionInBackground");
        task.setExtensionPropertyValue(SchemaConstants.SE_EXECUTE_SCRIPT, executeScriptCommand);
        task.setHandlerUri(ModelPublicConstants.SCRIPT_EXECUTION_TASK_HANDLER_URI);
        taskManager.switchToBackground(task, result);
        result.computeStatus();
    }

    /**
     * Asynchronously executes any scripting expression.
     *
     * @param executeScriptCommand ExecuteScript to be executed.
     * @param task Task in context of which the script should execute. The task should be "clean", i.e.
     *             (1) transient, (2) without any handler. This method puts the task into background,
     *             and assigns IterativeScriptExecutionTaskHandler to it, to execute the script.
     */
    public void evaluateIterativeExpressionInBackground(ExecuteScriptType executeScriptCommand, Task task, OperationResult parentResult) throws SchemaException {
        if (!task.isTransient()) {
            throw new IllegalStateException("Task must be transient");
        }
        if (task.getHandlerUri() != null) {
            throw new IllegalStateException("Task must not have a handler");
        }
        OperationResult result = parentResult.createSubresult(DOT_CLASS + "evaluateExpressionInBackground");
        task.setExtensionPropertyValue(SchemaConstants.SE_EXECUTE_SCRIPT, executeScriptCommand);
        task.setHandlerUri(ModelPublicConstants.ITERATIVE_SCRIPT_EXECUTION_TASK_HANDLER_URI);
        taskManager.switchToBackground(task, result);
        result.computeStatus();
    }

    /**
     * Main entry point.
     */
    public ExecutionContext evaluateExpression(@NotNull ExecuteScriptType executeScript, VariablesMap initialVariables,
            boolean recordProgressAndIterationStatistics, Task task, OperationResult result) throws ScriptExecutionException {
        return evaluateExpression(executeScript, initialVariables, false, recordProgressAndIterationStatistics, task, result);
    }

    /**
     * Entry point for privileged execution.
     * Note that privileged execution means
     */
    public ExecutionContext evaluateExpressionPrivileged(@NotNull ExecuteScriptType executeScript, @NotNull VariablesMap initialVariables, Task task, OperationResult result) throws ScriptExecutionException {
        return evaluateExpression(executeScript, initialVariables, true, false, task, result);
    }

    /**
     * Convenience method (if we don't have full ExecuteScriptType).
     */
    public ExecutionContext evaluateExpression(ScriptingExpressionType expression, Task task, OperationResult result) throws ScriptExecutionException {
        return evaluateExpression(createExecuteScriptCommand(expression), VariablesMap.emptyMap(), false, task, result);
    }

    private ExecutionContext evaluateExpression(@NotNull ExecuteScriptType executeScript, VariablesMap initialVariables,
            boolean privileged, boolean recordProgressAndIterationStatistics, Task task, OperationResult result) throws ScriptExecutionException {
        Validate.notNull(executeScript.getScriptingExpression(), "Scripting expression must be present");
        ExpressionProfile expressionProfile = MiscSchemaUtil.getExpressionProfile();
        try {
            VariablesMap frozenVariables = VariablesUtil.initialPreparation(initialVariables, executeScript.getVariables(), expressionFactory, modelObjectResolver, prismContext, expressionProfile, task, result);
            PipelineData pipelineData = PipelineData.parseFrom(executeScript.getInput(), frozenVariables, prismContext);
            ExecutionContext context = new ExecutionContext(executeScript.getOptions(), task, this,
                    privileged, recordProgressAndIterationStatistics, frozenVariables);
            PipelineData output = evaluateExpression(executeScript.getScriptingExpression().getValue(), pipelineData, context, result);
            context.setFinalOutput(output);
            result.computeStatusIfUnknown();
            context.computeResults();
            return context;
        } catch (ExpressionEvaluationException | SchemaException | ObjectNotFoundException | RuntimeException | CommunicationException | ConfigurationException | SecurityViolationException e) {
            result.recordFatalError("Couldn't execute script", e);
            throw new ScriptExecutionException("Couldn't execute script: " + e.getMessage(), e);
        }
    }

    // not to be called from outside
    public PipelineData evaluateExpression(JAXBElement<? extends ScriptingExpressionType> expression, PipelineData input, ExecutionContext context, OperationResult parentResult) throws ScriptExecutionException {
        return evaluateExpression(expression.getValue(), input, context, parentResult);
    }

    // not to be called from outside
    public PipelineData evaluateExpression(ScriptingExpressionType value, PipelineData input, ExecutionContext context, OperationResult parentResult) throws ScriptExecutionException {
        context.checkTaskStop();
        OperationResult globalResult = parentResult.createMinorSubresult(DOT_CLASS + "evaluateExpression");
        PipelineData output;
        if (value instanceof ExpressionPipelineType) {
            output = executePipeline((ExpressionPipelineType) value, input, context, globalResult);
        } else if (value instanceof ExpressionSequenceType) {
            output = executeSequence((ExpressionSequenceType) value, input, context, globalResult);
        } else if (value instanceof SelectExpressionType) {
            output = selectEvaluator.evaluate((SelectExpressionType) value, input, context, globalResult);
        } else if (value instanceof FilterContentExpressionType) {
            output = filterContentEvaluator.evaluate((FilterContentExpressionType) value, input, context, globalResult);
        } else if (value instanceof SearchExpressionType) {
            output = searchEvaluator.evaluate((SearchExpressionType) value, input, context, globalResult);
        } else if (value instanceof ActionExpressionType) {
            output = executeAction((ActionExpressionType) value, input, context, globalResult);
        } else {
            throw new IllegalArgumentException("Unsupported expression type: " + (value==null?"(null)":value.getClass()));
        }
        globalResult.computeStatusIfUnknown();
        return output;
    }

    private PipelineData executeAction(ActionExpressionType command, PipelineData input, ExecutionContext context, OperationResult globalResult) throws ScriptExecutionException {
        Validate.notNull(command, "command");
        Validate.notNull(command.getType(), "command.actionType");

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Executing action {} on {}", command.getType(), input.debugDump());
        } else if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Executing action {}", command.getType());
        }
        ActionExecutor executor = actionExecutors.get(command.getType());
        if (executor == null) {
            throw new IllegalStateException("Unsupported action type: " + command.getType());
        } else {
            PipelineData retval = executor.execute(command, input, context, globalResult);
            globalResult.setSummarizeSuccesses(true);
            globalResult.summarize();
            return retval;
        }
    }

    private PipelineData executePipeline(ExpressionPipelineType pipeline, PipelineData data, ExecutionContext context, OperationResult result) throws ScriptExecutionException {
        for (JAXBElement<? extends ScriptingExpressionType> expressionType : pipeline.getScriptingExpression()) {
            data = evaluateExpression(expressionType, data, context, result);
        }
        return data;
    }

    private PipelineData executeSequence(ExpressionSequenceType sequence, PipelineData input, ExecutionContext context, OperationResult result) throws ScriptExecutionException {
        PipelineData lastOutput = null;
        List<JAXBElement<? extends ScriptingExpressionType>> scriptingExpression = sequence.getScriptingExpression();
        for (int i = 0; i < scriptingExpression.size(); i++) {
            JAXBElement<? extends ScriptingExpressionType> expressionType = scriptingExpression.get(i);
            PipelineData branchInput = i < scriptingExpression.size() - 1 ? input.cloneMutableState() : input;
            lastOutput = evaluateExpression(expressionType, branchInput, context, result);
        }
        return lastOutput;
    }

    public PipelineData evaluateConstantExpression(@NotNull RawType constant, @Nullable Class<?> expectedClass, ExecutionContext context, String desc, OperationResult result) throws ScriptExecutionException {

        try {
            // TODO fix this brutal hacking
            PrismValue value;
            if (expectedClass == null) {
                value = constant.getParsedValue(null, null);
            } else {
                Object object = constant.getParsedRealValue(expectedClass);
                if (object instanceof Referencable) {
                    value = ((Referencable) object).asReferenceValue();
                } else if (object instanceof Containerable) {
                    value = ((Containerable) object).asPrismContainerValue();
                } else {
                    value = prismContext.itemFactory().createPropertyValue(object);
                }
            }
            if (value.isRaw()) {
                throw new IllegalStateException("Raw value while " + desc + ": " + value + ". Please specify type of the value.");
            }
            return PipelineData.createItem(value, context.getInitialVariables());
        } catch (SchemaException e) {
            throw new ScriptExecutionException(e.getMessage(), e);
        }
    }

    public PipelineData evaluateConstantStringExpression(RawType constant, ExecutionContext context, OperationResult result) throws ScriptExecutionException {
        try {
            String value = constant.getParsedRealValue(String.class);
            return PipelineData.createItem(prismContext.itemFactory().createPropertyValue(value), context.getInitialVariables());
        } catch (SchemaException e) {
            throw new ScriptExecutionException(e.getMessage(), e);
        }
    }

    public void registerActionExecutor(String actionName, ActionExecutor executor) {
        actionExecutors.put(actionName, executor);
    }

    ModelService getModelService() {
        return modelService;
    }

    PrismContext getPrismContext() {
        return prismContext;
    }
}
