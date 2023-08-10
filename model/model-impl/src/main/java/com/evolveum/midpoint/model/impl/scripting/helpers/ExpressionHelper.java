/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.scripting.helpers;

import com.evolveum.midpoint.model.api.PipelineItem;
import com.evolveum.midpoint.util.exception.ScriptExecutionException;
import com.evolveum.midpoint.model.impl.scripting.PipelineData;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.impl.scripting.ScriptingExpressionEvaluator;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.util.JavaTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ScriptingBeansUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionParameterValueType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component
public class ExpressionHelper {

    @Autowired private ScriptingExpressionEvaluator scriptingExpressionEvaluator;

    public ActionParameterValueType getArgument(
            List<ActionParameterValueType> arguments, String parameterName, boolean required,
            boolean requiredNonNull, String context) throws ScriptExecutionException {
        for (ActionParameterValueType parameterValue : arguments) {
            if (parameterName.equals(parameterValue.getName())) {
                if (parameterValue.getScriptingExpression() != null || parameterValue.getValue() != null) {
                    return parameterValue;
                } else {
                    if (requiredNonNull) {
                        throw new ScriptExecutionException(
                                "Required parameter " + parameterName + " is null in invocation of \"" + context + "\"");
                    } else {
                        return null;
                    }
                }
            }
        }
        if (required) {
            throw new ScriptExecutionException(
                    "Required parameter " + parameterName + " not present in invocation of \"" + context + "\"");
        } else {
            return null;
        }
    }

    private String getArgumentAsString(
            List<ActionParameterValueType> arguments,
            String argumentName,
            PipelineData input,
            ExecutionContext context,
            String defaultValue,
            String contextName,
            OperationResult parentResult)
            throws ScriptExecutionException, SchemaException, ConfigurationException, ObjectNotFoundException,
            CommunicationException, SecurityViolationException, ExpressionEvaluationException {
        ActionParameterValueType parameterValue =
                getArgument(arguments, argumentName, false, false, contextName);
        if (parameterValue != null) {
            if (parameterValue.getScriptingExpression() != null) {
                PipelineData data =
                        scriptingExpressionEvaluator.evaluateExpression(
                                parameterValue.getScriptingExpression(), input, context, parentResult);
                if (data != null) {
                    return data.getSingleValue(String.class);
                }
            } else if (parameterValue.getValue() != null) {
                PipelineData data =
                        scriptingExpressionEvaluator.evaluateConstantStringExpression(
                                (RawType) parameterValue.getValue(), context);
                if (data != null) {
                    return data.getSingleValue(String.class);
                }
            } else {
                throw new IllegalStateException("No expression nor value specified");
            }
        }
        return defaultValue;
    }

    public <T> T getActionArgument(
            Class<T> clazz,
            ActionExpressionType action,
            ItemName staticName,
            String dynamicName,
            PipelineData input,
            ExecutionContext context,
            T defaultValue,
            String contextName,
            OperationResult parentResult)
            throws ScriptExecutionException, SchemaException, ConfigurationException, ObjectNotFoundException,
            CommunicationException, SecurityViolationException, ExpressionEvaluationException {
        List<ActionParameterValueType> arguments = action.getParameter();
        ActionParameterValueType dynamicValue =
                getArgument(arguments, dynamicName, false, false, contextName);
        if (dynamicValue != null) {
            if (dynamicValue.getScriptingExpression() != null) {
                PipelineData data = scriptingExpressionEvaluator.evaluateExpression(
                        dynamicValue.getScriptingExpression(), input, context, parentResult);
                if (data != null) {
                    return data.getSingleValue(clazz);
                }
            } else if (dynamicValue.getValue() != null) {
                return ScriptingDataUtil.getRealValue(dynamicValue.getValue(), clazz);
            } else {
                throw new IllegalStateException("No expression nor value specified in parameter '" + dynamicName + "'");
            }
        }
        T staticValue = ScriptingBeansUtil.getBeanPropertyValue(action, staticName.getLocalPart(), clazz);
        if (staticValue != null) {
            return staticValue;
        }
        return defaultValue;
    }

