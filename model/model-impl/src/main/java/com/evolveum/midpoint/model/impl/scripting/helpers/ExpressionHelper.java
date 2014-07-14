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

import com.evolveum.midpoint.model.impl.scripting.Data;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.impl.scripting.ScriptExecutionException;
import com.evolveum.midpoint.model.impl.scripting.ScriptingExpressionEvaluator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionParameterValueType;

import com.evolveum.prism.xml.ns._public.types_3.RawType;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;

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

    public ActionParameterValueType getArgument(List<ActionParameterValueType> arguments, String parameterName, boolean required, boolean requiredNonNull, String context) throws ScriptExecutionException {
        for (ActionParameterValueType parameterValue : arguments) {
            if (parameterName.equals(parameterValue.getName())) {
                if (parameterValue.getExpression() != null || parameterValue.getValue() != null) {
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

    public String getArgumentAsString(List<ActionParameterValueType> arguments, String argumentName, Data input, ExecutionContext context, String defaultValue, String contextName, OperationResult parentResult) throws ScriptExecutionException {
        ActionParameterValueType parameterValue = getArgument(arguments, argumentName, false, false, contextName);
        if (parameterValue != null) {
            if (parameterValue.getExpression() != null) {
                Data data = scriptingExpressionEvaluator.evaluateExpression(parameterValue.getExpression(), input, context, parentResult);
                if (data != null) {
                    return data.getDataAsSingleString();
                }
            } else if (parameterValue.getValue() != null) {
                Data data = scriptingExpressionEvaluator.evaluateConstantExpression((RawType) parameterValue.getValue(), context, parentResult);
                if (data != null) {
                    return data.getDataAsSingleString();
                }
            } else {
                throw new IllegalStateException("No expression nor value specified");
            }
        }
        return defaultValue;
    }

    public Boolean getArgumentAsBoolean(List<ActionParameterValueType> arguments, String argumentName, Data input, ExecutionContext context, Boolean defaultValue, String contextName, OperationResult parentResult) throws ScriptExecutionException {
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

    public Data evaluateParameter(ActionParameterValueType parameter, Data input, ExecutionContext context, OperationResult result) throws ScriptExecutionException {
        Validate.notNull(parameter, "parameter");
        if (parameter.getExpression() != null) {
            return scriptingExpressionEvaluator.evaluateExpression(parameter.getExpression(), input, context, result);
        } else if (parameter.getValue() != null) {
            return scriptingExpressionEvaluator.evaluateConstantExpression((RawType) parameter.getValue(), context, result);
        } else {
            throw new IllegalStateException("No expression nor value specified");
        }
    }
}
