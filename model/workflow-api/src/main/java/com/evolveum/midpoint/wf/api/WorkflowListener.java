/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.api;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.datatype.Duration;

/**
 * An interface through which external observers can be notified about workflow related events.
 * Used e.g. for implementing workflow-related notifications.
 *
 * EXPERIMENTAL. This interface may change in near future.
 *
 * @author mederly
 */
public interface WorkflowListener {

    /**
     * This method is called by wf module when a process instance successfully starts.
     * @param result implementer should report its result here
     */
    void onProcessInstanceStart(CaseType aCase, Task task, OperationResult result);

    /**
     * This method is called by wf module when a process instance ends.
     * @param result implementer should report its result here
     */
    void onProcessInstanceEnd(CaseType aCase, Task task, OperationResult result);

    /**
     * This method is called by wf module when a work item is created.
     */
    void onWorkItemCreation(ObjectReferenceType assignee, @NotNull CaseWorkItemType workItem,
            CaseType aCase, Task task, OperationResult result);

    /**
     * This method is called by wf module when a work item is completed.
     */
    void onWorkItemDeletion(ObjectReferenceType assignee, @NotNull CaseWorkItemType workItem,
            @Nullable WorkItemOperationInfo operationInfo, @Nullable WorkItemOperationSourceInfo sourceInfo,
            CaseType aCase, Task task, OperationResult result);

    void onWorkItemCustomEvent(ObjectReferenceType assignee, @NotNull CaseWorkItemType workItem,
            @NotNull WorkItemNotificationActionType notificationAction, @Nullable WorkItemEventCauseInformationType cause,
            CaseType aCase, Task task, OperationResult result);

    /**
     * EXPERIMENTAL
     */
    void onWorkItemAllocationChangeCurrentActors(@NotNull CaseWorkItemType workItem,
            @NotNull WorkItemAllocationChangeOperationInfo operationInfo, @Nullable WorkItemOperationSourceInfo sourceInfo,
            Duration timeBefore, CaseType aCase, Task task, OperationResult result);

    void onWorkItemAllocationChangeNewActors(@NotNull CaseWorkItemType workItem,
            @NotNull WorkItemAllocationChangeOperationInfo operationInfo, @Nullable WorkItemOperationSourceInfo sourceInfo,
            CaseType aCase, Task task, OperationResult result);
}