    public Boolean getArgumentAsBoolean(
            List<ActionParameterValueType> arguments,
            String argumentName,
            PipelineData input,
            ExecutionContext context,
            Boolean defaultValue,
            String contextName,
            OperationResult result)
            throws ScriptExecutionException, SchemaException, ObjectNotFoundException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        String stringValue = getArgumentAsString(arguments, argumentName, input, context, null, contextName, result);
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

    public PipelineData evaluateParameter(
            ActionParameterValueType parameter,
            @Nullable Class<?> expectedClass,
            PipelineData input,
            ExecutionContext context,
            OperationResult result)
            throws ScriptExecutionException, SchemaException, ConfigurationException, ObjectNotFoundException,
            CommunicationException, SecurityViolationException, ExpressionEvaluationException {
        Validate.notNull(parameter, "parameter");
        if (parameter.getScriptingExpression() != null) {
            return scriptingExpressionEvaluator.evaluateExpression(
                    parameter.getScriptingExpression(), input, context, result);
        } else if (parameter.getValue() != null) {
            return scriptingExpressionEvaluator.evaluateConstantExpression(
                    (RawType) parameter.getValue(), expectedClass, context, "evaluating parameter " + parameter.getName());
        } else {
            throw new IllegalStateException("No expression nor value specified");
        }
    }

    public <T> T getSingleArgumentValue(
            List<ActionParameterValueType> arguments,
            String parameterName,
            boolean required,
            boolean requiredNonNull,
            String context,
            PipelineData input,
            ExecutionContext executionContext,
            Class<T> clazz,
            OperationResult result)
            throws ScriptExecutionException, SchemaException, ObjectNotFoundException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        ActionParameterValueType paramValue = getArgument(arguments, parameterName, required, requiredNonNull, context);
        if (paramValue == null) {
            return null;
        }
        PipelineData paramData = evaluateParameter(paramValue, clazz, input, executionContext, result);
        if (paramData.getData().size() != 1) {
            throw new ScriptExecutionException(
                    "Exactly one item was expected in '%s' parameter. Got %d".formatted(
                            parameterName, paramData.getData().size()));
        }
        PrismValue prismValue = paramData.getData().get(0).getValue();
        if (!(prismValue instanceof PrismPropertyValue)) {
            throw new ScriptExecutionException(
                    "A prism property value was expected in '%s' parameter. Got %s instead.".formatted(
                            parameterName, prismValue.getClass().getName()));
        }
        Object value = ((PrismPropertyValue<?>) prismValue).getValue();
        try {
            return JavaTypeConverter.convert(clazz, value);
        } catch (Throwable t) {
            throw new ScriptExecutionException(
                    "Couldn't retrieve value of parameter '" + parameterName + "': " + t.getMessage(), t);
        }
    }

    @NotNull
    public <T> Collection<T> getArgumentValues(
            List<ActionParameterValueType> arguments,
            String parameterName,
            boolean required,
            boolean requiredNonNull,
            String context,
            PipelineData input,
            ExecutionContext executionContext,
            Class<T> clazz,
            OperationResult result)
            throws ScriptExecutionException, SchemaException, ObjectNotFoundException,
            SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        List<T> rv = new ArrayList<>();
        for (ActionParameterValueType paramValue : arguments) {
            if (parameterName.equals(paramValue.getName())) {
                if (paramValue.getScriptingExpression() != null || paramValue.getValue() != null) {
                    PipelineData paramData = evaluateParameter(paramValue, clazz, input, executionContext, result);
                    for (PipelineItem item : paramData.getData()) {
                        PrismValue prismValue = item.getValue();
                        if (!(prismValue instanceof PrismPropertyValue)) {
                            throw new ScriptExecutionException(
                                    "A prism property value was expected in '%s' parameter. Got %s instead.".formatted(
                                            parameterName, prismValue.getClass().getName()));
                        } else {
                            rv.add(JavaTypeConverter.convert(clazz, prismValue.getRealValue()));
                        }
                    }
                } else {
                    if (requiredNonNull) {
                        throw new ScriptExecutionException(
                                "Required parameter " + parameterName + " is null in invocation of \"" + context + "\"");
                    } else {
                        return rv;
                    }
                }
            }
        }
        if (required && rv.isEmpty()) {
            throw new ScriptExecutionException(
                    "Required parameter " + parameterName + " not present in invocation of \"" + context + "\"");
        } else {
            return rv;
        }
    }
}
