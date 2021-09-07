/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.execution;

import static com.evolveum.midpoint.schema.result.OperationResultStatus.FATAL_ERROR;
import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;
import com.evolveum.midpoint.repo.common.task.work.workers.WorkersReconciliation;
import com.evolveum.midpoint.repo.common.task.work.workers.WorkersReconciliationOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.OperationResultUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * An activity that distributes (usually bucketed) activity to a set of worker tasks.
 * What is interesting is that this activity can maintain a work state of the type belonging to activity being distributed.
 *
 * @param <WD> work definition
 * @param <AH> activity handler
 */
public class DistributingActivityExecution<
        WD extends WorkDefinition,
        AH extends ActivityHandler<WD, AH>,
        WS extends AbstractActivityWorkStateType>
        extends AbstractActivityExecution<WD, AH, WS> {

    private static final Trace LOGGER = TraceManager.getTrace(DistributingActivityExecution.class);

    private static final ItemPath WORK_COMPLETE_PATH =
            ItemPath.create(ActivityStateType.F_BUCKETING, ActivityBucketingStateType.F_WORK_COMPLETE);

    private static final ItemPath BUCKETS_PROCESSING_ROLE_PATH =
            ItemPath.create(ActivityStateType.F_BUCKETING, ActivityBucketingStateType.F_BUCKETS_PROCESSING_ROLE);

    @NotNull private final SubtaskHelper helper;

    public DistributingActivityExecution(@NotNull ExecutionInstantiationContext<WD, AH> context) {
        super(context);
        helper = new SubtaskHelper(this);
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

        // Currently there are no activities having workers with persistent state,
        // so we assume there are no workers left at this point.
        helper.checkNoRelevantSubtasksDoExist(result);

        List<Task> children = createSuspendedChildren(result);
        helper.switchExecutionToChildren(children, result);

        activityState.markInProgressDistributed(result);
    }

    private void setProcessingRole(OperationResult result) throws ActivityExecutionException {
        activityState.setItemRealValues(BUCKETS_PROCESSING_ROLE_PATH, BucketsProcessingRoleType.COORDINATOR);
        activityState.flushPendingModificationsChecked(result);
    }

    private List<Task> createSuspendedChildren(OperationResult result) throws ActivityExecutionException {
        try {
            WorkersReconciliationOptions options = new WorkersReconciliationOptions();
            options.setCreateSuspended(true);
            options.setDontCloseWorkersWhenWorkDone(true);
            WorkersReconciliation workersReconciliation = new WorkersReconciliation(
                    getRunningTask().getRootTask(),
                    getRunningTask(),
                    getActivityPath(),
                    options,
                    getBeans());
            workersReconciliation.execute(result);
            return workersReconciliation.getCurrentWorkers(result);
        } catch (CommonException e) {
            throw new ActivityExecutionException("Couldn't create/update activity children (workers)",
                    FATAL_ERROR, PERMANENT_ERROR, e);
        }
    }

    @Override
    protected void debugDumpExtra(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabel(sb, "Distribution state", distributionState, indent + 1);
    }

    @Override
    public boolean doesSupportStatistics() {
        return true;
    }

    @Override
    public boolean doesSupportSynchronizationStatistics() {
        return false;
    }

    @Override
    public boolean doesSupportActionsExecuted() {
        return false;
    }

    private enum DistributionState {

        /** The distribution (i.e. subtasks creation) didn't take place yet. */
        NOT_DISTRIBUTED_YET,

        /** The activity realization is already distributed, but not complete. */
        DISTRIBUTED,

        /** The activity realization is complete. */
        COMPLETE
    }
}
