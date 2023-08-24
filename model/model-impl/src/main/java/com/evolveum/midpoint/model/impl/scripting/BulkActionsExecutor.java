/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.scripting;

import static com.evolveum.midpoint.schema.util.ScriptingBeansUtil.getActionType;

import java.util.List;

import com.evolveum.midpoint.schema.expression.ExpressionProfile;

import jakarta.xml.bind.JAXBElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.common.expression.ExpressionProfileManager;
import com.evolveum.midpoint.model.impl.scripting.expressions.FilterContentEvaluator;
import com.evolveum.midpoint.model.impl.scripting.expressions.SearchEvaluator;
import com.evolveum.midpoint.model.impl.scripting.expressions.SelectEvaluator;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.schema.config.ExecuteScriptConfigItem;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.*;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

/**
 * Main entry point for evaluating scripting expressions.
 */
@Component
public class BulkActionsExecutor {

    private static final Trace LOGGER = TraceManager.getTrace(BulkActionsExecutor.class);
    private static final String DOT_CLASS = BulkActionsExecutor.class + ".";

    @Autowired private SearchEvaluator searchEvaluator;
    @Autowired private SelectEvaluator selectEvaluator;
    @Autowired private FilterContentEvaluator filterContentEvaluator;
    @Autowired private ModelService modelService;
    @Autowired private PrismContext prismContext;
    @Autowired private ScriptingActionExecutorRegistry actionExecutorRegistry;
    @Autowired private ExpressionProfileManager expressionProfileManager;

    /**
     * Executes given bulk action. This is the main entry point.
     */
    public ExecutionContext execute(
            @NotNull ExecuteScriptConfigItem executeScript,
            @NotNull VariablesMap initialVariables,
            boolean recordProgressAndIterationStatistics,
            @NotNull Task task,
            @NotNull OperationResult result) throws ScriptExecutionException {
        return execute(
                executeScript, initialVariables, false, recordProgressAndIterationStatistics, task, result);
    }

    /**
     * Entry point for privileged execution.
     *
     * The difference from the regular execution is in the default expression profile used.
     * Here, it is {@link ExpressionProfile#full()} even for unprivileged users.
     */
    public ExecutionContext executePrivileged(
            @NotNull ExecuteScriptConfigItem executeScript,
            @NotNull VariablesMap initialVariables,
            Task task,
            OperationResult result) throws ScriptExecutionException {
        return execute(
                executeScript, initialVariables, true, false, task, result);
    }

    private ExecutionContext execute(
            @NotNull ExecuteScriptConfigItem executeScript,
            @NotNull VariablesMap initialVariables,
            boolean privileged,
            boolean recordProgressAndIterationStatistics,
            @NotNull Task task,
            @NotNull OperationResult result) throws ScriptExecutionException {
        var executeScriptBean = executeScript.value();
        try {
            var expressionProfile =
                    expressionProfileManager.determineBulkActionsProfile(executeScript.origin(), privileged, task, result);
            VariablesMap frozenVariables = VariablesUtil.initialPreparation(
                    initialVariables, executeScriptBean.getVariables(),
                    expressionProfile, task, result);
            PipelineData inputPipeline = PipelineData.parseFrom(executeScriptBean.getInput(), frozenVariables);
            ExecutionContext context = new ExecutionContext(
                    executeScriptBean.getOptions(), task, this,
                    recordProgressAndIterationStatistics, frozenVariables, expressionProfile);
            PipelineData output = execute(
                    executeScriptBean.getScriptingExpression().getValue(), inputPipeline, context, result);
            context.setFinalOutput(output);
            result.computeStatusIfUnknown();
            context.computeResults();
            return context;
        } catch (CommonException | RuntimeException e) {
            result.recordException("Couldn't execute script: " + e.getMessage(), e);
            throw new ScriptExecutionException("Couldn't execute script: " + e.getMessage(), e);
        } catch (Throwable t) {
            result.recordException("Couldn't execute script: " + t.getMessage(), t);
            throw t;
        }
    }

