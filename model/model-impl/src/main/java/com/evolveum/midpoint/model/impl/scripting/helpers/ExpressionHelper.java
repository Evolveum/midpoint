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

package com.evolveum.midpoint.model.impl.scripting.helpers;

import com.evolveum.midpoint.model.api.ScriptExecutionException;
import com.evolveum.midpoint.model.impl.scripting.PipelineData;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.impl.scripting.ScriptingExpressionEvaluator;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.util.JavaTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionParameterValueType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author mederly
 */
@Component
public class ExpressionHelper {

	@Autowired
	private ScriptingExpressionEvaluator scriptingExpressionEvaluator;

	//    public JAXBElement<?> getArgument(ActionExpressionType actionExpression, String parameterName) throws ScriptExecutionException {
	//        return getArgument(actionExpression.getParameter(), parameterName, false, false, actionExpression.getType());
	//    }

	public ActionParameterValueType getArgument(List<ActionParameterValueType> arguments, String parameterName, boolean required,
			boolean requiredNonNull, String context) throws ScriptExecutionException {
		for (ActionParameterValueType parameterValue : arguments) {
			if (parameterName.equals(parameterValue.getName())) {
				if (parameterValue.getScriptingExpression() != null || parameterValue.getValue() != null) {
					return parameterValue;
				} else {
					if (requiredNonNull) {
						throw new ScriptExecutionException("Required parameter " + parameterName + " is null in invocation of \"" + context + "\"");
					} else {
						return null;
					}
				}
			}
		}
		if (required) {
			throw new ScriptExecutionException("Required parameter " + parameterName + " not present in invocation of \"" + context + "\"");
		} else {
			return null;
		}
	}

	public String getArgumentAsString(List<ActionParameterValueType> arguments, String argumentName, PipelineData input, ExecutionContext context,
			String defaultValue, String contextName, OperationResult parentResult) throws ScriptExecutionException {
		ActionParameterValueType parameterValue = getArgument(arguments, argumentName, false, false, contextName);
		if (parameterValue != null) {
			if (parameterValue.getScriptingExpression() != null) {
				PipelineData data = scriptingExpressionEvaluator.evaluateExpression(parameterValue.getScriptingExpression(), input, context, parentResult);
				if (data != null) {
					return data.getDataAsSingleString();
				}
			} else if (parameterValue.getValue() != null) {
				PipelineData data = scriptingExpressionEvaluator.evaluateConstantStringExpression((RawType) parameterValue.getValue(), context, parentResult);
				if (data != null) {
					return data.getDataAsSingleString();
				}
			} else {
				throw new IllegalStateException("No expression nor value specified");
			}
		}
		return defaultValue;
	}

	public Boolean getArgumentAsBoolean(List<ActionParameterValueType> arguments, String argumentName, PipelineData input, ExecutionContext context,
			Boolean defaultValue, String contextName, OperationResult parentResult) throws ScriptExecutionException {
		String stringValue = getArgumentAsString(arguments, argumentName, input, context, null, contextName, parentResult);
		if (stringValue == null) {
			return defaultValue;
		} else if ("true".equals(stringValue)) {
			return Boolean.TRUE;
		} else if ("false".equals(stringValue)) {
			return Boolean.FALSE;
		} else {
			throw new ScriptExecutionException("Invalid value for a boolean parameter '" + argumentName + "': " + stringValue);
		}
	}

	public PipelineData evaluateParameter(ActionParameterValueType parameter, @Nullable Class<?> expectedClass, PipelineData input, ExecutionContext context, OperationResult result)
			throws ScriptExecutionException {
		Validate.notNull(parameter, "parameter");
		if (parameter.getScriptingExpression() != null) {
			return scriptingExpressionEvaluator.evaluateExpression(parameter.getScriptingExpression(), input, context, result);
		} else if (parameter.getValue() != null) {
			return scriptingExpressionEvaluator.evaluateConstantExpression((RawType) parameter.getValue(), expectedClass, context, "evaluating parameter " + parameter.getName(), result);
		} else {
			throw new IllegalStateException("No expression nor value specified");
		}
	}

	public <T> T getSingleArgumentValue(List<ActionParameterValueType> arguments, String parameterName, boolean required,
			boolean requiredNonNull, String context, PipelineData input, ExecutionContext executionContext, Class<T> clazz, OperationResult result) throws ScriptExecutionException {
		ActionParameterValueType paramValue = getArgument(arguments, parameterName, required, requiredNonNull, context);
		if (paramValue == null) {
			return null;
		}
		PipelineData paramData = evaluateParameter(paramValue, clazz, input, executionContext, result);
		if (paramData.getData().size() != 1) {
			throw new ScriptExecutionException("Exactly one item was expected in '" + parameterName + "' parameter. Got " + paramData.getData().size());
		}
		PrismValue prismValue = paramData.getData().get(0).getValue();
		if (!(prismValue instanceof PrismPropertyValue)) {
			throw new ScriptExecutionException("A prism property value was expected in '" + parameterName + "' parameter. Got " + prismValue.getClass().getName() + " instead.");
		}
		Object value = ((PrismPropertyValue) prismValue).getValue();
		try {
			return JavaTypeConverter.convert(clazz, value);
		} catch (Throwable t) {
			throw new ScriptExecutionException("Couldn't retrieve value of parameter '" + parameterName + "': " + t.getMessage(), t);
		}
	}
}
