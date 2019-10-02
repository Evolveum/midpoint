/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
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

	private static final String OP_EXECUTE = ReleaseWorkItemsAction.class.getName() + ".execute";

	public ReleaseWorkItemsAction(@NotNull EngineInvocationContext ctx, @NotNull ReleaseWorkItemsRequest request) {
		super(ctx, request);
	}

	@Override
	public Action execute(OperationResult parentResult) {
		OperationResult result = parentResult.subresult(OP_EXECUTE)
				.setMinor()
				.build();

		try {
			traceEnter(LOGGER);

			for (ReleaseWorkItemsRequest.SingleRelease release : request.getReleases()) {
				CaseWorkItemType workItem = ctx.findWorkItemById(release.getWorkItemId());
				if (workItem.getCloseTimestamp() != null) {
					LOGGER.debug("Work item {} in {} cannot be released because it's already closed", workItem,
							ctx.getCurrentCase());
					result.recordStatus(OperationResultStatus.NOT_APPLICABLE,
							"There are no candidates this work item can be offered to");
				} else if (workItem.getAssigneeRef().isEmpty()) {
					throw new SystemException("The work item is not assigned to a user");
				} else if (workItem.getAssigneeRef().size() > 1) {
					throw new SystemException("The work item is assigned to more than one user, so it cannot be released");
				} else if (!ctx.getPrincipal().getOid().equals(workItem.getAssigneeRef().get(0).getOid())) {
					throw new SystemException("The work item is not assigned to the current user");
				} else if (workItem.getCandidateRef().isEmpty()) {
					result.recordStatus(OperationResultStatus.NOT_APPLICABLE,
							"There are no candidates this work item can be offered to");
				} else {
					workItem.getAssigneeRef().clear();
				}
			}

			traceExit(LOGGER, null);
			return null;
		} catch (Throwable t) {
			result.recordFatalError(t);
			throw t;
		} finally {
			result.computeStatusIfUnknown();
		}
	}
}
