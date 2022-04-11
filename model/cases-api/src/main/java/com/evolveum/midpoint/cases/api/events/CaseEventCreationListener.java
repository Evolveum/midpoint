/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.cases.api.events;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.datatype.Duration;

/**
 * An interface through which external observers can be notified about case-related events.
 * Used to implement case-related notifications. It is here to avoid a dependency of case-impl
 * (i.e. the producer of change notification events) on notification-impl (where change events
 * are implemented).
 *
 * In the future, the specific events (case, model, resource objects, ...) PROBABLY will be
 * implemented in the respective modules (case-impl, model-impl, provisioning-impl, etc).
 * Then this interface will disappear.
 */
@Experimental
public interface CaseEventCreationListener {

    /**
     * This method is called by cases module when a case is opened.
     * @param result implementer should report its result here
     */
    void onCaseOpening(CaseType aCase, Task task, OperationResult result);

    /**
     * This method is called by cases module when a case is being closed.
     * (There is an uncertainty about closing vs closed state.)
     * @param result implementer should report its result here
     */
    void onCaseClosing(CaseType aCase, Task task, OperationResult result);

    /**
     * This method is called by cases module when a work item is created.
     */
    void onWorkItemCreation(
            ObjectReferenceType assignee,
            @NotNull CaseWorkItemType workItem,
            CaseType aCase,
            Task task,
            OperationResult result);

    /**
     * This method is called by cases module when a work item is being closed (completed or cancelled).
     */
    void onWorkItemClosing(ObjectReferenceType assignee, @NotNull CaseWorkItemType workItem,
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
