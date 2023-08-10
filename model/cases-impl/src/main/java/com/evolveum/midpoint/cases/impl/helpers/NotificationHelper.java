/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.cases.impl.helpers;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.datatype.Duration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.cases.api.events.CaseEventCreationListener;
import com.evolveum.midpoint.cases.api.events.FutureNotificationEvent;
import com.evolveum.midpoint.cases.api.events.WorkItemAllocationChangeOperationInfo;
import com.evolveum.midpoint.cases.api.events.WorkItemOperationSourceInfo;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * TODO
 */
@Component
public class NotificationHelper {

    private static final Trace LOGGER = TraceManager.getTrace(NotificationHelper.class);

    private final Set<CaseEventCreationListener> notificationEventCreationListeners = ConcurrentHashMap.newKeySet();

    // The following two methods are of "immediate notification" kind. They are an exception; usually we
    // prepare notifications first and send them only after the case modification succeeds.

    public void notifyWorkItemAllocationChangeCurrentActors(CaseWorkItemType workItem,
            @NotNull WorkItemAllocationChangeOperationInfo operationInfo,
            WorkItemOperationSourceInfo sourceInfo, Duration timeBefore,
            CaseType aCase, RunningTask opTask, OperationResult result) {
        for (CaseEventCreationListener eventCreationListener : notificationEventCreationListeners) {
            eventCreationListener.onWorkItemAllocationChangeCurrentActors(
                    workItem, operationInfo, sourceInfo, timeBefore, aCase, opTask, result);
        }
    }

    public void notifyWorkItemCustom(
            @Nullable ObjectReferenceType assignee, CaseWorkItemType workItem,
            WorkItemEventCauseInformationType cause, CaseType aCase,
            @NotNull WorkItemNotificationActionType notificationAction,
            Task opTask, OperationResult result) {
        for (CaseEventCreationListener eventCreationListener : notificationEventCreationListeners) {
            eventCreationListener.onWorkItemCustomEvent(assignee, workItem, notificationAction, cause, aCase, opTask, result);
        }
    }

    public void registerNotificationEventCreationListener(@NotNull CaseEventCreationListener listener) {
        LOGGER.trace("Registering notification event creation listener {}", listener);
        notificationEventCreationListeners.add(listener);
    }

    public void send(FutureNotificationEvent eventSupplier, Task task, OperationResult result) {
        for (CaseEventCreationListener listener : notificationEventCreationListeners) {
            eventSupplier.send(listener, task, result);
        }
    }
}
