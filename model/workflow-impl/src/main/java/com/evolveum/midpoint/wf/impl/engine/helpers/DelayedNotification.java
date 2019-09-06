/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.engine.helpers;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.wf.api.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import javax.xml.datatype.Duration;

/**
 *
 */

public abstract class DelayedNotification {

	public final CaseType aCase;

	DelayedNotification(CaseType aCase) {
		this.aCase = aCase.clone();
	}

	public static class ProcessStart extends DelayedNotification {
		public ProcessStart(CaseType aCase) {
			super(aCase);
		}

		@Override
		public void send(WorkflowListener listener, OperationResult result) {
			listener.onProcessInstanceStart(aCase, result);
		}
	}

	public static class ProcessEnd extends DelayedNotification {
		public ProcessEnd(CaseType aCase) {
			super(aCase);
		}

		@Override
		public void send(WorkflowListener listener, OperationResult result) {
			listener.onProcessInstanceEnd(aCase, result);
		}
	}

	public abstract static class WorkItem<OI extends WorkItemOperationInfo> extends DelayedNotification {
		final CaseWorkItemType workItem;
		final OI operationInfo;
		final WorkItemOperationSourceInfo sourceInfo;

		WorkItem(CaseType aCase, CaseWorkItemType workItem, OI operationInfo, WorkItemOperationSourceInfo sourceInfo) {
			super(aCase);
			this.workItem = workItem.clone();
			this.operationInfo = operationInfo;
			this.sourceInfo = sourceInfo;
		}
	}

	public static class ItemCreation extends WorkItem<WorkItemOperationInfo> {
		public ItemCreation(CaseType aCase, CaseWorkItemType workItem, WorkItemOperationInfo operationInfo,
				WorkItemOperationSourceInfo sourceInfo, ObjectReferenceType assignee) {
			super(aCase, workItem, operationInfo, sourceInfo);
			this.assignee = assignee;
		}

		public final ObjectReferenceType assignee;

		@Override
		public void send(WorkflowListener listener, OperationResult result) {
			listener.onWorkItemCreation(assignee, workItem, aCase, result);
		}
	}

	public static class ItemDeletion extends WorkItem<WorkItemOperationInfo> {
		public ItemDeletion(CaseType aCase, CaseWorkItemType workItem, WorkItemOperationInfo operationInfo,
				WorkItemOperationSourceInfo sourceInfo, ObjectReferenceType assignee) {
			super(aCase, workItem, operationInfo, sourceInfo);
			this.assignee = assignee;
		}

		public final ObjectReferenceType assignee;

		@Override
		public void send(WorkflowListener listener, OperationResult result) {
			listener.onWorkItemDeletion(assignee, workItem, operationInfo, sourceInfo, aCase, result);
		}
	}

	public static class ItemCustom extends WorkItem<WorkItemOperationInfo> {
		public ItemCustom(CaseType aCase, CaseWorkItemType workItem, WorkItemOperationInfo operationInfo,
				WorkItemOperationSourceInfo sourceInfo, ObjectReferenceType assignee,
				WorkItemNotificationActionType notificationAction,
				WorkItemEventCauseInformationType cause) {
			super(aCase, workItem, operationInfo, sourceInfo);
			this.assignee = assignee;
			this.notificationAction = notificationAction;
			this.cause = cause;
		}

		public final ObjectReferenceType assignee;
		public final WorkItemNotificationActionType notificationAction;
		public final WorkItemEventCauseInformationType cause;

		@Override
		public void send(WorkflowListener listener, OperationResult result) {
			listener.onWorkItemCustomEvent(assignee, workItem, notificationAction, cause, aCase, result);
		}
	}

	public static class AllocationChangeCurrent extends WorkItem<WorkItemAllocationChangeOperationInfo> {
		public AllocationChangeCurrent(CaseType aCase, CaseWorkItemType workItem,
				WorkItemAllocationChangeOperationInfo operationInfo, WorkItemOperationSourceInfo sourceInfo, Duration timeBefore) {
			super(aCase, workItem, operationInfo, sourceInfo);
			this.timeBefore = timeBefore;
		}

		public final Duration timeBefore;

		@Override
		public void send(WorkflowListener listener, OperationResult result) {
			listener.onWorkItemAllocationChangeCurrentActors(workItem, operationInfo, sourceInfo, timeBefore, aCase, result);
		}
	}

	public static class AllocationChangeNew extends WorkItem<WorkItemAllocationChangeOperationInfo> {
		public AllocationChangeNew(CaseType aCase, CaseWorkItemType workItem,
				WorkItemAllocationChangeOperationInfo operationInfo, WorkItemOperationSourceInfo sourceInfo) {
			super(aCase, workItem, operationInfo, sourceInfo);
		}

		@Override
		public void send(WorkflowListener listener, OperationResult result) {
			listener.onWorkItemAllocationChangeNewActors(workItem, operationInfo, sourceInfo, aCase, result);
		}
	}

	public abstract void send(WorkflowListener listener, OperationResult result);
}
