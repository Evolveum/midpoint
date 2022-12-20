/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run;

import static com.evolveum.midpoint.schema.result.OperationResultStatus.FATAL_ERROR;
import static com.evolveum.midpoint.schema.result.OperationResultStatus.IN_PROGRESS;
import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityRealizationStateType.IN_PROGRESS_DISTRIBUTED;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;
import com.evolveum.midpoint.repo.common.activity.run.distribution.WorkersReconciliation;
import com.evolveum.midpoint.repo.common.activity.run.distribution.WorkersReconciliationOptions;
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
public final class DistributingActivityRun<
        WD extends WorkDefinition,
        AH extends ActivityHandler<WD, AH>,
        WS extends AbstractActivityWorkStateType>
        extends AbstractActivityRun<WD, AH, WS> {

    private static final Trace LOGGER = TraceManager.getTrace(DistributingActivityRun.class);

    private static final ItemPath WORK_COMPLETE_PATH =
            ItemPath.create(ActivityStateType.F_BUCKETING, ActivityBucketingStateType.F_WORK_COMPLETE);

    private static final ItemPath BUCKETS_PROCESSING_ROLE_PATH =
            ItemPath.create(ActivityStateType.F_BUCKETING, ActivityBucketingStateType.F_BUCKETS_PROCESSING_ROLE);

    @NotNull private final SubtaskHelper helper;

    public DistributingActivityRun(@NotNull ActivityRunInstantiationContext<WD, AH> context) {
        super(context);
        helper = new SubtaskHelper(this);
        setInstanceReady();
    }

    @Override
    public @NotNull ActivityReportingCharacteristics createReportingCharacteristics() {
        return super.createReportingCharacteristics()
                .statisticsSupported(true) // Not sure about this.
                .progressSupported(false);
    }

    private DistributionState distributionState;

    @Override
    protected @NotNull ActivityRunResult runInternal(OperationResult result) throws ActivityRunException {
        distributionState = determineDistributionState();

        switch (distributionState) {
            case NOT_DISTRIBUTED_YET:
                distribute(result);
                return ActivityRunResult.waiting();
            case DISTRIBUTED:
                return ActivityRunResult.waiting();
            case COMPLETE:
                ActivityRunResult runResult = ActivityRunResult.finished(computeFinalStatus(result));
                getTreeStateOverview().recordDistributedActivityRealizationFinish(this, runResult, result);
                return runResult;
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

    private void distribute(OperationResult result) throws ActivityRunException {
        activityState.setItemRealValues(BUCKETS_PROCESSING_ROLE_PATH, BucketsProcessingRoleType.COORDINATOR);

        activityState.recordRunStart(startTimestamp);
        activityState.recordRealizationStart(startTimestamp);
        activityState.setResultStatus(IN_PROGRESS);

        // We want to have this written to the task before execution is switched to children
        activityState.flushPendingTaskModificationsChecked(result);

        try {
            // Currently there are no activities having workers with persistent state,
            // so we assume there are no workers left at this point.
            helper.checkNoRelevantSubtasksDoExist(result);

            onActivityRealizationStart(result);

            List<Task> children = createSuspendedChildren(result);
            helper.switchExecutionToChildren(children, result);

            activityState.setRealizationState(IN_PROGRESS_DISTRIBUTED); // We want to set this only after workers are created
            getTreeStateOverview().recordDistributedActivityRealizationStart(this, result);
        } finally {
            noteEndTimestampIfNone();
            activityState.recordRunEnd(endTimestamp);
            activityState.flushPendingTaskModificationsChecked(result);
        }
    }

    private List<Task> createSuspendedChildren(OperationResult result) throws ActivityRunException {
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
            throw new ActivityRunException("Couldn't create/update activity children (workers)",
                    FATAL_ERROR, PERMANENT_ERROR, e);
        }
    }

    @Override
    protected void debugDumpExtra(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabel(sb, "Distribution state", distributionState, indent + 1);
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
