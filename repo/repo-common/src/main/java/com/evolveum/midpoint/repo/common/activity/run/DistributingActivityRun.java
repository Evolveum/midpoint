/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.run;

import static com.evolveum.midpoint.schema.result.OperationResultStatus.FATAL_ERROR;
import static com.evolveum.midpoint.schema.result.OperationResultStatus.IN_PROGRESS;
import static com.evolveum.midpoint.repo.common.activity.ActivityRunResultStatus.PERMANENT_ERROR;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityRealizationStateType.IN_PROGRESS_DISTRIBUTED;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.evolveum.midpoint.repo.common.activity.run.state.OtherActivityState;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.NotNull;

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

    private DistributionAction distributionAction;

    @Override
    protected @NotNull ActivityRunResult runInternal(OperationResult result) throws ActivityRunException, CommonException {
        distributionAction = determineDistributionState();

        return switch (distributionAction) {
            case CREATE_OR_REVIVE_WORKERS -> {
                createOrReviveWorkers(result);
                yield ActivityRunResult.waiting();
            }
            case WAIT -> ActivityRunResult.waiting();
            case FINISH -> {
                ActivityRunResult runResult = ActivityRunResult.finished(computeFinalStatus(result));
                getTreeStateOverview().recordDistributedActivityRealizationFinish(this, runResult, result);
                yield runResult;
            }
            case ABORT -> computeAbortResult(result); // TODO record this into task tree state overview
        };
    }

    private ActivityRunResult computeAbortResult(OperationResult result) throws SchemaException, ObjectNotFoundException {
        var abortingWorkerRef = Objects.requireNonNull(activityState.getAbortingWorkerRef()); // because we are aborting
        var abortingWorker = getBeans().taskManager.getTaskPlain(abortingWorkerRef.getOid(), result);
        var workerState = OtherActivityState.of(abortingWorker, getActivityPath());
        stateCheck(
                workerState.isAborted(),
                "Aborting worker is not aborted: '%s' in %s", getActivityPath(), getRunningTask());
        return ActivityRunResult.aborted(workerState.getResultStatus(), workerState.getAbortingInformation());
    }

    private @NotNull DistributionAction determineDistributionState() {
        ActivityRealizationStateType realizationState = activityState.getRealizationState();
        if (realizationState == null) {
            LOGGER.trace("My realization state is null => CREATE_OR_REVIVE_WORKERS");
            return DistributionAction.CREATE_OR_REVIVE_WORKERS;
        } else if (realizationState == ActivityRealizationStateType.IN_PROGRESS_DISTRIBUTED) {
            if (activityState.isBucketedWorkComplete()) {
                LOGGER.trace("My realization state is IN_PROGRESS_DISTRIBUTED and the bucketed work is complete => FINISH");
                return DistributionAction.FINISH;
            } else if (activityState.isBucketedWorkAborted()) {
                LOGGER.trace("My realization state is IN_PROGRESS_DISTRIBUTED and the bucketed work is aborted => ABORT");
                return DistributionAction.ABORT;
            } else {
                LOGGER.trace("My realization state is IN_PROGRESS_DISTRIBUTED and the bucketed work is in progress => WAIT");
                return DistributionAction.WAIT;
            }
        } else {
            throw new IllegalStateException(String.format("Unexpected realization state %s for activity '%s' in %s",
                    realizationState, getActivityPath(), getRunningTask()));
        }
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

    private void createOrReviveWorkers(OperationResult result) throws ActivityRunException, SchemaException {
        activityState.setBucketProcessingRole(BucketsProcessingRoleType.COORDINATOR);

        activityState.recordRunStart(startTimestamp);
        activityState.recordRealizationStart(startTimestamp);
        activityState.setResultStatus(IN_PROGRESS);

        // We want to have this written to the task before execution is switched to children
        activityState.flushPendingTaskModificationsChecked(result);

        try {
            // Currently there are no activities having workers with persistent state, so it's safe to delete all workers.
            // Moreover, in standard cases (i.e. no abortions) there should be no workers left at this point, because
            // they should be deleted when purging activity state at the start of activity realization.
            helper.deleteRemainingWorkers(result);

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
        DebugUtil.debugDumpWithLabel(sb, "Distribution action", distributionAction, indent + 1);
    }

    private enum DistributionAction {

        /** The distribution (i.e. subtasks creation) didn't take place yet. Let's make the workers run! */
        CREATE_OR_REVIVE_WORKERS,

        /** The activity realization is already distributed, but not complete. Let's wait. */
        WAIT,

        /** Let's signal that this activity has finished. */
        FINISH,

        /** Report this activity as aborted. */
        ABORT
    }
}
