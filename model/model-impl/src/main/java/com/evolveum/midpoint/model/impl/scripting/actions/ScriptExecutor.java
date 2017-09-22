/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.model.impl.scripting.actions;

import com.evolveum.midpoint.repo.common.expression.ExpressionSyntaxException;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.model.api.ScriptExecutionException;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpression;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionFactory;
import com.evolveum.midpoint.model.impl.scripting.PipelineData;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.api.PipelineItem;
import com.evolveum.midpoint.model.impl.util.Utils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.List;

/**
 * @author mederly
 */
@Component
public class ScriptExecutor extends BaseActionExecutor {

    //private static final Trace LOGGER = TraceManager.getTrace(ScriptExecutor.class);

	@Autowired
	private ScriptExpressionFactory scriptExpressionFactory;

    private static final String NAME = "execute-script";
    private static final String PARAM_SCRIPT = "script";
    private static final String PARAM_OUTPUT_ITEM = "outputItem";		// item name or type (as URI!) -- EXPERIMENTAL
	private static final String PARAM_FOR_WHOLE_INPUT = "forWholeInput";

    @PostConstruct
    public void init() {
        scriptingExpressionEvaluator.registerActionExecutor(NAME, this);
    }

    @Override
    public PipelineData execute(ActionExpressionType expression, PipelineData input, ExecutionContext context, OperationResult globalResult) throws ScriptExecutionException {

		checkRootAuthorization(context, globalResult, NAME);

		ScriptExpressionEvaluatorType script = expressionHelper.getSingleArgumentValue(expression.getParameter(), PARAM_SCRIPT, true, true,
				NAME, input, context, ScriptExpressionEvaluatorType.class, globalResult);
		String outputItem = expressionHelper.getSingleArgumentValue(expression.getParameter(), PARAM_OUTPUT_ITEM, false, false,
				NAME, input, context, String.class, globalResult);
	    boolean forWholeInput = expressionHelper.getArgumentAsBoolean(expression.getParameter(), PARAM_FOR_WHOLE_INPUT, input, context, false, PARAM_FOR_WHOLE_INPUT, globalResult);

		ItemDefinition<?> outputDefinition = getItemDefinition(outputItem);

		ScriptExpression scriptExpression;
		try {
			scriptExpression = scriptExpressionFactory.createScriptExpression(script, outputDefinition, "script");
		} catch (ExpressionSyntaxException e) {
			throw new ScriptExecutionException("Couldn't parse script expression: " + e.getMessage(), e);
		}

		PipelineData output = PipelineData.createEmpty();

		if (forWholeInput) {
			OperationResult result = operationsHelper.createActionResult(null, this, context, globalResult);
			context.checkTaskStop();
			Throwable exception = null;
			try {
				Object outObject = executeScript(scriptExpression, input, context, result);
				if (outObject != null) {
					addToData(outObject, PipelineData.newOperationResult(), output);
				} else {
					// no definition means we don't plan to provide any output - so let's just copy the input item to the output
					// (actually, null definition with non-null outObject should not occur)
					if (outputDefinition == null) {
						output.addAllFrom(input);
					}
				}
			} catch (Throwable ex) {
				exception = processActionException(ex, NAME, null, context);        // TODO value for error reporting (3rd parameter)
			}
			context.println((exception != null ? "Attempted to execute " : "Executed ")
					+ "script on the pipeline" + exceptionSuffix(exception));
			operationsHelper.trimAndCloneResult(result, globalResult, context);
		} else {
			for (PipelineItem item : input.getData()) {
				PrismValue value = item.getValue();
				OperationResult result = operationsHelper.createActionResult(item, this, context, globalResult);

				context.checkTaskStop();
				String valueDescription;
				long started;
				if (value instanceof PrismObjectValue) {
					started = operationsHelper.recordStart(context, asObjectType(value));
					valueDescription = asObjectType(value).asPrismObject().toString();
				} else {
					started = 0;
					valueDescription = value.toHumanReadableString();
				}
				Throwable exception = null;
				try {
					Object outObject = executeScript(scriptExpression, value, context, result);
					if (outObject != null) {
						addToData(outObject, item.getResult(), output);
					} else {
						// no definition means we don't plan to provide any output - so let's just copy the input item to the output
						// (actually, null definition with non-null outObject should not occur)
						if (outputDefinition == null) {
							output.add(item);
						}
					}
					if (value instanceof PrismObjectValue) {
						operationsHelper.recordEnd(context, asObjectType(value), started, null);
					}
				} catch (Throwable ex) {
					if (value instanceof PrismObjectValue) {
						operationsHelper.recordEnd(context, asObjectType(value), started, ex);
					}
					exception = processActionException(ex, NAME, value, context);
				}
				context.println((exception != null ? "Attempted to execute " : "Executed ")
						+ "script on " + valueDescription + exceptionSuffix(exception));
				operationsHelper.trimAndCloneResult(result, globalResult, context);
			}
		}
        return output;
    }

	private void addToData(@NotNull Object outObject, @NotNull OperationResult result, PipelineData output) throws SchemaException {
		if (outObject instanceof Collection) {
			for (Object o : (Collection) outObject) {
				addToData(o, result, output);
			}
		} else {
			PrismValue value;
			if (outObject instanceof PrismValue) {
				value = (PrismValue) outObject;
			} else if (outObject instanceof Objectable) {
				value = new PrismObjectValue<>((Objectable) outObject, prismContext);
			} else if (outObject instanceof Containerable) {
				value = new PrismContainerValue<>((Containerable) outObject, prismContext);
			} else {
				value = new PrismPropertyValue<>(outObject, prismContext);
			}
			output.add(new PipelineItem(value, result));
		}
	}

	private ItemDefinition<?> getItemDefinition(String itemUri) throws ScriptExecutionException {
		if (StringUtils.isBlank(itemUri)) {
			return null;
		}

		QName itemName = QNameUtil.uriToQName(itemUri, true);
		ItemDefinition def = prismContext.getSchemaRegistry().findItemDefinitionByElementName(itemName);
		if (def != null) {
			return def;
		}
		def = prismContext.getSchemaRegistry().findItemDefinitionByType(itemName);
		if (def != null) {
			return def;
		}
		throw new ScriptExecutionException("Supplied item identification " + itemUri + " corresponds neither to item name nor type name");
	}

	private Object executeScript(ScriptExpression scriptExpression, Object input, ExecutionContext context, OperationResult result)
			throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		ExpressionVariables variables = new ExpressionVariables();
		variables.addVariableDefinition(ExpressionConstants.VAR_INPUT, input);
		variables.addVariableDefinition(ExpressionConstants.VAR_PRISM_CONTEXT, prismContext);
		ExpressionUtil.addActorVariable(variables, securityEnforcer);

		List<?> rv = Utils.evaluateScript(scriptExpression, null, variables, true, "in '"+NAME+"' action", context.getTask(), result);

		if (rv == null || rv.size() == 0) {
			return null;
		} else if (rv.size() == 1) {
			return rv.get(0);
		} else {
			return rv;		// shouldn't occur; would cause problems
		}
	}

	private ObjectType asObjectType(PrismValue value) {
		return (ObjectType) ((PrismObjectValue) value).asObjectable();
	}

}
