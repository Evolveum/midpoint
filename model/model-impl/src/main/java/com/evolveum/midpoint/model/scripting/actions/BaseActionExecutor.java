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

package com.evolveum.midpoint.model.scripting.actions;

import com.evolveum.midpoint.model.scripting.ActionExecutor;
import com.evolveum.midpoint.model.scripting.Data;
import com.evolveum.midpoint.model.scripting.ExecutionContext;
import com.evolveum.midpoint.model.scripting.ScriptExecutionException;
import com.evolveum.midpoint.model.scripting.ScriptExpressionEvaluator;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.model.scripting_2.ActionExpressionType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_2.ActionParameterValueType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_2.ExpressionType;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author mederly
 */
public abstract class BaseActionExecutor implements ActionExecutor {

    @Autowired
    protected ScriptExpressionEvaluator scriptExpressionEvaluator;

    @Autowired
    protected PrismContext prismContext;

    protected ExpressionType getArgument(ActionExpressionType actionExpression, String parameterName) throws ScriptExecutionException {
        return getArgument(actionExpression, parameterName, false, false);
    }

    protected ExpressionType getArgument(ActionExpressionType actionExpression, String parameterName, boolean required, boolean requiredNonNull) throws ScriptExecutionException {
        String actionName = actionExpression.getType();
        for (ActionParameterValueType parameterValue : actionExpression.getParameter()) {
            if (parameterName.equals(parameterValue.getName())) {
                if (parameterValue.getExpression() != null) {
                    return parameterValue.getExpression().getValue();
                } else {
                    if (requiredNonNull) {
                        throw new ScriptExecutionException("Required parameter " + parameterName + " is null in invocation of \"" + actionName + "\" action ");
                    } else {
                        return null;
                    }
                }
            }
        }
        if (required) {
            throw new ScriptExecutionException("Required parameter " + parameterName + " not present in invocation of \"" + actionName + "\" action ");
        } else {
            return null;
        }
    }

    protected String getArgumentAsString(ActionExpressionType expression, String argumentName, Data input, ExecutionContext context, String defaultValue, OperationResult parentResult) throws ScriptExecutionException {
        ExpressionType argumentExpression = getArgument(expression, argumentName);
        if (argumentExpression != null) {
            Data level = scriptExpressionEvaluator.evaluateExpression(argumentExpression, input, context, parentResult);
            if (level != null) {
                return level.getDataAsSingleString();
            }
        }
        return defaultValue;
    }

    protected Boolean getArgumentAsBoolean(ActionExpressionType expression, String argumentName, Data input, ExecutionContext context, Boolean defaultValue, OperationResult parentResult) throws ScriptExecutionException {
        String stringValue = getArgumentAsString(expression, argumentName, input, context, null, parentResult);
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


    //protected Data getArgumentValue(ActionExpressionType actionExpression, String parameterName, boolean required) throws ScriptExecutionException {

}
