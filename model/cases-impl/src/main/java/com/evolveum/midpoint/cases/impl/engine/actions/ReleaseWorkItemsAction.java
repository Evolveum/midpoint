/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.cases.impl.engine.actions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.cases.api.request.ReleaseWorkItemsRequest;
import com.evolveum.midpoint.cases.impl.engine.CaseEngineOperationImpl;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;

/**
 * Releases work items.
 */
public class ReleaseWorkItemsAction extends RequestedAction<ReleaseWorkItemsRequest> {

    private static final Trace LOGGER = TraceManager.getTrace(ReleaseWorkItemsAction.class);

    public ReleaseWorkItemsAction(@NotNull CaseEngineOperationImpl ctx, @NotNull ReleaseWorkItemsRequest request) {
        super(ctx, request, LOGGER);
    }

    @Override
    public @Nullable Action executeInternal(OperationResult result) {
        for (ReleaseWorkItemsRequest.SingleRelease release : request.getReleases()) {
            CaseWorkItemType workItem = operation.getWorkItemById(release.getWorkItemId());
            if (workItem.getCloseTimestamp() != null) {
                LOGGER.debug("Work item {} in {} cannot be released because it's already closed",
                        workItem, operation.getCurrentCase());
                result.recordNotApplicable("There are no candidates this work item can be offered to");
            } else if (workItem.getAssigneeRef().isEmpty()) {
                throw new SystemException("The work item is not assigned to a user");
            } else if (workItem.getAssigneeRef().size() > 1) {
                throw new SystemException("The work item is assigned to more than one user, so it cannot be released");
            } else if (!operation.getPrincipal().getOid().equals(workItem.getAssigneeRef().get(0).getOid())) {
                throw new SystemException("The work item is not assigned to the current user");
            } else if (workItem.getCandidateRef().isEmpty()) {
                result.recordNotApplicable("There are no candidates this work item can be offered to");
            } else {
                workItem.getAssigneeRef().clear();
            }
        }
        return null;
    }
}
