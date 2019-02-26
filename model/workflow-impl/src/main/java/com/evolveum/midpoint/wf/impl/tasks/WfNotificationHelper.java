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

package com.evolveum.midpoint.wf.impl.tasks;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import javax.xml.datatype.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
@Component
public class WfNotificationHelper {

	private static final Trace LOGGER = TraceManager.getTrace(WfNotificationHelper.class);

	private Set<ProcessListener> processListeners = ConcurrentHashMap.newKeySet();
	private Set<WorkItemListener> workItemListeners = ConcurrentHashMap.newKeySet();

	public void notifyProcessStart(CaseType aCase, Task opTask, OperationResult result) {
		for (ProcessListener processListener : processListeners) {
			processListener.onProcessInstanceStart(aCase, opTask, result);
		}
	}

	public void notifyProcessEnd(CaseType aCase, Task opTask, OperationResult result) {
		for (ProcessListener processListener : processListeners) {
			processListener.onProcessInstanceEnd(aCase, opTask, result);
		}
	}

	public void notifyWorkItemCreated(ObjectReferenceType originalAssigneeRef, CaseWorkItemType workItem,
			CaseType aCase, Task opTask, OperationResult result) {
		for (WorkItemListener workItemListener : workItemListeners) {
			workItemListener.onWorkItemCreation(originalAssigneeRef, workItem, aCase, opTask, result);
		}
	}

	public void notifyWorkItemDeleted(ObjectReferenceType assignee, CaseWorkItemType workItem,
			WorkItemOperationInfo operationInfo, WorkItemOperationSourceInfo sourceInfo,
			CaseType aCase, Task opTask, OperationResult result) {
		for (WorkItemListener workItemListener : workItemListeners) {
			workItemListener.onWorkItemDeletion(assignee, workItem, operationInfo, sourceInfo, aCase, opTask, result);
		}
	}

	public void notifyWorkItemAllocationChangeCurrentActors(CaseWorkItemType workItem,
			@NotNull WorkItemAllocationChangeOperationInfo operationInfo,
			WorkItemOperationSourceInfo sourceInfo, Duration timeBefore,
			CaseType aCase, Task opTask, OperationResult result) {
		for (WorkItemListener workItemListener : workItemListeners) {
			workItemListener.onWorkItemAllocationChangeCurrentActors(workItem, operationInfo, sourceInfo, timeBefore, aCase, opTask, result);
		}
	}

	public void notifyWorkItemAllocationChangeNewActors(CaseWorkItemType workItem,
			@NotNull WorkItemAllocationChangeOperationInfo operationInfo,
			@Nullable WorkItemOperationSourceInfo sourceInfo,
			CaseType aCase, Task opTask, OperationResult result) {
		for (WorkItemListener workItemListener : workItemListeners) {
			workItemListener.onWorkItemAllocationChangeNewActors(workItem, operationInfo, sourceInfo, aCase, opTask, result);
		}
	}

	public void notifyWorkItemCustom(@Nullable ObjectReferenceType assignee, CaseWorkItemType workItem,
			WorkItemEventCauseInformationType cause, CaseType aCase, Task opTask,
			@NotNull WorkItemNotificationActionType notificationAction,
			OperationResult result) {
		for (WorkItemListener workItemListener : workItemListeners) {
			workItemListener.onWorkItemCustomEvent(assignee, workItem, notificationAction, cause, aCase, opTask, result);
		}
	}

	public void registerProcessListener(ProcessListener processListener) {
		LOGGER.trace("Registering process listener {}", processListener);
		processListeners.add(processListener);
	}

	public void registerWorkItemListener(WorkItemListener workItemListener) {
		LOGGER.trace("Registering work item listener {}", workItemListener);
		workItemListeners.add(workItemListener);
	}

}
