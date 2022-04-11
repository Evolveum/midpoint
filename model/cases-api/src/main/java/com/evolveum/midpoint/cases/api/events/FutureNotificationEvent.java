/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.cases.api.events;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import javax.xml.datatype.Duration;

/**
 * Object that is able to produce given notification event (with the help of {@link CaseEventCreationListener})
 * when the {@link #send(CaseEventCreationListener, Task, OperationResult)} method is called.
 */
public abstract class FutureNotificationEvent implements DebugDumpable {

    /** Live current case object. */
    public final CaseType aCase;

    FutureNotificationEvent(CaseType aCase) {
        this.aCase = aCase.clone();
    }

    public static class CaseOpening extends FutureNotificationEvent {

        public CaseOpening(CaseType aCase) {
            super(aCase);
        }

        @Override
        public void send(CaseEventCreationListener listener, Task task, OperationResult result) {
            listener.onCaseOpening(aCase, task, result);
        }
    }

    public static class CaseClosing extends FutureNotificationEvent {

        public CaseClosing(CaseType aCase) {
            super(aCase);
        }

        @Override
        public void send(CaseEventCreationListener listener, Task task, OperationResult result) {
            listener.onCaseClosing(aCase, task, result);
        }
    }

    public abstract static class WorkItem<OI extends WorkItemOperationInfo> extends FutureNotificationEvent {

        /** Cloned work item. */
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
        public void send(CaseEventCreationListener listener, Task task, OperationResult result) {
            listener.onWorkItemCreation(assignee, workItem, aCase, task, result);
        }
    }

    public static class ItemClosing extends WorkItem<WorkItemOperationInfo> {

        public ItemClosing(
                CaseType aCase,
                CaseWorkItemType workItem,
                WorkItemOperationInfo operationInfo,
                WorkItemOperationSourceInfo sourceInfo,
                ObjectReferenceType assignee) {
            super(aCase, workItem, operationInfo, sourceInfo);
            this.assignee = assignee;
        }

        public final ObjectReferenceType assignee;

        @Override
        public void send(CaseEventCreationListener listener, Task task, OperationResult result) {
            listener.onWorkItemClosing(assignee, workItem, operationInfo, sourceInfo, aCase, task, result);
        }
    }

    public static class AllocationChangeCurrent extends WorkItem<WorkItemAllocationChangeOperationInfo> {

        public AllocationChangeCurrent(
                CaseType aCase,
                CaseWorkItemType workItem,
                WorkItemAllocationChangeOperationInfo operationInfo,
                WorkItemOperationSourceInfo sourceInfo,
                Duration timeBefore) {
            super(aCase, workItem, operationInfo, sourceInfo);
            this.timeBefore = timeBefore;
        }

        final Duration timeBefore;

        @Override
        public void send(CaseEventCreationListener listener, Task task, OperationResult result) {
            listener.onWorkItemAllocationChangeCurrentActors(workItem, operationInfo, sourceInfo, timeBefore, aCase, task, result);
        }
    }

    public static class AllocationChangeNew extends WorkItem<WorkItemAllocationChangeOperationInfo> {

        public AllocationChangeNew(
                CaseType aCase,
                CaseWorkItemType workItem,
                WorkItemAllocationChangeOperationInfo operationInfo,
                WorkItemOperationSourceInfo sourceInfo) {
            super(aCase, workItem, operationInfo, sourceInfo);
        }

        @Override
        public void send(CaseEventCreationListener listener, Task task, OperationResult result) {
            listener.onWorkItemAllocationChangeNewActors(workItem, operationInfo, sourceInfo, aCase, task, result);
        }
    }

    public abstract void send(CaseEventCreationListener listener, Task task, OperationResult result);

    @Override
    public String debugDump(int indent) {
        return DebugUtil.createIndentedStringBuilder(indent) + getClass().getSimpleName();
    }
}