    // not to be called from outside
    public PipelineData execute(JAXBElement<? extends ScriptingExpressionType> expression, PipelineData input,
            ExecutionContext context, OperationResult parentResult) throws ScriptExecutionException, SchemaException,
            ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        return execute(expression.getValue(), input, context, parentResult);
    }

    // not to be called from outside
    public PipelineData execute(
            ScriptingExpressionType value, PipelineData input, ExecutionContext context, OperationResult parentResult)
            throws ScriptExecutionException, SchemaException, ConfigurationException, ObjectNotFoundException,
            CommunicationException, SecurityViolationException, ExpressionEvaluationException {
        context.checkTaskStop();
        OperationResult globalResult = parentResult.createMinorSubresult(DOT_CLASS + "evaluate");
        try {
            PipelineData output;
            if (value instanceof ExpressionPipelineType pipeline) {
                output = executePipeline(pipeline, input, context, globalResult);
            } else if (value instanceof ExpressionSequenceType sequence) {
                output = executeSequence(sequence, input, context, globalResult);
            } else if (value instanceof SelectExpressionType selectExpression) {
                output = selectEvaluator.evaluate(selectExpression, input, context, globalResult);
            } else if (value instanceof FilterContentExpressionType filterContentExpression) {
                output = filterContentEvaluator.evaluate(filterContentExpression, input, context, globalResult);
            } else if (value instanceof SearchExpressionType searchExpression) {
                output = searchEvaluator.evaluate(searchExpression, input, context, globalResult);
            } else if (value instanceof ActionExpressionType actionExpression) {
                output = executeAction(actionExpression, input, context, globalResult);
            } else {
                throw new IllegalArgumentException(
                        "Unsupported expression type: " + (value == null ? "(null)" : value.getClass()));
            }
            globalResult.computeStatusIfUnknown();
            globalResult.setSummarizeSuccesses(true);
            globalResult.summarize();
            return output;
        } catch (Throwable t) {
            globalResult.recordException(t);
            throw t;
        } finally {
            globalResult.close();
        }
    }

    private PipelineData executeAction(
            @NotNull ActionExpressionType action, PipelineData input, ExecutionContext context, OperationResult globalResult)
            throws ScriptExecutionException, SchemaException, ObjectNotFoundException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Executing action {} on {}", getActionType(action), input.debugDump());
        } else if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Executing action {}", getActionType(action));
        }
        ActionExecutor executor = actionExecutorRegistry.getExecutor(action);
        executor.checkExecutionAllowed(context);
        return executor.execute(action, input, context, globalResult);
    }

    private PipelineData executePipeline(ExpressionPipelineType pipeline, PipelineData data, ExecutionContext context,
            OperationResult result) throws ScriptExecutionException, SchemaException, ConfigurationException,
            ObjectNotFoundException, CommunicationException, SecurityViolationException, ExpressionEvaluationException {
        for (JAXBElement<? extends ScriptingExpressionType> expressionType : pipeline.getScriptingExpression()) {
            data = execute(expressionType, data, context, result);
        }
        return data;
    }

    private PipelineData executeSequence(ExpressionSequenceType sequence, PipelineData input, ExecutionContext context,
            OperationResult result) throws ScriptExecutionException, SchemaException, ConfigurationException,
            ObjectNotFoundException, CommunicationException, SecurityViolationException, ExpressionEvaluationException {
        PipelineData lastOutput = null;
        List<JAXBElement<? extends ScriptingExpressionType>> scriptingExpression = sequence.getScriptingExpression();
        for (int i = 0; i < scriptingExpression.size(); i++) {
            JAXBElement<? extends ScriptingExpressionType> expressionType = scriptingExpression.get(i);
            PipelineData branchInput = i < scriptingExpression.size() - 1 ? input.cloneMutableState() : input;
            lastOutput = execute(expressionType, branchInput, context, result);
        }
        return lastOutput;
    }

    public PipelineData evaluateConstantExpression(@NotNull RawType constant, @Nullable Class<?> expectedClass, ExecutionContext context, String desc) throws ScriptExecutionException {

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
            return PipelineData.create(value, context.getInitialVariables());
        } catch (SchemaException e) {
            throw new ScriptExecutionException(e.getMessage(), e);
        }
    }

    public PipelineData evaluateConstantStringExpression(RawType constant, ExecutionContext context) throws ScriptExecutionException {
        try {
            String value = constant.getParsedRealValue(String.class);
            return PipelineData.create(prismContext.itemFactory().createPropertyValue(value), context.getInitialVariables());
        } catch (SchemaException e) {
            throw new ScriptExecutionException(e.getMessage(), e);
        }
    }

    ModelService getModelService() {
        return modelService;
    }

    PrismContext getPrismContext() {
        return prismContext;
    }
}
