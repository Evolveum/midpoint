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
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.request.ReleaseWorkItemsRequest;
import com.evolveum.midpoint.wf.impl.engine.EngineInvocationContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public class ReleaseWorkItemsAction extends RequestedAction<ReleaseWorkItemsRequest> {

	private static final Trace LOGGER = TraceManager.getTrace(ReleaseWorkItemsAction.class);

	public ReleaseWorkItemsAction(@NotNull EngineInvocationContext ctx, @NotNull ReleaseWorkItemsRequest request) {
		super(ctx, request);
	}

	@Override
	public Action execute(OperationResult result) {
		traceEnter(LOGGER);

		for (ReleaseWorkItemsRequest.SingleRelease release : request.getReleases()) {
			CaseWorkItemType workItem = ctx.findWorkItemById(release.getWorkItemId());
			if (workItem.getCloseTimestamp() != null) {
				LOGGER.debug("Work item {} in {} cannot be released because it's already closed", workItem, ctx.getCurrentCase());
				result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "There are no candidates this work item can be offered to");
			} else if (workItem.getAssigneeRef().isEmpty()) {
				throw new SystemException("The work item is not assigned to a user");
			} else if (workItem.getAssigneeRef().size() > 1) {
				throw new SystemException("The work item is assigned to more than one user, so it cannot be released");
			} else if (!ctx.getPrincipal().getOid().equals(workItem.getAssigneeRef().get(0).getOid())) {
				throw new SystemException("The work item is not assigned to the current user");
			} else if (workItem.getCandidateRef().isEmpty()) {
				result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "There are no candidates this work item can be offered to");
			} else {
				workItem.getAssigneeRef().clear();
			}
		}

		traceExit(LOGGER, null);
		return null;
	}
}
