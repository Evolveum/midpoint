/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.execution;

import static com.evolveum.midpoint.schema.result.OperationResultStatus.FATAL_ERROR;
import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR;

import com.evolveum.midpoint.repo.common.activity.ActivityStateDefinition;

import com.evolveum.midpoint.task.api.Task;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import java.util.List;

public class DelegatingActivityExecution<
        WD extends WorkDefinition,
        AH extends ActivityHandler<WD, AH>> extends AbstractActivityExecution<WD, AH, DelegationWorkStateType> {

    private static final Trace LOGGER = TraceManager.getTrace(DelegatingActivityExecution.class);

    @NotNull private final SubtaskHelper helper;

    public DelegatingActivityExecution(@NotNull ExecutionInstantiationContext<WD, AH> context) {
        super(context);
        helper = new SubtaskHelper(this);
    }

    @Override
    protected ActivityStateDefinition<DelegationWorkStateType> determineActivityStateDefinition() {
        return ActivityStateDefinition.normal(DelegationWorkStateType.COMPLEX_TYPE);
    }

    private DelegationState delegationState;

    /**
     * Child task: either discovered in {@link #determineDelegationState(OperationResult)}
     * or created in {@link #delegate(OperationResult)}.
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

    private void delegate(OperationResult result) throws ActivityExecutionException {
        helper.deleteRelevantSubtasksIfPossible(result);

        Task child = createSuspendedChildTask(result);

        helper.switchExecutionToChildren(List.of(child), result);

        setTaskRef();
        activityState.markInProgressDelegated(result);
    }

    private Task createSuspendedChildTask(OperationResult result) throws ActivityExecutionException {
        try {
            RunningTask parent = getRunningTask();
            TaskType childToCreate = new TaskType(getPrismContext());
            childToCreate.setName(PolyStringType.fromOrig(getChildTaskName(parent)));
            // group?
            childToCreate.setExecutionStatus(TaskExecutionStateType.SUSPENDED);
            childToCreate.setSchedulingState(TaskSchedulingStateType.SUSPENDED);
            childToCreate.setOwnerRef(CloneUtil.clone(parent.getOwnerRef()));
            childToCreate.setRecurrence(TaskRecurrenceType.SINGLE);
            childToCreate.setParent(parent.getTaskIdentifier());
            childToCreate.setExecutionEnvironment(CloneUtil.clone(parent.getExecutionEnvironment()));
            ActivityPath localRoot = getActivityPath();
            childToCreate.beginActivityState()
                    .localRoot(localRoot.toBean())
                    .localRootActivityExecutionRole(ActivityExecutionRoleType.DELEGATE);

            LOGGER.debug("Creating activity subtask {} with local root {}", childToCreate.getName(), localRoot);
            String childOid = getBeans().taskManager.addTask(childToCreate.asPrismObject(), result);
            LOGGER.debug("Created activity subtask {}: {}", childToCreate.getName(), childOid);

            Task child = getBeans().taskManager.getTaskPlain(childOid, result); // TODO eliminate this extra read some day
            childTask = child.getRawTaskObjectClonedIfNecessary().asObjectable();
            return child;
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

    private void setTaskRef() {
        try {
            ObjectReferenceType taskRef = ObjectTypeUtil.createObjectRef(childTask, getPrismContext());
            activityState.setWorkStateItemRealValues(DelegationWorkStateType.F_TASK_REF, taskRef);
        } catch (SchemaException e) {
            throw new IllegalStateException("Unexpected schema exception: " + e.getMessage(), e);
        }
    }

    @Override
    protected void debugDumpExtra(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabelLn(sb, "Delegation state", delegationState, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "Child task", childTask, indent + 1);
    }

    @Override
    public boolean supportsStatistics() {
        return false;
    }

    private enum DelegationState {
        NOT_DELEGATED_YET,
        DELEGATED_IN_PROGRESS,
        DELEGATED_COMPLETE
    }
}
