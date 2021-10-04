/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.execution;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;
import com.evolveum.midpoint.repo.common.activity.state.ActivityState;
import com.evolveum.midpoint.repo.common.task.IterativeActivityExecutionSpecifics;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
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

import static com.evolveum.midpoint.schema.result.OperationResultStatus.IN_PROGRESS;
import static com.evolveum.midpoint.schema.result.OperationResultStatus.UNKNOWN;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityRealizationStateType.IN_PROGRESS_LOCAL;

/**
 * The "real" execution of an activity - i.e. not a delegation nor a distribution.
 *
 * Responsibilities at this level of abstraction:
 *
 * 1. records execution start/stop + item progress in the tree state overview,
 * 2. records execution start/stop in the item processing statistics (execution records),
 * 3. updates progress information (clears uncommitted on start).
 */
public abstract class LocalActivityExecution<
        WD extends WorkDefinition,
        AH extends ActivityHandler<WD, AH>,
        BS extends AbstractActivityWorkStateType> extends AbstractActivityExecution<WD, AH, BS> {

    private static final Trace LOGGER = TraceManager.getTrace(LocalActivityExecution.class);

    private static final long DEFAULT_TREE_PROGRESS_UPDATE_INTERVAL_FOR_STANDALONE = 9000;
    private static final long DEFAULT_TREE_PROGRESS_UPDATE_INTERVAL_FOR_WORKERS = 60000;

    @NotNull private OperationResultStatus currentResultStatus = UNKNOWN;

    protected LocalActivityExecution(@NotNull ExecutionInstantiationContext<WD, AH> context) {
        super(context);
    }

    @Override
    protected @NotNull ActivityExecutionResult executeInternal(OperationResult result)
            throws ActivityExecutionException {

        updateStateOnExecutionStart(result);
        ActivityExecutionResult executionResult;
        try {
            getRunningTask().setExcludedFromStalenessChecking(isExcludedFromStalenessChecking());
            executionResult = executeLocal(result);
        } catch (Exception e) {
            executionResult = ActivityExecutionResult.handleException(e, this);
        }
        getRunningTask().setExcludedFromStalenessChecking(false);

        updateStateOnExecutionFinish(result, executionResult);
        return executionResult;
    }

    private void updateStateOnExecutionStart(OperationResult result) throws ActivityExecutionException {
        initializeCurrentResultStatusOnStart();

        getTreeStateOverview().recordLocalExecutionStart(this, result);

        if (supportsExecutionRecords()) {
            activityState.getLiveStatistics().getLiveItemProcessing().recordExecutionStart(startTimestamp);
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
        }

        activityState.setResultStatus(IN_PROGRESS);
        activityState.recordExecutionStart(startTimestamp);
        activityState.flushPendingTaskModificationsChecked(result);
    }

    /** Returns (potentially not fresh) activity state of the coordinator task. Assuming we are in worker task. */
    protected ActivityState getCoordinatorActivityState() {
        try {
            return activityState.getCurrentActivityStateInParentTask(false,
                    getActivityStateDefinition().getWorkStateTypeName(), null);
        } catch (SchemaException | ObjectNotFoundException e) {
            // Shouldn't occur for running tasks with fresh = false.
            throw new SystemException("Unexpected exception: " + e.getMessage(), e);
        }
    }

    private void updateStateOnExecutionFinish(OperationResult result, ActivityExecutionResult executionResult)
            throws ActivityExecutionException {
        noteEndTimestampIfNone();
        activityState.setExecutionEndTimestamp(endTimestamp);

        setCurrentResultStatus(executionResult.getOperationResultStatus());

        getTreeStateOverview().recordLocalExecutionFinish(this, executionResult, result);

        if (supportsExecutionRecords()) {
            activityState.getLiveStatistics().getLiveItemProcessing()
                    .recordExecutionEnd(startTimestamp, endTimestamp);
        }

        // The state is flushed upstream
    }

    private boolean supportsExecutionRecords() {
        // Temporary solution: activities that have persistent/semi-persistent state are those that execute in short cycles
        // (like live sync, various scanners, and so on). We usually do not want to store execution records for these.
        return doesSupportStatistics() && activityStateDefinition.isSingleRealization();
    }

    protected abstract @NotNull ActivityExecutionResult executeLocal(OperationResult result)
            throws ActivityExecutionException, CommonException;

    /** Updates item progress in the tree overview. Assumes that the activity execution is still in progress. */
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
        switch (mode) {
            case ALWAYS:
                return true;
            case NEVER:
                return false;
            case FOR_NON_LOCAL_ACTIVITIES:
                return !getRunningTask().isRoot();
            default:
                throw new AssertionError(mode);
        }
    }

    /**
     * Initializes current execution status when activity execution starts.
     * The default behavior is to set IN_PROGRESS here.
     */
    private void initializeCurrentResultStatusOnStart() {
        setCurrentResultStatus(IN_PROGRESS);
    }

    public @NotNull OperationResultStatus getCurrentResultStatus() {
        return currentResultStatus;
    }

    public @NotNull OperationResultStatusType getCurrentResultStatusBean() {
        return OperationResultStatus.createStatusType(currentResultStatus);
    }

    public void setCurrentResultStatus(@NotNull OperationResultStatus currentResultStatus) {
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
    protected final void setTaskObjectRef(OperationResult result) throws CommonException {
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
     * It should be called _after_ {@link IterativeActivityExecutionSpecifics#beforeExecution(OperationResult)} method,
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
        if (parent != null && parent.getObjectOid() != null) {
            setObjectRefRecursivelyUpwards(parent, desiredObjectRef, result);
        }
    }
}
