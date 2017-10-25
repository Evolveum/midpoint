/*
 * Copyright (c) 2010-2017 Evolveum
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
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ModelExecuteOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PartialProcessingOptionsType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;
import org.springframework.beans.factory.annotation.Autowired;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.PartialProcessingTypeType.SKIP;

/**
 * @author mederly
 */
public abstract class BaseActionExecutor implements ActionExecutor {

	private static final Trace LOGGER = TraceManager.getTrace(BaseActionExecutor.class);

	private static final String PARAM_RAW = "raw";
    private static final String PARAM_DRY_RUN = "dryRun";
	private static final String PARAM_SKIP_APPROVALS = "skipApprovals";
	private static final String PARAM_OPTIONS = "options";

    @Autowired protected ScriptingExpressionEvaluator scriptingExpressionEvaluator;
    @Autowired protected PrismContext prismContext;
    @Autowired protected OperationsHelper operationsHelper;
    @Autowired protected ExpressionHelper expressionHelper;
    @Autowired protected ProvisioningService provisioningService;
    @Autowired protected ModelService modelService;
	@Autowired protected SecurityEnforcer securityEnforcer;
	@Autowired protected SecurityContextManager securityContextManager;

    // todo move to some helper?

	protected ModelExecuteOptions getOptions(ActionExpressionType expression, PipelineData input, ExecutionContext context, OperationResult result) throws  ScriptExecutionException {
		boolean raw = expressionHelper.getArgumentAsBoolean(expression.getParameter(), PARAM_RAW, input, context, false, PARAM_RAW, result);
		boolean skipApprovals = expressionHelper.getArgumentAsBoolean(expression.getParameter(), PARAM_SKIP_APPROVALS, input, context, false, PARAM_RAW, result);
		ModelExecuteOptionsType optionsBean = expressionHelper.getSingleArgumentValue(expression.getParameter(), PARAM_OPTIONS, false, false, "options", input, context,
				ModelExecuteOptionsType.class, result);
		ModelExecuteOptions options;
		if (optionsBean != null) {
			options = ModelExecuteOptions.fromModelExecutionOptionsType(optionsBean);
		} else {
			options = new ModelExecuteOptions();
		}
		if (raw) {
			options.setRaw(true);
		}
		if (skipApprovals) {
			if (options.getPartialProcessing() != null) {
				options.getPartialProcessing().setApprovals(SKIP);
			} else {
				options.setPartialProcessing(new PartialProcessingOptionsType().approvals(SKIP));
			}
		}
		return options;
	}

    protected boolean getParamDryRun(ActionExpressionType expression, PipelineData input, ExecutionContext context, OperationResult result) throws ScriptExecutionException {
        return expressionHelper.getArgumentAsBoolean(expression.getParameter(), PARAM_DRY_RUN, input, context, false, PARAM_DRY_RUN, result);
    }

    private String optionsSuffix(ModelExecuteOptions options) {
        return options.notEmpty() ? " " + options : "";
    }

    protected String drySuffix(boolean dry) {
        return dry ? " (dry run)" : "";
    }

    protected String optionsSuffix(ModelExecuteOptions options, boolean dry) {
        return optionsSuffix(options) + drySuffix(dry);
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

	protected void checkRootAuthorization(ExecutionContext context,
			OperationResult globalResult, String actionName) throws ScriptExecutionException {
		if (context.isPrivileged()) {
			return;
		}
		try {
			securityEnforcer.authorize(AuthorizationConstants.AUTZ_ALL_URL, null, null, null, null, null, context.getTask(), globalResult);
		} catch (SecurityViolationException | SchemaException | ExpressionEvaluationException | ObjectNotFoundException | CommunicationException | ConfigurationException e) {
			throw new ScriptExecutionException("You are not authorized to execute '" + actionName + "' action.");
		}
	}

}
