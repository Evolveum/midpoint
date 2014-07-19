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

package com.evolveum.midpoint.model.impl.scripting.actions;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.impl.scripting.ActionExecutor;
import com.evolveum.midpoint.model.impl.scripting.Data;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.impl.scripting.ScriptExecutionException;
import com.evolveum.midpoint.model.impl.scripting.ScriptingExpressionEvaluator;
import com.evolveum.midpoint.model.impl.scripting.helpers.ExpressionHelper;
import com.evolveum.midpoint.model.impl.scripting.helpers.OperationsHelper;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author mederly
 */
public abstract class BaseActionExecutor implements ActionExecutor {

    private static final String PARAM_RAW = "raw";

    @Autowired
    protected ScriptingExpressionEvaluator scriptingExpressionEvaluator;

    @Autowired
    protected PrismContext prismContext;

    @Autowired
    protected OperationsHelper operationsHelper;

    @Autowired
    protected ExpressionHelper expressionHelper;

    @Autowired
    protected ProvisioningService provisioningService;

    @Autowired
    protected ModelService modelService;

    //protected Data getArgumentValue(ActionExpressionType actionExpression, String parameterName, boolean required) throws ScriptExecutionException {

    // todo move to some helper?
    protected boolean getParamRaw(ActionExpressionType expression, Data input, ExecutionContext context, OperationResult result) throws ScriptExecutionException {
        return expressionHelper.getArgumentAsBoolean(expression.getParameter(), PARAM_RAW, input, context, false, PARAM_RAW, result);
    }

    protected String rawSuffix(boolean raw) {
        return raw ? " (raw)" : "";
    }

}
