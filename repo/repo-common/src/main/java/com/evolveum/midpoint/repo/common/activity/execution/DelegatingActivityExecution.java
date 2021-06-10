/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.execution;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.schema.result.OperationResultStatus.FATAL_ERROR;
import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR;

public class DelegatingActivityExecution<
        WD extends WorkDefinition,
        AH extends ActivityHandler<WD, AH>> extends AbstractActivityExecution<WD, AH, DelegationWorkStateType> {

    private static final Trace LOGGER = TraceManager.getTrace(DelegatingActivityExecution.class);

    public DelegatingActivityExecution(@NotNull ExecutionInstantiationContext<WD, AH> context) {
        super(context);
    }

    @Override
    protected @NotNull QName getWorkStateTypeName(@NotNull ExecutionInstantiationContext<WD, AH> context) {
        return DelegationWorkStateType.COMPLEX_TYPE;
    }

    private DelegationState delegationState;

    /**
     * Non-null for {@link DelegationState#DELEGATED_COMPLETE} and {@link DelegationState#DELEGATED_IN_PROGRESS}
     * and when child task was created during delegation process.
     */
    private TaskType childTask;

    @Override
    protected @NotNull ActivityExecutionResult executeInternal(OperationResult result) throws ActivityExecutionException {
        delegationState = determineDelegationState(result);

        switch (delegationState) {
            case NOT_DELEGATED_YET:
                delegate(result);
                return ActivityExecutionResult.waiting();
            case DELEGATED_IN_PROGRESS:
                return ActivityExecutionResult.waiting();
            case DELEGATED_COMPLETE:
                assert childTask != null;
                return ActivityExecutionResult.finished(childTask.getResultStatus());
            default:
                throw new AssertionError(delegationState);
        }
    }

    private @NotNull DelegationState determineDelegationState(OperationResult result) throws ActivityExecutionException {
        ActivityRealizationStateType realizationState = activityState.getRealizationState();
        if (realizationState == null) {
            return DelegationState.NOT_DELEGATED_YET;
        } else if (realizationState == ActivityRealizationStateType.IN_PROGRESS_DELEGATED) {
            return determineDelegationStateFromChildTask(result);
        } else {
            throw new IllegalStateException(String.format("Unexpected realization state %s for activity '%s' in %s",
                    realizationState, getActivityPath(), getRunningTask()));
        }
    }

    private @NotNull DelegationState determineDelegationStateFromChildTask(OperationResult result)
            throws ActivityExecutionException {
        ObjectReferenceType childRef = getTaskRef();
        String childOid = childRef != null ? childRef.getOid() : null;
        if (childOid == null) {
            // We are so harsh here to debug any issues. Later on we can switch this to returning NOT_DELEGATED_YET
            // (i.e. a kind of auto-healing).
            throw new ActivityExecutionException(
                    String.format("Activity '%s' is marked as delegated but no child task OID is present. Will create one. In: %s",
                            getActivityPath(), getRunningTask()),
                    FATAL_ERROR, PERMANENT_ERROR, null);
        }

        try {
            childTask = getBeans().taskManager
                    .getTaskPlain(childOid, result)
                    .getUpdatedTaskObject()
                    .asObjectable();
        } catch (ObjectNotFoundException e) {
            LOGGER.warn("Activity '{}' is marked as delegated but child task (OID {}) was not found. Will create one. In: {}",
                    getActivityPath(), childOid, getRunningTask());
            return DelegationState.NOT_DELEGATED_YET;
        } catch (Exception e) {
            throw new ActivityExecutionException(String.format("Child task (OID %s) for activity '%s' in %s could "
                    + "not be retrieved", childOid, getActivityPath(), getRunningTask()), FATAL_ERROR, PERMANENT_ERROR, e);
        }

        TaskExecutionStateType childStatus = childTask.getExecutionStatus();
        if (childStatus == TaskExecutionStateType.CLOSED) {
            LOGGER.debug("Child task {} is closed, considering delegated action to be complete", childTask);
            return DelegationState.DELEGATED_COMPLETE;
        } else {
            LOGGER.debug("Child task {} is not closed ({}), considering delegated action to be in progress",
                    childStatus, childTask);
            return DelegationState.DELEGATED_IN_PROGRESS;
        }
    }

    private ObjectReferenceType getTaskRef() {
        return activityState.getWorkStateReferenceRealValue(DelegationWorkStateType.F_TASK_REF);
    }

    private void setTaskRef(ObjectReferenceType taskRef) {
        try {
            activityState.setWorkStateItemRealValues(DelegationWorkStateType.F_TASK_REF, taskRef);
        } catch (SchemaException e) {
            throw new IllegalStateException("Unexpected schema exception: " + e.getMessage(), e);
        }
    }

    private void delegate(OperationResult result) throws ActivityExecutionException {
        deleteSubtasksIfPossible(result);

        ObjectReferenceType childRef = createSuspendedChildTask(result);
        switchExecutionToChild(childRef, result);

        setTaskRef(childRef);
        activityState.markInProgressDelegated(childRef, result);
    }

    private void deleteSubtasksIfPossible(OperationResult result) throws ActivityExecutionException {
        LOGGER.debug("Going to delete subtasks, if there are any, and if they are closed");
        try {
            List<? extends Task> relevantChildren = getRunningTask().listSubtasks(true, result).stream()
                    .filter(this::isRelevantWorker)
                    .collect(Collectors.toList());
            LOGGER.debug("Found {} relevant workers: {}", relevantChildren.size(), relevantChildren);
            List<? extends Task> notClosed = relevantChildren.stream()
                    .filter(t -> !t.isClosed())
                    .collect(Collectors.toList());
            if (!notClosed.isEmpty()) {
                // The error may be permanent or transient. But reporting it as permanent is more safe, as it causes
                // the parent task to always suspend, catching the attention of the administrators.
                throw new ActivityExecutionException("Couldn't (re)create activity subtask because there are existing one(s) "
                        + "that are not closed: " + notClosed, FATAL_ERROR, PERMANENT_ERROR);
            }
            for (Task worker : relevantChildren) {
                getBeans().taskManager.deleteTaskTree(worker.getOid(), result);
            }
        } catch (Exception e) {
            throw new ActivityExecutionException("Couldn't delete activity subtask(s)", FATAL_ERROR, PERMANENT_ERROR, e);
        }
    }

    private boolean isRelevantWorker(Task worker) {
        return getActivityPath().equalsBean(worker.getWorkState().getLocalRoot());
    }

    private ObjectReferenceType createSuspendedChildTask(OperationResult result) throws ActivityExecutionException {
        try {
            RunningTask parent = getRunningTask();
            TaskType child = new TaskType(getPrismContext());
            child.setName(PolyStringType.fromOrig(getChildTaskName(parent)));
            // group?
            child.setExecutionStatus(TaskExecutionStateType.SUSPENDED);
            child.setSchedulingState(TaskSchedulingStateType.SUSPENDED);
            child.setOwnerRef(CloneUtil.clone(parent.getOwnerRef()));
            child.setRecurrence(TaskRecurrenceType.SINGLE);
            child.setParent(parent.getTaskIdentifier());
            child.setExecutionEnvironment(CloneUtil.clone(parent.getExecutionEnvironment()));
            ActivityPath localRoot = getActivityPath();
            child.beginActivityState()
                    .localRoot(localRoot.toBean())
                    .role(ActivityExecutionRoleType.DELEGATE);

            LOGGER.info("Creating activity subtask {} with local root {}", child.getName(), localRoot);
            String childOid = getBeans().taskManager.addTask(child.asPrismObject(), result);
            LOGGER.debug("Created activity subtask {}: {}", child.getName(), childOid);

            return ObjectTypeUtil.createObjectRef(childOid, ObjectTypes.TASK);
        } catch (Exception e) {
            throw new ActivityExecutionException("Couldn't create activity child task", FATAL_ERROR, PERMANENT_ERROR, e);
        }
    }

    private @NotNull String getChildTaskName(RunningTask parent) {
        if (activity.isRoot()) {
            return String.format("Delegated root activity in %s", parent.getRootTaskOid());
        } else {
            return String.format("Delegated activity '%s' in %s", activity.getPath(), parent.getRootTaskOid());
        }
    }

    private PrismContext getPrismContext() {
        return getBeans().prismContext;
    }

    private void switchExecutionToChild(ObjectReferenceType childRef, OperationResult result) throws ActivityExecutionException {
        try {
            RunningTask runningTask = getRunningTask();
            runningTask.makeWaitingForOtherTasks(TaskUnpauseActionType.EXECUTE_IMMEDIATELY);
            runningTask.flushPendingModifications(result);
            getBeans().taskManager.resumeTask(childRef.getOid(), result);
            LOGGER.debug("Switched execution to child {}", childRef.getOid());
        } catch (SchemaException | ObjectNotFoundException | ObjectAlreadyExistsException e) {
            throw new ActivityExecutionException("Couldn't switch execution to activity subtask",
                    FATAL_ERROR, PERMANENT_ERROR, e);
        }
    }

    @Override
    protected void debugDumpExtra(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabelLn(sb, "Delegation state", delegationState, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "Child task ref", childTask, indent + 1);
    }

    private enum DelegationState {
        NOT_DELEGATED_YET,
        DELEGATED_IN_PROGRESS,
        DELEGATED_COMPLETE
    }
}
