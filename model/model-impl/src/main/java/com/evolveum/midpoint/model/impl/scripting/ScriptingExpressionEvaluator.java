/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.model.impl.scripting;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.ScriptExecutionException;
import com.evolveum.midpoint.model.impl.scripting.expressions.FilterContentEvaluator;
import com.evolveum.midpoint.model.impl.scripting.expressions.SearchEvaluator;
import com.evolveum.midpoint.model.impl.scripting.expressions.SelectEvaluator;
import com.evolveum.midpoint.model.impl.scripting.helpers.ScriptingJaxbUtil;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.marshaller.QueryConvertor;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.SchemaException;
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

    private ObjectFactory objectFactory = new ObjectFactory();

    private Map<String, ActionExecutor> actionExecutors = new HashMap<>();

    /**
     * Asynchronously executes simple scripting expressions, consisting of one search command and one action.
     *
     * @param objectType Object type to search (e.g. c:UserType)
     * @param filter Filter to be applied (ObjectFilter)
     * @param actionName Action to be executed on objects found (e.g. "disable", "delete", "recompute", etc).
     * @param task Task in context of which the script should execute. The task should be "clean", i.e.
     *             (1) transient, (2) without any handler. This method puts the task into background,
     *             and assigns ScriptExecutionTaskHandler to it, to execute the script.
     */
    @Deprecated
    public void evaluateExpressionInBackground(QName objectType, ObjectFilter filter, String actionName, Task task, OperationResult parentResult) throws SchemaException {
        Validate.notNull(objectType);
        Validate.notNull(actionName);
        Validate.notNull(task);

        SearchExpressionType search = new SearchExpressionType();
        search.setType(objectType);
        if (filter != null) {
            search.setSearchFilter(QueryConvertor.createSearchFilterType(filter, prismContext));
        }
        ActionExpressionType action = new ActionExpressionType();
        action.setType(actionName);
        search.setScriptingExpression(objectFactory.createAction(action));
        evaluateExpressionInBackground(search, task, parentResult);
    }

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
        task.setHandlerUri(ScriptExecutionTaskHandler.HANDLER_URI);
        taskManager.switchToBackground(task, result);
        result.computeStatus();
    }

    /**
     * Entry point to _synchronous_ script execution, with no input data.
     *
     * @param expression Scripting expression to execute.
     * @param task Task in context of which the script should execute (in foreground!)
     * @param result Operation result
     * @return ExecutionContext, from which the caller can retrieve the output data via getFinalOutput() method,
     *         and the console output via getConsoleOutput() method.
     */

    public ExecutionContext evaluateExpression(ScriptingExpressionType expression, Task task, OperationResult result) throws ScriptExecutionException {
        ExecutionContext context = evaluateExpression(expression, PipelineData.createEmpty(), null, task, result);
        context.computeResults();
        return context;
    }


	public ExecutionContext evaluateExpression(@NotNull ExecuteScriptType executeScript, Task task, OperationResult result) throws ScriptExecutionException {
		Validate.notNull(executeScript.getScriptingExpression(), "Scripting expression must be present");
        ExecutionContext context = evaluateExpression(executeScript.getScriptingExpression().getValue(),
                PipelineData.parseFrom(executeScript.getInput(), prismContext), executeScript.getOptions(), task, result);
        context.computeResults();
        return context;
    }

    // main entry point from the outside
	private ExecutionContext evaluateExpression(ScriptingExpressionType expression, PipelineData data,
			ScriptingExpressionEvaluationOptionsType options, Task task, OperationResult result) throws ScriptExecutionException {
		ExecutionContext context = new ExecutionContext(options, task, this);
		PipelineData output;
		try {
			output = evaluateExpression(expression, data, context, result);
		} catch (RuntimeException e) {
			result.recordFatalError("Couldn't execute script", e);
			throw new ScriptExecutionException("Couldn't execute script: " + e.getMessage(), e);
		}
		result.computeStatusIfUnknown();
		context.setFinalOutput(output);
		return context;
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
        for (JAXBElement<? extends ScriptingExpressionType> expressionType : sequence.getScriptingExpression()) {
            lastOutput = evaluateExpression(expressionType, input, context, result);
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
                    value = new PrismPropertyValue<>(object);
                }
            }
            if (value.isRaw()) {
                throw new IllegalStateException("Raw value while " + desc + ": " + value + ". Please specify type of the value.");
            }
            return PipelineData.createItem(value);
        } catch (SchemaException e) {
            throw new ScriptExecutionException(e.getMessage(), e);
        }
    }

    public PipelineData evaluateConstantStringExpression(RawType constant, ExecutionContext context, OperationResult result) throws ScriptExecutionException {
        try {
            String value = constant.getParsedRealValue(String.class);
            return PipelineData.createItem(new PrismPropertyValue<>(value));
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
