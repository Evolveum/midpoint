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
import com.evolveum.midpoint.wf.api.WorkItemOperationSourceInfo;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author mederly
 */
public class WorkItemCustomEvent extends WorkItemEvent {

    public WorkItemCustomEvent(@NotNull LightweightIdentifierGenerator lightweightIdentifierGenerator,
            @NotNull ChangeType changeType,
            @NotNull CaseWorkItemType workItem,
            @Nullable SimpleObjectRef assignee, @Nullable WorkItemOperationSourceInfo sourceInfo,
            @Nullable ApprovalContextType approvalContext, CaseType aCase,
            @Nullable EventHandlerType handler) {
        super(lightweightIdentifierGenerator, changeType, workItem, assignee, null, null,
                sourceInfo, approvalContext, aCase, handler, null);
    }

    @Override
    public boolean isCategoryType(EventCategoryType eventCategoryType) {
        return eventCategoryType == EventCategoryType.WORK_ITEM_CUSTOM_EVENT
                || eventCategoryType == EventCategoryType.WORK_ITEM_EVENT
                || eventCategoryType == EventCategoryType.WORKFLOW_EVENT;
    }

    @Override
    public void createExpressionVariables(VariablesMap variables, OperationResult result) {
        super.createExpressionVariables(variables, result);
    }

    public WorkItemNotificationActionType getNotificationAction() {
        return (WorkItemNotificationActionType) getSource();
    }

    @Override
    public String toString() {
        return "WorkItemCustomEvent:" + super.toString();
    }

}
