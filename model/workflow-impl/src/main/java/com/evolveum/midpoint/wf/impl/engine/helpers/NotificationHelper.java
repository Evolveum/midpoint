/*
 * Copyright (c) 2010-2019 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.wf.impl.engine.helpers;

import com.evolveum.midpoint.schema.result.OperationResult;
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

	private Set<WorkflowListener> workflowListeners = ConcurrentHashMap.newKeySet();

	public void sendPreparedNotifications(EngineInvocationContext ctx, OperationResult result) {
		for (DelayedNotification notification : ctx.pendingNotifications) {
			for (WorkflowListener listener : workflowListeners) {
				notification.send(listener, result);
			}
		}
	}

	// The following two methods are of "immediate notification" kind. They are an exception; usually we
	// prepare notifications first and send them only after the case modification succeeds.

	public void notifyWorkItemAllocationChangeCurrentActors(CaseWorkItemType workItem,
			@NotNull WorkItemAllocationChangeOperationInfo operationInfo,
			WorkItemOperationSourceInfo sourceInfo, Duration timeBefore,
			CaseType aCase, OperationResult result) {
		for (WorkflowListener workflowListener : workflowListeners) {
			workflowListener.onWorkItemAllocationChangeCurrentActors(workItem, operationInfo, sourceInfo, timeBefore, aCase, result);
		}
	}

	public void notifyWorkItemCustom(@Nullable ObjectReferenceType assignee, CaseWorkItemType workItem,
			WorkItemEventCauseInformationType cause, CaseType aCase,
			@NotNull WorkItemNotificationActionType notificationAction,
			OperationResult result) {
		for (WorkflowListener workflowListener : workflowListeners) {
			workflowListener.onWorkItemCustomEvent(assignee, workItem, notificationAction, cause, aCase, result);
		}
	}

	public void registerWorkItemListener(WorkflowListener workflowListener) {
		LOGGER.trace("Registering work item listener {}", workflowListener);
		workflowListeners.add(workflowListener);
	}

}
