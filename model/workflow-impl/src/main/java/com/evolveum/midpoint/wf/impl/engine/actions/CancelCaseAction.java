/*
 * Copyright (c) 2010-2019 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.wf.impl.engine.actions;

import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.enforcer.api.AuthorizationParameters;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.request.CancelCaseRequest;
import com.evolveum.midpoint.wf.impl.engine.EngineInvocationContext;

/**
 *
 */
public class CancelCaseAction extends RequestedAction<CancelCaseRequest> {

	private static final Trace LOGGER = TraceManager.getTrace(CancelCaseAction.class);

	public CancelCaseAction(EngineInvocationContext ctx, CancelCaseRequest request) {
		super(ctx, request);
	}

	@Override
	public Action execute(OperationResult result)
			throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
			ConfigurationException, ExpressionEvaluationException {
		traceEnter(LOGGER);

		engine.securityEnforcer.authorize(ModelAuthorizationAction.STOP_APPROVAL_PROCESS_INSTANCE.getUrl(), null,
				AuthorizationParameters.Builder.buildObject(ctx.getCurrentCase().asPrismObject()), null, ctx.getTask(), result);

		// TODO consider putting some events and notifications here

		CloseCaseAction next = new CloseCaseAction(ctx, null);
		traceExit(LOGGER, next);
		return next;
	}
}
