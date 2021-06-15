/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.execution;

import static com.google.common.base.MoreObjects.firstNonNull;

import static com.evolveum.midpoint.schema.result.OperationResultStatus.FATAL_ERROR;
import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.OperationResultUtil;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class DistributingActivityExecution<
        WD extends WorkDefinition,
        AH extends ActivityHandler<WD, AH>> extends AbstractActivityExecution<WD, AH, DistributionWorkStateType> {

    private static final Trace LOGGER = TraceManager.getTrace(DistributingActivityExecution.class);

    private static final ItemPath WORK_COMPLETE_PATH =
            ItemPath.create(ActivityStateType.F_BUCKETING, ActivityBucketingStateType.F_WORK_COMPLETE);

    private static final ItemPath BUCKETS_PROCESSING_ROLE_PATH =
            ItemPath.create(ActivityStateType.F_BUCKETING, ActivityBucketingStateType.F_BUCKETS_PROCESSING_ROLE);

    @NotNull
    private final SubtaskHelper helper;

    public DistributingActivityExecution(@NotNull ExecutionInstantiationContext<WD, AH> context) {
        super(context);
        helper = new SubtaskHelper(this);
    }

    @Override
    protected @NotNull QName getWorkStateTypeName(@NotNull ExecutionInstantiationContext<WD, AH> context) {
        return DistributionWorkStateType.COMPLEX_TYPE;
    }

    private DistributionState distributionState;

    @Override
    protected @NotNull ActivityExecutionResult executeInternal(OperationResult result) throws ActivityExecutionException {
        distributionState = determineDistributionState();

        switch (distributionState) {
            case NOT_DISTRIBUTED_YET:
                distribute(result);
                return ActivityExecutionResult.waiting();
            case DISTRIBUTED:
                return ActivityExecutionResult.waiting();
            case COMPLETE:
                return ActivityExecutionResult.finished(computeFinalStatus(result));
            default:
                throw new AssertionError(distributionState);
        }
    }

    private @NotNull DistributionState determineDistributionState() {
        ActivityRealizationStateType realizationState = activityState.getRealizationState();
        if (realizationState == null) {
            return DistributionState.NOT_DISTRIBUTED_YET;
        } else if (realizationState == ActivityRealizationStateType.IN_PROGRESS_DISTRIBUTED) {
            if (isWorkComplete()) {
                return DistributionState.COMPLETE;
            } else {
                return DistributionState.DISTRIBUTED;
            }
        } else {
            throw new IllegalStateException(String.format("Unexpected realization state %s for activity '%s' in %s",
                    realizationState, getActivityPath(), getRunningTask()));
        }
    }

    private boolean isWorkComplete() {
        return Boolean.TRUE.equals(
                activityState.getPropertyRealValue(WORK_COMPLETE_PATH, Boolean.class));
    }

    private OperationResultStatus computeFinalStatus(OperationResult result) {
        try {
            List<? extends Task> children = helper.getRelevantChildren(result);
            Set<OperationResultStatus> statuses = children.stream()
                    .map(Task::getResultStatus)
                    .map(OperationResultStatus::parseStatusType)
                    .collect(Collectors.toSet());
            LOGGER.trace("Children statuses: {}", statuses);
            return OperationResultUtil.aggregateFinishedResults(statuses);
        } catch (CommonException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't compute final status for {}", e, getRunningTask());
            return FATAL_ERROR;
        }
    }

    private void distribute(OperationResult result) throws ActivityExecutionException {
        setProcessingRole(result);

        helper.deleteRelevantSubtasksIfPossible(result);

        List<TaskType> children = createSuspendedChildren(result);
        helper.switchExecutionToChildren(children, result);

        activityState.markInProgressDistributed(result);
    }

    private void setProcessingRole(OperationResult result) throws ActivityExecutionException {
        activityState.setItemRealValues(BUCKETS_PROCESSING_ROLE_PATH, BucketsProcessingRoleType.COORDINATOR);
        activityState.flushPendingModificationsChecked(result);
    }

    // TEMPORARY IMPLEMENTATION
    private List<TaskType> createSuspendedChildren(OperationResult result) throws ActivityExecutionException {
        try {
            int count = getWorkersCount();
            List<TaskType> children = new ArrayList<>();
            for (int i = 1; i <= count; i++) {
                children.add(createSuspendedChild(i, result));
            }
            return children;
        } catch (Exception e) {
            throw new ActivityExecutionException("Couldn't create activity children (workers)", FATAL_ERROR, PERMANENT_ERROR, e);
        }
    }

    @NotNull
    private TaskType createSuspendedChild(int i, OperationResult result) throws ObjectAlreadyExistsException, SchemaException {
        RunningTask parent = getRunningTask();
        TaskType child = new TaskType(getPrismContext());
        child.setName(PolyStringType.fromOrig(getChildTaskName(parent, String.valueOf(i))));
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
                .role(ActivityExecutionRoleType.WORKER)
                .beginActivity()
                    .beginBucketing()
                        .bucketsProcessingRole(BucketsProcessingRoleType.WORKER)
                        .scavenger(i == 1 ? Boolean.TRUE : null);

        LOGGER.info("Creating activity subtask {} with local root {}", child.getName(), localRoot);
        String childOid = getBeans().taskManager.addTask(child.asPrismObject(), result);
        LOGGER.debug("Created activity subtask {}: {}", child.getName(), childOid);
        return child;
    }

    private int getWorkersCount() {
        int count;
        List<WorkerTasksPerNodeConfigurationType> perNode =
                activity.getDefinition().getDistributionDefinition().getWorkers().getWorkersPerNode();
        if (perNode.isEmpty()) {
            count = 1;
        } else if (perNode.size() == 1) {
            count = firstNonNull(perNode.get(0).getCount(), 1);
        } else {
            throw new UnsupportedOperationException("Only single workersPerNode element is supported for now.");
        }
        return count;
    }

    private @NotNull String getChildTaskName(RunningTask parent, String identification) {
        if (activity.isRoot()) {
            return String.format("Worker %s for root activity in %s", identification, parent.getRootTaskOid());
        } else {
            return String.format("Worker %s for activity '%s' in %s", identification, activity.getPath(), parent.getRootTaskOid());
        }
    }

    @Override
    protected void debugDumpExtra(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabel(sb, "Distribution state", distributionState, indent + 1);
    }

    @Override
    public boolean supportsStatistics() {
        return true;
    }

    private enum DistributionState {
        NOT_DISTRIBUTED_YET,
        DISTRIBUTED,
        COMPLETE
    }
}
