/*
 * Copyright (C) 2020-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.notifications.impl.events;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.cases.api.events.WorkItemOperationSourceInfo;
import com.evolveum.midpoint.notifications.api.events.SimpleObjectRef;
import com.evolveum.midpoint.notifications.api.events.WorkItemCustomEvent;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class WorkItemCustomEventImpl extends WorkItemEventImpl implements WorkItemCustomEvent {

    public WorkItemCustomEventImpl(
            @NotNull LightweightIdentifierGenerator lightweightIdentifierGenerator,
            @NotNull ChangeType changeType,
            @NotNull CaseWorkItemType workItem,
            @Nullable SimpleObjectRef assignee, @Nullable WorkItemOperationSourceInfo sourceInfo,
            @Nullable ApprovalContextType approvalContext, CaseType aCase) {
        super(lightweightIdentifierGenerator, changeType, workItem, assignee, null, null,
                sourceInfo, approvalContext, aCase, null);
    }

    @Override
    public boolean isCategoryType(EventCategoryType eventCategory) {
        return eventCategory == EventCategoryType.WORK_ITEM_CUSTOM_EVENT
                || eventCategory == EventCategoryType.WORK_ITEM_EVENT
                || eventCategory == EventCategoryType.WORKFLOW_EVENT;
    }

    @Override
    public WorkItemNotificationActionType getNotificationAction() {
        return (WorkItemNotificationActionType) getSource();
    }
}
