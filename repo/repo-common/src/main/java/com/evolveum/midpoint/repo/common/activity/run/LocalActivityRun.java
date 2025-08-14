/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.common.activity.ActivityInterruptedException;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityState;
import com.evolveum.midpoint.schema.TaskExecutionMode;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.SimulationTransaction;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractActivityWorkStateType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.datatype.XMLGregorianCalendar;

import java.util.Objects;

import static com.evolveum.midpoint.schema.result.OperationResultStatus.IN_PROGRESS;
import static com.evolveum.midpoint.schema.result.OperationResultStatus.UNKNOWN;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityRealizationStateType.IN_PROGRESS_LOCAL;

import static java.util.Objects.requireNonNull;

/**
 * The "real" run of an activity - i.e. not a delegation nor a distribution.
 *
 * Responsibilities at this level of abstraction:
 *
 * 1. records run start/stop + item progress in the tree state overview,
 * 2. records run start/stop in the item processing statistics (run records),
 * 3. updates progress information (clears uncommitted on start).
 */
public abstract class LocalActivityRun<
        WD extends WorkDefinition,
        AH extends ActivityHandler<WD, AH>,
        BS extends AbstractActivityWorkStateType> extends AbstractActivityRun<WD, AH, BS> {

    private static final Trace LOGGER = TraceManager.getTrace(LocalActivityRun.class);

    private static final String OP_RUN_LOCALLY = LocalActivityRun.class.getName() + ".runLocally";

    private static final long DEFAULT_TREE_PROGRESS_UPDATE_INTERVAL_FOR_STANDALONE = 9000;
    private static final long DEFAULT_TREE_PROGRESS_UPDATE_INTERVAL_FOR_WORKERS = 60000;

    @NotNull private OperationResultStatus currentResultStatus = UNKNOWN;

    protected LocalActivityRun(@NotNull ActivityRunInstantiationContext<WD, AH> context) {
        super(context);
    }

    @Override
    public @NotNull ActivityReportingCharacteristics createReportingCharacteristics() {
        return super.createReportingCharacteristics()
                .runRecordsSupported(activityStateDefinition.isSingleRealization());
    }

    @Override
    protected @NotNull ActivityRunResult runInternal(OperationResult result)
            throws ActivityRunException {

        initializeCurrentResultStatusOnStart();
        updateStateOnRunStart(result);

        RunningTask runningTask = getRunningTask();
        TaskExecutionMode oldExecutionMode = runningTask.getExecutionMode();

        ActivityRunResult runResult;
        OperationResult localResult = result.createSubresult(OP_RUN_LOCALLY);

        simulationSupport.initializeSimulationResult(result);

        // The transaction should (probably) not be set here. The "real" transaction is created by the item processing
        // gatekeeper. This one may be used as a "catch-all" for objects processed without the context of item processing.
        // (But are there any such objects? Should not be, because e.g. provisioning retry operations should not be allowed,
        // and plain synchronization is executed within item processing.)
        var oldSimulationTransaction = runningTask.setSimulationTransaction(getSimulationTransaction());
        try {
            runningTask.setExcludedFromStalenessChecking(isExcludedFromStalenessChecking());
            runningTask.setExecutionMode(getTaskExecutionMode());
            runResult = runLocally(localResult);
        } catch (ActivityInterruptedException e) {
            localResult.recordException(e); // TODO reconsider if it's ok to write WARNING as status?
            runResult = ActivityRunResult.interrupted();
        } catch (Exception e) {
            runResult = ActivityRunResult.handleException(e, localResult, this); // sets the local result status
        } finally {
            localResult.close();
            runningTask.setExcludedFromStalenessChecking(false);
            runningTask.setExecutionMode(oldExecutionMode);
            runningTask.setSimulationTransaction(oldSimulationTransaction);
        }

        updateStateOnRunEnd(localResult, runResult, result);

        return runResult;
    }

    public @NotNull TaskExecutionMode getTaskExecutionMode() throws ConfigurationException {
        return activity.getDefinition().getExecutionModeDefinition().getTaskExecutionMode();
    }

    public SimulationTransaction getSimulationTransaction() {
        return simulationSupport.getSimulationTransaction();
    }

    /** Updates {@link #activityState} (including flushing) and the tree state overview. */
    private void updateStateOnRunStart(OperationResult result) throws ActivityRunException {
        getTreeStateOverview().recordLocalRunStart(this, result);

        if (areRunRecordsSupported()) {
            activityState.getLiveStatistics().getLiveItemProcessing().recordRunStart(startTimestamp);
        }
        activityState.getLiveProgress().clearUncommitted();

        if (activityState.getRealizationState() != IN_PROGRESS_LOCAL) {
            activityState.setRealizationState(IN_PROGRESS_LOCAL);
            XMLGregorianCalendar realizationStart;
            if (isWorker()) {
                // Getting the timestamp from the coordinator task. Note that the coordinator activity state does not need
                // to be fresh, because the realization start timestamp is updated before worker tasks are started.
                realizationStart = getCoordinatorActivityState().getRealizationStartTimestamp();
            } else {
                realizationStart = XmlTypeConverter.createXMLGregorianCalendar(startTimestamp);
            }
            activityState.recordRealizationStart(realizationStart);
            if (!isWorker()) {
                onActivityRealizationStart(result);
            } else {
                // already done in DistributingActivityRun
            }
        }

        activityState.setResultStatus(IN_PROGRESS);
        activityState.recordRunStart(startTimestamp);

        activityState.updateProgressAndStatisticsNoCommit();
        activityState.flushPendingTaskModificationsChecked(result);
    }

    /** Returns (potentially not fresh) activity state of the coordinator task. Assuming we are in worker task. */
    ActivityState getCoordinatorActivityState() {
        try {
            return activityState.getCurrentActivityStateInParentTask(false,
                    getActivityStateDefinition().getWorkStateTypeName(), null);
        } catch (SchemaException | ObjectNotFoundException e) {
            // Shouldn't occur for running tasks with fresh = false.
            throw new SystemException("Unexpected exception: " + e.getMessage(), e);
        }
    }

    // TODO better method name?
    private void updateStateOnRunEnd(
            @NotNull OperationResult closedLocalResult, @NotNull ActivityRunResult runResult, @NotNull OperationResult result)
            throws ActivityRunException {

        noteEndTimestampIfNone();

        activityState.setRunEndTimestamp(endTimestamp);

        if (runResult.getOperationResultStatus() == null) {
            runResult.setOperationResultStatus(closedLocalResult.getStatus());
        }
        setCurrentResultStatus(runResult.getOperationResultStatus());

        getTreeStateOverview().recordLocalRunFinish(this, runResult, result);

        if (areRunRecordsSupported()) {
            activityState.getLiveStatistics().getLiveItemProcessing()
                    .recordRunEnd(
                            requireNonNull(startTimestamp, "no start timestamp"),
                            endTimestamp);
        }

        // The state is flushed upstream
    }

    protected abstract @NotNull ActivityRunResult runLocally(OperationResult result)
            throws ActivityRunException, CommonException, ActivityInterruptedException;

    /**
     * Fails if running within a worker task. (Currently this mode is not supported e.g. for composite activities.)
     */
    final void ensureNotInWorkerTask(@Nullable String customMessage) {
        if (isWorker()) {
            throw new UnsupportedOperationException(
                    Objects.requireNonNull(
                            customMessage,
                            "This activity cannot be run in worker tasks."));
        }
    }

    /** Updates item progress in the tree overview. Assumes that the activity run is still in progress. */
    public void updateItemProgressInTreeOverviewIfTimePassed(OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        getTreeStateOverview().updateItemProgressIfTimePassed(
                this,
                getStateOverviewProgressUpdateInterval(),
                result);
    }

    private long getStateOverviewProgressUpdateInterval() {
        Long configuredValue = getActivity().getReportingDefinition().getStateOverviewProgressUpdateInterval();
        if (configuredValue != null) {
            return configuredValue;
        } else if (isWorker()) {
            return DEFAULT_TREE_PROGRESS_UPDATE_INTERVAL_FOR_WORKERS;
        } else {
            return DEFAULT_TREE_PROGRESS_UPDATE_INTERVAL_FOR_STANDALONE;
        }
    }

    public boolean shouldUpdateProgressInStateOverview() {
        var mode = getActivity().getReportingDefinition().getStateOverviewProgressUpdateMode();
        return switch (mode) {
            case ALWAYS -> true;
            case NEVER -> false;
            case FOR_NON_LOCAL_ACTIVITIES -> !getRunningTask().isRoot();
        };
    }

    /**
     * Initializes current run status when activity run starts.
     * The default behavior is to set IN_PROGRESS here.
     */
    private void initializeCurrentResultStatusOnStart() {
        setCurrentResultStatus(IN_PROGRESS);
    }

    public @NotNull OperationResultStatusType getCurrentResultStatusBean() {
        return OperationResultStatus.createStatusType(currentResultStatus);
    }

    private void setCurrentResultStatus(@NotNull OperationResultStatus currentResultStatus) {
        this.currentResultStatus = currentResultStatus;
    }

    /** True if the task is excluded from staleness checking while running this activity. */
    public boolean isExcludedFromStalenessChecking() {
        return false;
    }

    /**
     * Updates task objectRef. This is e.g. to allow displaying of all tasks related to given resource.
     * Conditions:
     *
     * 1. task.objectRef has no value yet,
     * 2. the value provided by {@link #getDesiredTaskObjectRef()} is non-null.
     *
     * The method does this recursively towards the root of the task tree.
     */
    @Experimental
    final void setTaskObjectRef(OperationResult result) throws CommonException {
        RunningTask task = getRunningTask();
        if (task.getObjectOid() != null) {
            LOGGER.trace("Task.objectRef is already set for the current task. We assume it is also set for parent tasks.");
            return;
        }

        ObjectReferenceType desiredObjectRef = getDesiredTaskObjectRef();
        LOGGER.trace("Desired task object ref: {}", desiredObjectRef);
        if (desiredObjectRef == null) {
            return;
        }
        setObjectRefRecursivelyUpwards(task, desiredObjectRef, result);
    }

    /**
     * Returns the value that should be put into task.objectRef.
     *
     * Should be overridden by subclasses.
     *
     * It should be called _after_ {@link IterativeActivityRunSpecifics#beforeRun(OperationResult)} method,
     * in order to give the execution a chance to prepare data for this method.
     */
    protected @Nullable ObjectReferenceType getDesiredTaskObjectRef() {
        return null; // By default, we don't try to set up that item.
    }

    /** Sets objectRef on the given task and its ancestors (if not already set). */
    private void setObjectRefRecursivelyUpwards(@NotNull Task task, @NotNull ObjectReferenceType desiredObjectRef,
            @NotNull OperationResult result)
            throws CommonException {
        task.setObjectRef(desiredObjectRef);
        task.flushPendingModifications(result);
        Task parent = task.getParentTask(result);
        if (parent != null && parent.getObjectOid() == null) {
            setObjectRefRecursivelyUpwards(parent, desiredObjectRef, result);
        }
    }
}
