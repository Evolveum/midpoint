/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.run;

import static com.evolveum.midpoint.repo.common.activity.ActivityRunResultStatus.PERMANENT_ERROR;
import static com.evolveum.midpoint.schema.result.OperationResultStatus.FATAL_ERROR;
import static com.evolveum.midpoint.schema.result.OperationResultStatus.IN_PROGRESS;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityRealizationStateType.IN_PROGRESS_DELEGATED;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityStateDefinition;
import com.evolveum.midpoint.repo.common.activity.run.state.OtherActivityState;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Activity run that delegates its work to a child task.
 *
 * This class manages starting the child task if needed and extracting its state upon completion or abortion
 * (and passing it further up).
 */
public final class DelegatingActivityRun<
        WD extends WorkDefinition,
        AH extends ActivityHandler<WD, AH>> extends AbstractActivityRun<WD, AH, DelegationWorkStateType> {

    private static final Trace LOGGER = TraceManager.getTrace(DelegatingActivityRun.class);

    @NotNull private final SubtaskHelper helper;

    public DelegatingActivityRun(@NotNull ActivityRunInstantiationContext<WD, AH> context) {
        super(context);
        helper = new SubtaskHelper(this);
        setInstanceReady();
    }

    @Override
    public @NotNull ActivityReportingCharacteristics createReportingCharacteristics() {
        return super.createReportingCharacteristics()
                .statisticsSupported(false)
                .progressSupported(false);
    }

    @Override
    protected ActivityStateDefinition determineActivityStateDefinition() {
        return ActivityStateDefinition.normal(DelegationWorkStateType.COMPLEX_TYPE);
    }

    /** State of the delegation determined when the processing starts. */
    private DelegationState delegationState;

    /**
     * Child task: either discovered in {@link #determineDelegationState(OperationResult)}
     * or created in {@link #delegate(OperationResult)}.
     */
    private Task childTask;

    /**
     * Activity state of this activity in the child task. Guaranteed to exist for
     *
     * - {@link DelegationState#DELEGATED_COMPLETE}
     * - {@link DelegationState#DELEGATED_ABORTED}
     */
    private OtherActivityState activityStateInChildTask;

    @Override
    protected @NotNull ActivityRunResult runInternal(OperationResult result)
            throws ActivityRunException, CommonException {

        assert !activityState.isComplete() && !activityState.isAborted(); // from the caller

        delegationState = determineDelegationState(result);

        return switch (delegationState) {
            case NOT_DELEGATED_YET -> {
                delegate(result);
                yield ActivityRunResult.waiting();
            }
            case DELEGATED_IN_PROGRESS -> ActivityRunResult.waiting();
            case DELEGATED_COMPLETE -> {
                assert activityStateInChildTask != null;
                yield ActivityRunResult.finished(activityStateInChildTask.getResultStatus());
            }
            case DELEGATED_ABORTED -> {
                assert activityStateInChildTask != null;
                yield ActivityRunResult.aborted(
                        activityStateInChildTask.getResultStatus(), activityStateInChildTask.getAbortingInformation());
            }
        };
    }

    private @NotNull DelegationState determineDelegationState(OperationResult result) throws ActivityRunException {
        ActivityRealizationStateType realizationState = activityState.getRealizationState();
        if (realizationState == null) {
            return DelegationState.NOT_DELEGATED_YET;
        } else if (realizationState == IN_PROGRESS_DELEGATED) {
            return determineDelegationStateFromChildTask(result);
        } else {
            // IN_PROGRESS_LOCAL and IN_PROGRESS_DISTRIBUTED are not expected here; other ones are excluded upstream
            throw new IllegalStateException(String.format("Unexpected realization state %s for activity '%s' in %s",
                    realizationState, getActivityPath(), getRunningTask()));
        }
    }

    private @NotNull DelegationState determineDelegationStateFromChildTask(OperationResult result)
            throws ActivityRunException {
        String childOid = getChildOid();
        try {
            childTask = getBeans().taskManager.getTaskPlain(childOid, result);
        } catch (ObjectNotFoundException e) {
            LOGGER.warn("Activity '{}' is marked as delegated but child task (OID {}) was not found. Will create one. In: {}",
                    getActivityPath(), childOid, getRunningTask());
            return DelegationState.NOT_DELEGATED_YET;
        } catch (Exception e) {
            throw new ActivityRunException(
                    "Child task (OID %s) for activity '%s' in %s could not be retrieved".formatted(
                            childOid, getActivityPath(), getRunningTask()),
                    FATAL_ERROR, PERMANENT_ERROR, e);
        }

        activityStateInChildTask = OtherActivityState.of(childTask, getActivityPath());
        var realizationStateInChildTask = activityStateInChildTask.getRealizationState();
        if (realizationStateInChildTask == null) {
            return DelegationState.DELEGATED_IN_PROGRESS;
        } else {
            return switch (realizationStateInChildTask) {
                case COMPLETE -> DelegationState.DELEGATED_COMPLETE;
                case ABORTED -> DelegationState.DELEGATED_ABORTED;
                case IN_PROGRESS_LOCAL, IN_PROGRESS_DISTRIBUTED, IN_PROGRESS_DELEGATED -> DelegationState.DELEGATED_IN_PROGRESS;
            };
        }
    }

    private @NotNull String getChildOid() throws ActivityRunException {
        ObjectReferenceType childRef = getTaskRef();
        String childOid = childRef != null ? childRef.getOid() : null;
        if (childOid == null) {
            // We are so harsh here to debug any issues. Later on we can switch this to returning NOT_DELEGATED_YET
            // (i.e. a kind of auto-healing).
            throw new ActivityRunException(
                    String.format("Activity '%s' is marked as delegated but no child task OID is present. In: %s",
                            getActivityPath(), getRunningTask()),
                    FATAL_ERROR, PERMANENT_ERROR, null);
        }
        return childOid;
    }

    private ObjectReferenceType getTaskRef() {
        return activityState.getWorkStateReferenceRealValue(DelegationWorkStateType.F_TASK_REF);
    }

    private void delegate(OperationResult result) throws ActivityRunException, SchemaException, ObjectNotFoundException {
        activityState.recordRunStart(startTimestamp);
        activityState.recordRealizationStart(startTimestamp);
        activityState.setResultStatus(IN_PROGRESS);

        // We want to have this written to the task before execution is switched to children
        activityState.flushPendingTaskModificationsChecked(result);

        try {
            childTask = createOrFindTheChild(result);

            helper.switchExecutionToChildren(List.of(childTask), result);

            setTaskRef();
            activityState.setRealizationState(IN_PROGRESS_DELEGATED); // We want to set this only after subtask is created
        } finally {
            noteEndTimestampIfNone();
            activityState.recordRunEnd(endTimestamp);
            activityState.flushPendingTaskModificationsChecked(result);
        }
    }

    @NotNull
    private Task createOrFindTheChild(OperationResult result)
            throws SchemaException, ActivityRunException, ObjectNotFoundException {
        List<? extends Task> children = helper.getRelevantChildren(result);
        if (children.isEmpty()) {
            return createSuspendedChildTask(result);
        } else if (children.size() == 1) {
            Task child = children.get(0);
            stateCheck(child.isClosed(), "Child %s is not closed; its state is %s", child, child.getExecutionState());
            // This is to allow the safe switch of root task to WAITING state
            getBeans().taskManager.markClosedTaskSuspended(child.getOid(), result);
            child.refresh(result);
            return child;
        } else {
            throw new IllegalStateException("There is more than one child: " + children);
        }
    }

    private Task createSuspendedChildTask(OperationResult result) throws ActivityRunException {
        try {
            RunningTask parent = getRunningTask();
            TaskType childToCreate = new TaskType();
            childToCreate.setName(PolyStringType.fromOrig(getChildTaskName(parent)));
            // group?
            childToCreate.setExecutionState(TaskExecutionStateType.SUSPENDED);
            childToCreate.setSchedulingState(TaskSchedulingStateType.SUSPENDED);
            childToCreate.setOwnerRef(CloneUtil.clone(parent.getOwnerRef()));
            childToCreate.setParent(parent.getTaskIdentifier());
            childToCreate.setExecutionEnvironment(CloneUtil.clone(parent.getExecutionEnvironment()));
            ActivityPath localRoot = getActivityPath();
            childToCreate.beginActivityState()
                    .localRoot(localRoot.toBean())
                    .taskRole(TaskRoleType.DELEGATE);

            LOGGER.debug("Creating activity subtask {} with local root {}", childToCreate.getName(), localRoot);
            String childOid = getBeans().taskManager.addTask(childToCreate.asPrismObject(), result);
            LOGGER.debug("Created activity subtask {}: {}", childToCreate.getName(), childOid);

            // TODO eliminate this extra read some day
            return getBeans().taskManager.getTaskPlain(childOid, result);
        } catch (Exception e) {
            throw new ActivityRunException("Couldn't create activity child task", FATAL_ERROR, PERMANENT_ERROR, e);
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
            activityState.setWorkStateItemRealValues(DelegationWorkStateType.F_TASK_REF, childTask.getSelfReference());
        } catch (SchemaException e) {
            throw new IllegalStateException("Unexpected schema exception: " + e.getMessage(), e);
        }
    }

    @Override
    protected void debugDumpExtra(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabelLn(sb, "Delegation state", delegationState, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "Child task", childTask, indent + 1);
    }

    private enum DelegationState {
        NOT_DELEGATED_YET,
        DELEGATED_IN_PROGRESS,
        DELEGATED_COMPLETE,
        DELEGATED_ABORTED
    }
}
