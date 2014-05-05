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

package com.evolveum.midpoint.model.scripting.helpers;

import com.evolveum.midpoint.model.scripting.Data;
import com.evolveum.midpoint.model.scripting.ExecutionContext;
import com.evolveum.midpoint.model.scripting.ScriptExecutionException;
import com.evolveum.midpoint.model.scripting.ScriptingExpressionEvaluator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionParameterValueType;
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

    public JAXBElement<?> getArgument(ActionExpressionType actionExpression, String parameterName) throws ScriptExecutionException {
        return getArgument(actionExpression.getParameter(), parameterName, false, false, actionExpression.getType());
    }

    public JAXBElement<?> getArgument(List<ActionParameterValueType> arguments, String parameterName, boolean required, boolean requiredNonNull, String context) throws ScriptExecutionException {
        for (ActionParameterValueType parameterValue : arguments) {
            if (parameterName.equals(parameterValue.getName())) {
                if (parameterValue.getExpression() != null) {
                    return parameterValue.getExpression();
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
        JAXBElement<?> argumentExpression = getArgument(arguments, argumentName, false, false, contextName);
        if (argumentExpression != null) {
            Data level = scriptingExpressionEvaluator.evaluateExpression(argumentExpression, input, context, parentResult);
            if (level != null) {
                return level.getDataAsSingleString();
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

}
