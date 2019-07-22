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

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.request.ClaimWorkItemsRequest;
import com.evolveum.midpoint.wf.impl.engine.EngineInvocationContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public class ClaimWorkItemsAction extends RequestedAction<ClaimWorkItemsRequest> {

	private static final Trace LOGGER = TraceManager.getTrace(ClaimWorkItemsAction.class);

	public ClaimWorkItemsAction(@NotNull EngineInvocationContext ctx, @NotNull ClaimWorkItemsRequest request) {
		super(ctx, request);
	}

	@Override
	public Action execute(OperationResult result) throws SecurityViolationException {
		traceEnter(LOGGER);

		for (ClaimWorkItemsRequest.SingleClaim claim : request.getClaims()) {
			CaseWorkItemType workItem = ctx.findWorkItemById(claim.getWorkItemId());
			if (workItem.getCloseTimestamp() != null) {
				result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "Work item has been already closed");     // todo better result handling
			} else if (!workItem.getAssigneeRef().isEmpty()) {
				String desc;
				if (workItem.getAssigneeRef().size() == 1 && ctx.getPrincipal().getOid().equals(workItem.getAssigneeRef().get(0).getOid())) {
					desc = "the current";
				} else {
					desc = "another";
				}
				throw new SystemException("The work item is already assigned to "+desc+" user");
			} else if (!engine.authorizationHelper.isAuthorizedToClaim(workItem)) {
				throw new SecurityViolationException("You are not authorized to claim the selected work item.");
			} else {
				workItem.getAssigneeRef().add(ctx.getPrincipal().toObjectReference().clone());
			}
		}

		traceExit(LOGGER, null);
		return null;
	}
}
