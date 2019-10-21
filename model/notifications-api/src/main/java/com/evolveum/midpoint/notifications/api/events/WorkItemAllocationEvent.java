/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.api.events;

import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.wf.api.WorkItemOperationInfo;
import com.evolveum.midpoint.wf.api.WorkItemOperationSourceInfo;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.datatype.Duration;

/**
 * @author mederly
 */
public class WorkItemAllocationEvent extends WorkItemEvent {

    public WorkItemAllocationEvent(@NotNull LightweightIdentifierGenerator lightweightIdentifierGenerator,
            @NotNull ChangeType changeType,
            @NotNull CaseWorkItemType workItem, @Nullable SimpleObjectRef assignee, @Nullable SimpleObjectRef initiator,
            @Nullable WorkItemOperationInfo operationInfo, @Nullable WorkItemOperationSourceInfo sourceInfo,
            @Nullable ApprovalContextType approvalContext, @NotNull CaseType aCase,
            @Nullable Duration timeBefore) {
        super(lightweightIdentifierGenerator, changeType, workItem, assignee, initiator, operationInfo, sourceInfo,
                approvalContext, aCase, null, timeBefore);
    }

    @Override
    public boolean isCategoryType(EventCategoryType eventCategoryType) {
        return eventCategoryType == EventCategoryType.WORK_ITEM_ALLOCATION_EVENT
                || eventCategoryType == EventCategoryType.WORK_ITEM_EVENT
                || eventCategoryType == EventCategoryType.WORKFLOW_EVENT;
    }

    @Override
    public void createExpressionVariables(VariablesMap variables, OperationResult result) {
        super.createExpressionVariables(variables, result);
    }

    @Override
    public String toString() {
        return "WorkItemAllocationEvent:" + super.toString();
    }

}
