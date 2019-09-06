/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.engine.actions;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.request.OpenCaseRequest;
import com.evolveum.midpoint.wf.impl.engine.EngineInvocationContext;
import com.evolveum.midpoint.wf.impl.engine.helpers.DelayedNotification;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;

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

		boolean approvalCase = ctx.isApprovalCase();
		CaseType currentCase = ctx.getCurrentCase();
		String currentState = currentCase.getState();

		Action next;
		if (currentState != null && !currentState.equals(SchemaConstants.CASE_STATE_CREATED)) { // todo URI comparison
			LOGGER.debug("Case was already opened; its state is {}", currentState);
			next = null;
		} else {
			engine.auditHelper.prepareProcessStartRecord(ctx, result);
			ctx.prepareNotification(new DelayedNotification.ProcessStart(currentCase));

			if (!approvalCase) {
				// Work items are already there, so let's audit and notify their creation
				for (CaseWorkItemType workItem : currentCase.getWorkItem()) {
					OpenStageAction.prepareAuditAndNotifications(workItem, result, ctx, engine);
				}
			}

			currentCase.setState(SchemaConstants.CASE_STATE_OPEN);

			if (approvalCase) {
				if (ctx.getNumberOfStages() > 0) {
					next = new OpenStageAction(ctx);
				} else {
					next = new CloseCaseAction(ctx, SchemaConstants.MODEL_APPROVAL_OUTCOME_APPROVE);
				}
			} else {
				// For manual cases we assume that work items were created by the manual connector.
				// So we simply wait for their completion by appropriate assignee.
				// In the future we might
				//  1. create work items at this point - based on some "workflow schema" defined in the resource
				//  2. or, at least, check if there is any approver defined
				//  3. as an option the manual connector would simply signal "create a case" and Workflow manager would
				//     create the case according to the spec provided by the manual connector
				next = null;
			}
		}

		traceExit(LOGGER, next);
		return next;
	}
}
