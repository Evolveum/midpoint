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
import com.evolveum.midpoint.model.scripting.ScriptExecutionException;
import com.evolveum.midpoint.model.scripting.RootExpressionEvaluator;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.xml.ns._public.model.scripting_2.ActionExpressionType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_2.ActionParameterValueType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_2.ExpressionType;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;

/**
 * @author mederly
 */
public abstract class BaseActionExecutor implements ActionExecutor {

    @Autowired
    protected RootExpressionEvaluator rootExpressionEvaluator;

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

    //protected Data getArgumentValue(ActionExpressionType actionExpression, String parameterName, boolean required) throws ScriptExecutionException {

}
