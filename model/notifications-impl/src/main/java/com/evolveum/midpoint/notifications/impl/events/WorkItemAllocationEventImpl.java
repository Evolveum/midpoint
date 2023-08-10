/*
 * Copyright (C) 2020-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.notifications.impl.events;

import javax.xml.datatype.Duration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.cases.api.events.WorkItemOperationInfo;
import com.evolveum.midpoint.cases.api.events.WorkItemOperationSourceInfo;
import com.evolveum.midpoint.notifications.api.events.SimpleObjectRef;
import com.evolveum.midpoint.notifications.api.events.WorkItemAllocationEvent;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalContextType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventCategoryType;

public class WorkItemAllocationEventImpl extends WorkItemEventImpl implements WorkItemAllocationEvent {

    public WorkItemAllocationEventImpl(@NotNull LightweightIdentifierGenerator lightweightIdentifierGenerator,
            @NotNull ChangeType changeType,
            @NotNull CaseWorkItemType workItem, @Nullable SimpleObjectRef assignee, @Nullable SimpleObjectRef initiator,
            @Nullable WorkItemOperationInfo operationInfo, @Nullable WorkItemOperationSourceInfo sourceInfo,
            @Nullable ApprovalContextType approvalContext, @NotNull CaseType aCase,
            @Nullable Duration timeBefore) {
        super(lightweightIdentifierGenerator, changeType, workItem, assignee, initiator, operationInfo, sourceInfo,
                approvalContext, aCase, timeBefore);
    }

    @Override
    public boolean isCategoryType(EventCategoryType eventCategory) {
        return eventCategory == EventCategoryType.WORK_ITEM_ALLOCATION_EVENT
                || eventCategory == EventCategoryType.WORK_ITEM_EVENT
                || eventCategory == EventCategoryType.WORKFLOW_EVENT;
    }
}
