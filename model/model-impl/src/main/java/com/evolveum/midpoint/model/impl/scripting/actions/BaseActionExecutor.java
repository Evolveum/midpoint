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

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.impl.scripting.ActionExecutor;
import com.evolveum.midpoint.model.impl.scripting.PipelineData;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.api.ScriptExecutionException;
import com.evolveum.midpoint.model.impl.scripting.ScriptingExpressionEvaluator;
import com.evolveum.midpoint.model.impl.scripting.helpers.ExpressionHelper;
import com.evolveum.midpoint.model.impl.scripting.helpers.OperationsHelper;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.SecurityEnforcer;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author mederly
 */
public abstract class BaseActionExecutor implements ActionExecutor {

	private static final Trace LOGGER = TraceManager.getTrace(BaseActionExecutor.class);

	private static final String PARAM_RAW = "raw";
    private static final String PARAM_DRY_RUN = "dryRun";

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

	@Autowired
	protected SecurityEnforcer securityEnforcer;

    // todo move to some helper?
    protected boolean getParamRaw(ActionExpressionType expression, PipelineData input, ExecutionContext context, OperationResult result) throws ScriptExecutionException {
        return expressionHelper.getArgumentAsBoolean(expression.getParameter(), PARAM_RAW, input, context, false, PARAM_RAW, result);
    }

    protected boolean getParamDryRun(ActionExpressionType expression, PipelineData input, ExecutionContext context, OperationResult result) throws ScriptExecutionException {
        return expressionHelper.getArgumentAsBoolean(expression.getParameter(), PARAM_DRY_RUN, input, context, false, PARAM_DRY_RUN, result);
    }

    protected String rawSuffix(boolean raw) {
        return raw ? " (raw)" : "";
    }

    protected String drySuffix(boolean dry) {
        return dry ? " (dry run)" : "";
    }

    protected String rawDrySuffix(boolean raw, boolean dry) {
        return rawSuffix(raw) + drySuffix(dry);
    }

    protected String exceptionSuffix(Throwable t) {
    	return t != null ? " (error: " + t.getClass().getSimpleName() + ": " + t.getMessage() + ")" : "";
	}

	protected Throwable processActionException(Throwable e, String actionName, PrismValue value, ExecutionContext context) throws ScriptExecutionException {
    	if (context.isContinueOnAnyError()) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't execute action '{}' on {}: {}", e,
					actionName, value, e.getMessage());
			return e;
		} else {
    		throw new ScriptExecutionException("Couldn't execute action '" + actionName + "' on " + value + ": " + e.getMessage(), e);
		}
	}
}
