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

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.request.OpenCaseRequest;
import com.evolveum.midpoint.wf.impl.engine.helpers.DelayedNotification;
import com.evolveum.midpoint.wf.impl.engine.EngineInvocationContext;

/**
 *
 */
public class OpenCaseAction extends RequestedAction<OpenCaseRequest> {

	private static final Trace LOGGER = TraceManager.getTrace(OpenCaseAction.class);

	public OpenCaseAction(EngineInvocationContext ctx, OpenCaseRequest request) {
		super(ctx, request);
	}

	@Override
	public Action execute(OperationResult result) {
		traceEnter(LOGGER);

		Action next;
		String currentState = ctx.getCurrentCase().getState();
		if (currentState != null && !currentState.equals(SchemaConstants.CASE_STATE_CREATED)) { // todo URI comparison
			LOGGER.debug("Case was already opened; its state is {}", currentState);
			next = null;
		} else {
			engine.auditHelper.prepareProcessStartRecord(ctx, result);
			ctx.prepareNotification(new DelayedNotification.ProcessStart(ctx.getCurrentCase()));
			ctx.getCurrentCase().setState(SchemaConstants.CASE_STATE_OPEN);

			if (ctx.getNumberOfStages() > 0) {
				next = new OpenStageAction(ctx);
			} else {
				next = new CloseCaseAction(ctx, SchemaConstants.MODEL_APPROVAL_OUTCOME_APPROVE);
			}
		}

		traceExit(LOGGER, next);
		return next;
	}
}
