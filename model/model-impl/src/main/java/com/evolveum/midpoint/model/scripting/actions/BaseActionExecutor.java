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

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.scripting.ActionExecutor;
import com.evolveum.midpoint.model.scripting.ScriptingExpressionEvaluator;
import com.evolveum.midpoint.model.scripting.helpers.ExpressionHelper;
import com.evolveum.midpoint.model.scripting.helpers.OperationsHelper;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author mederly
 */
public abstract class BaseActionExecutor implements ActionExecutor {

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

}
