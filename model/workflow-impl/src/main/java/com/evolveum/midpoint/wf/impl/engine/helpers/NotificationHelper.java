/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.engine.helpers;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.WorkItemAllocationChangeOperationInfo;
import com.evolveum.midpoint.wf.api.WorkItemOperationSourceInfo;
import com.evolveum.midpoint.wf.api.WorkflowListener;
import com.evolveum.midpoint.wf.impl.engine.EngineInvocationContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import javax.xml.datatype.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 *  Helps with notification activities.
 */
@Component
public class NotificationHelper {

	private static final Trace LOGGER = TraceManager.getTrace(NotificationHelper.class);

	private static final String OP_SEND_PREPARED_NOTIFICATIONS = NotificationHelper.class.getName() + ".sendPreparedNotifications";

	private Set<WorkflowListener> workflowListeners = ConcurrentHashMap.newKeySet();

	public void sendPreparedNotifications(EngineInvocationContext ctx, OperationResult parentResult) {
		OperationResult result = parentResult.subresult(OP_SEND_PREPARED_NOTIFICATIONS)
				.setMinor()
				.build();
		try {
			for (DelayedNotification notification : ctx.pendingNotifications) {
				for (WorkflowListener listener : workflowListeners) {
					notification.send(listener, ctx.getTask(), result);
				}
			}
		} catch (Throwable t) {
			result.recordFatalError(t);
			throw t;
		} finally {
			result.computeStatusIfUnknown();
		}
	}

	// The following two methods are of "immediate notification" kind. They are an exception; usually we
	// prepare notifications first and send them only after the case modification succeeds.

	public void notifyWorkItemAllocationChangeCurrentActors(CaseWorkItemType workItem,
			@NotNull WorkItemAllocationChangeOperationInfo operationInfo,
			WorkItemOperationSourceInfo sourceInfo, Duration timeBefore,
			CaseType aCase, RunningTask opTask, OperationResult result) {
		for (WorkflowListener workflowListener : workflowListeners) {
			workflowListener.onWorkItemAllocationChangeCurrentActors(workItem, operationInfo, sourceInfo, timeBefore, aCase,
					opTask, result);
		}
	}

	public void notifyWorkItemCustom(@Nullable ObjectReferenceType assignee, CaseWorkItemType workItem,
			WorkItemEventCauseInformationType cause, CaseType aCase,
			@NotNull WorkItemNotificationActionType notificationAction,
			Task opTask, OperationResult result) {
		for (WorkflowListener workflowListener : workflowListeners) {
			workflowListener.onWorkItemCustomEvent(assignee, workItem, notificationAction, cause, aCase, opTask, result);
		}
	}

	public void registerWorkItemListener(WorkflowListener workflowListener) {
		LOGGER.trace("Registering work item listener {}", workflowListener);
		workflowListeners.add(workflowListener);
	}

}
