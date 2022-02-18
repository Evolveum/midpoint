/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.cases.impl.engine.actions;

import com.evolveum.midpoint.cases.impl.engine.CaseEngineOperationImpl;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.cases.api.request.ClaimWorkItemsRequest;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Claims the work items.
 */
public class ClaimWorkItemsAction extends RequestedAction<ClaimWorkItemsRequest> {

    private static final Trace LOGGER = TraceManager.getTrace(ClaimWorkItemsAction.class);

    public ClaimWorkItemsAction(@NotNull CaseEngineOperationImpl ctx, @NotNull ClaimWorkItemsRequest request) {
        super(ctx, request, LOGGER);
    }

    @Override
    public @Nullable Action executeInternal(OperationResult result) throws SecurityViolationException {
        for (ClaimWorkItemsRequest.SingleClaim claim : request.getClaims()) {
            CaseWorkItemType workItem = operation.getWorkItemById(claim.getWorkItemId());
            if (workItem.getCloseTimestamp() != null) {
                result.recordNotApplicable("Work item has been already closed"); // todo better result handling
            } else if (!workItem.getAssigneeRef().isEmpty()) {
                String desc;
                if (workItem.getAssigneeRef().size() == 1
                        && operation.getPrincipal().getOid().equals(workItem.getAssigneeRef().get(0).getOid())) {
                    desc = "the current";
                } else {
                    desc = "another";
                }
                throw new SystemException("The work item is already assigned to " + desc + " user");
            } else if (!beans.authorizationHelper.isAuthorizedToClaim(workItem)) {
                throw new SecurityViolationException("You are not authorized to claim the selected work item.");
            } else {
                workItem.getAssigneeRef().add(operation.getPrincipal().toObjectReference().clone());
            }
        }
        return null;
    }
}
