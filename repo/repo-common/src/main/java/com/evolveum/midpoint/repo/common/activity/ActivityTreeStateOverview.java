/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity;

import static com.evolveum.midpoint.prism.Referencable.getOid;
import static com.evolveum.midpoint.schema.result.OperationResultStatus.FATAL_ERROR;
import static com.evolveum.midpoint.schema.util.task.ActivityStateOverviewUtil.*;
import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR;
import static com.evolveum.midpoint.util.DebugUtil.lazy;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityStateOverviewProgressInformationVisibilityType.HIDDEN;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityStateOverviewProgressInformationVisibilityType.VISIBLE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.TaskActivityStateType.F_TREE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType.F_ACTIVITY_STATE;

import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.api.RepositoryService.ModificationsSupplier;
import com.evolveum.midpoint.repo.common.activity.execution.ActivityExecutionResult;
import com.evolveum.midpoint.repo.common.activity.execution.DistributingActivityExecution;
import com.evolveum.midpoint.repo.common.activity.execution.LocalActivityExecution;
import com.evolveum.midpoint.repo.common.task.CommonTaskBeans;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class ActivityTreeStateOverview {

    private static final Trace LOGGER = TraceManager.getTrace(ActivityTreeStateOverview.class);

    @NotNull private final Task rootTask;
    @NotNull private final CommonTaskBeans beans;

    @NotNull private static final ItemPath PATH_REALIZATION_STATE
            = ItemPath.create(F_ACTIVITY_STATE, F_TREE, ActivityTreeStateType.F_REALIZATION_STATE);
    @NotNull private static final ItemPath PATH_ACTIVITY_STATE_TREE
            = ItemPath.create(F_ACTIVITY_STATE, F_TREE, ActivityTreeStateType.F_ACTIVITY);

    /** When was the item progress last updated for the current activity? */
    private long itemProgressUpdatedTimestamp;

    public ActivityTreeStateOverview(@NotNull Task rootTask, @NotNull CommonTaskBeans beans) {
        this.rootTask = rootTask;
        this.beans = beans;
    }

    /**
     * Records the start of local activity execution in the tree overview.
     */
    public void recordLocalExecutionStart(@NotNull LocalActivityExecution<?, ?, ?> execution, @NotNull OperationResult result)
            throws ActivityExecutionException {

        modifyRootTask(taskBean -> {
            boolean progressVisible = execution.shouldUpdateProgressInStateOverview();
            ActivityStateOverviewType overview = getOrCreateStateOverview(taskBean);
            ActivityStateOverviewType entry = findOrCreateEntry(overview, execution.getActivityPath());
            if (execution.isWorker()) {
                LOGGER.trace("Not updating global activity entry fields because we are the worker: {}", execution);
            } else {
                entry.realizationState(ActivitySimplifiedRealizationStateType.IN_PROGRESS)
                        .resultStatus(OperationResultStatusType.IN_PROGRESS)
                        .progressInformationVisibility(progressVisible ? VISIBLE : HIDDEN)
                        .persistence(execution.getActivity().getActivityStateDefinition().getPersistence());
            }
            ActivityTaskStateOverviewType taskEntry =
                    findOrCreateTaskEntry(entry, execution.getRunningTask().getSelfReference())
                            .bucketsProcessingRole(execution.getActivityState().getBucketingRole())
                            .executionState(ActivityTaskExecutionStateType.EXECUTING)
                            .resultStatus(execution.getCurrentResultStatusBean());
            if (progressVisible && execution.doesSupportProgress()) {
                taskEntry.progress(execution.getActivityState().getLiveProgress().getOverview());
            }
            // bucket progress is updated on bucketing operations
            return createOverviewReplaceDeltas(overview);
        }, result);

        itemProgressUpdatedTimestamp = 0; // new activity -> new item progress
    }

    /**
     * Records the start of distributing (coordinator) activity realization (NOT execution).
     * It sets the fields that are not set by workers in their local executions.
     */
    public void recordDistributingActivityRealizationStart(@NotNull DistributingActivityExecution<?, ?, ?> execution,
            @NotNull OperationResult result) throws ActivityExecutionException {

        modifyRootTask(taskBean -> {
            ActivityStateOverviewType overview = getOrCreateStateOverview(taskBean);
            findOrCreateEntry(overview, execution.getActivityPath())
                    .realizationState(ActivitySimplifiedRealizationStateType.IN_PROGRESS)
                    .resultStatus(OperationResultStatusType.IN_PROGRESS)
                    .progressInformationVisibility(VISIBLE) // workers are always in subtasks
                    .persistence(execution.getActivity().getActivityStateDefinition().getPersistence());
            // bucket progress is updated on bucketing operations
            return createOverviewReplaceDeltas(overview);
        }, result);
    }

    /**
     * We need to know about all the children to be able to display the progress correctly.
     * So this method updates the tree with the information about children of a composite activity.
     *
     * (Note: We only add records here. We assume that no children are ever deleted.)
     */
    public void recordChildren(@NotNull LocalActivityExecution<?, ?, ?> execution, List<Activity<?, ?>> children,
            @NotNull OperationResult result)
            throws ActivityExecutionException {

        modifyRootTask(taskBean -> {
            ActivityStateOverviewType overview = getOrCreateStateOverview(taskBean);
            ActivityStateOverviewType entry = findOrCreateEntry(overview, execution.getActivityPath());
            boolean changed = false;
            for (Activity<?, ?> child : children) {
                var childEntry =
                        findOrCreateChildEntry(entry, child.getIdentifier(), false);
                if (childEntry == null) {
                    findOrCreateChildEntry(entry, child.getIdentifier(), true);
                    changed = true;
                }
            }
            return createOverviewReplaceDeltas(overview, changed);
        }, result);
    }

    /**
     * Records the finish of a local activity execution.
     *
     * Note that execution result can be null only in the case of (very rare) uncaught exception.
     */
    public void recordLocalExecutionFinish(@NotNull LocalActivityExecution<?, ?, ?> execution,
            @Nullable ActivityExecutionResult executionResult, @NotNull OperationResult result)
            throws ActivityExecutionException {

        modifyRootTask(taskBean -> {
            boolean progressVisible = execution.shouldUpdateProgressInStateOverview();

            ActivityStateOverviewType overview = getOrCreateStateOverview(taskBean);
            ActivityStateOverviewType entry = findOrCreateEntry(overview, execution.getActivityPath());
            if (execution.isWorker()) {
                LOGGER.trace("Not updating global activity entry fields because we are the worker: {}", execution);
            } else if (executionResult == null) {
                LOGGER.trace("Execution result is null. This is strange, most probably an error. "
                        + "We won't update the realization state, only the result status");
                entry.resultStatus(OperationResultStatusType.FATAL_ERROR);
            } else {
                entry.setRealizationState(executionResult.getSimplifiedRealizationState());
                entry.setResultStatus(executionResult.getOperationResultStatusBean());
            }
            // bucket progress is updated on bucketing operations
            findOrCreateTaskEntry(entry, execution.getRunningTask().getSelfReference())
                    .progress(progressVisible && execution.doesSupportProgress() ?
                            execution.getActivityState().getLiveProgress().getOverview() : null)
                    .executionState(ActivityTaskExecutionStateType.NOT_EXECUTING)
                    .resultStatus(execution.getCurrentResultStatusBean());
            return createOverviewReplaceDeltas(overview);
        }, result);
    }

    /**
     * Records the finish of distributing (coordinator) activity realization (NOT execution).
     * It sets the fields that are not set by workers in their local executions.
     */
    public void recordDistributingActivityRealizationFinish(@NotNull DistributingActivityExecution<?, ?, ?> execution,
            @NotNull ActivityExecutionResult executionResult, @NotNull OperationResult result)
            throws ActivityExecutionException {
        modifyRootTask(taskBean -> {
            ActivityStateOverviewType overview = getOrCreateStateOverview(taskBean);
            findOrCreateEntry(overview, execution.getActivityPath())
                    .realizationState(executionResult.getSimplifiedRealizationState())
                    .resultStatus(executionResult.getOperationResultStatusBean());
            // bucket progress is updated on bucketing operations
            return createOverviewReplaceDeltas(overview);
        }, result);
    }

    /**
     * Called when a task was found dead.
     * Updates the execution state (in the future we plan also to update the progress).
     */
    public void recordTaskDead(@NotNull Task task, @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        Holder<Boolean> changed = new Holder<>(false);

        modifyRootTaskUnchecked(taskBean -> {
            changed.setValue(false);
            ActivityStateOverviewType overview = getStateOverview(taskBean);
            if (overview == null) {
                return List.of();
            }
            updateActivityTreeOnTaskDead(overview, task, changed);
            return createOverviewReplaceDeltas(overview, changed.getValue());
        }, result);

        if (changed.getValue()) {
            LOGGER.info("'Task dead' event for {} was recorded successfully in {}", task, rootTask);
        }
    }

    private void updateActivityTreeOnTaskDead(@NotNull ActivityStateOverviewType activity, @NotNull Task task,
            @NotNull Holder<Boolean> changed) {
        updateActivityForTaskDead(activity, task, changed);
        activity.getActivity().forEach(child -> updateActivityTreeOnTaskDead(child, task, changed));
    }

    private void updateActivityForTaskDead(@NotNull ActivityStateOverviewType activity, @NotNull Task task,
            @NotNull Holder<Boolean> changed) {
        activity.getTask().stream()
                .filter(taskOverview -> task.getOid().equals(getOid(taskOverview.getTaskRef())))
                .filter(taskOverview -> taskOverview.getExecutionState() == ActivityTaskExecutionStateType.EXECUTING)
                .forEach(taskOverview -> {
                    taskOverview.setExecutionState(ActivityTaskExecutionStateType.NOT_EXECUTING);
                    // TODO in the future we can switch IN_PROGRESS result status to UNKNOWN. But not now.
                    changed.setValue(true);
                });
    }

    /**
     * Updates bucket (and item) progress information in the activity tree.
     *
     * Note that there can be a race condition when updating buckets. If an update that was generated earlier
     * (containing smaller # of completed buckets) is applied after an update generated later (larger #
     * of completed buckets), incorrect results may be stored in the overview. Therefore we use a hack
     * with {@link #isBefore(BucketProgressOverviewType, BucketProgressOverviewType)} method.
     */
    public void updateBucketAndItemProgress(@NotNull LocalActivityExecution<?, ?, ?> execution,
            @NotNull BucketProgressOverviewType bucketProgress, @NotNull OperationResult result)
            throws ActivityExecutionException {

        if (!execution.shouldUpdateProgressInStateOverview()) {
            return;
        }

        modifyRootTask(taskBean -> {
            ActivityStateOverviewType overview = getOrCreateStateOverview(taskBean);
            ActivityStateOverviewType entry = findOrCreateEntry(overview, execution.getActivityPath());
            if (isBefore(bucketProgress, entry.getBucketProgress())) {
                LOGGER.debug("Updated bucket progress ({}) is 'before' the stored one ({}) - not updating",
                        bucketProgress, entry.getBucketProgress());
            } else {
                entry.setBucketProgress(bucketProgress.clone());
            }
            findOrCreateTaskEntry(entry, execution.getRunningTask().getSelfReference())
                    .progress(execution.doesSupportProgress() ?
                            execution.getActivityState().getLiveProgress().getOverview() : null)
                    .resultStatus(execution.getCurrentResultStatusBean());
            return createOverviewReplaceDeltas(overview);
        }, result);

        LOGGER.trace("Bucket and item progress updated in {}", execution);
        itemProgressUpdatedTimestamp = System.currentTimeMillis();
    }

    private boolean isBefore(@NotNull BucketProgressOverviewType bucket1, @Nullable BucketProgressOverviewType bucket2) {
        return bucket2 != null &&
                Objects.equals(bucket1.getTotalBuckets(), bucket2.getTotalBuckets()) &&
                bucket1.getCompleteBuckets() != null && bucket2.getCompleteBuckets() != null &&
                bucket1.getCompleteBuckets() < bucket2.getCompleteBuckets();
    }

    public void updateItemProgressIfTimePassed(@NotNull LocalActivityExecution<?, ?, ?> execution, long interval,
            OperationResult result) throws SchemaException, ObjectNotFoundException {

        if (!execution.shouldUpdateProgressInStateOverview() || !execution.doesSupportProgress()) {
            LOGGER.trace("Item progress update skipped in {}", execution);
            return;
        }

        if (System.currentTimeMillis() < itemProgressUpdatedTimestamp + interval) {
            LOGGER.trace("Item progress update interval was not reached yet, skipping updating in {}", execution);
            return;
        }

        modifyRootTaskUnchecked(taskBean -> {
            ActivityStateOverviewType overview = getOrCreateStateOverview(taskBean);
            ActivityStateOverviewType entry = findOrCreateEntry(overview, execution.getActivityPath());
            findOrCreateTaskEntry(entry, execution.getRunningTask().getSelfReference())
                    .progress(execution.getActivityState().getLiveProgress().getOverview())
                    .resultStatus(execution.getCurrentResultStatusBean());
            return createOverviewReplaceDeltas(overview);
        }, result);

        LOGGER.trace("Item progress updated in {}", execution);
        itemProgressUpdatedTimestamp = System.currentTimeMillis();
    }

    public ActivityTreeRealizationStateType getRealizationState() {
        return rootTask.getPropertyRealValue(PATH_REALIZATION_STATE, ActivityTreeRealizationStateType.class);
    }

    /**
     * Updates the realization state (including writing to the repository).
     */
    public void updateRealizationState(ActivityTreeRealizationStateType value, OperationResult result)
            throws ActivityExecutionException {
        try {
            rootTask.setItemRealValues(PATH_REALIZATION_STATE, value);
            rootTask.flushPendingModifications(result);
        } catch (CommonException e) {
            throw new ActivityExecutionException("Couldn't update tree realization state", FATAL_ERROR, PERMANENT_ERROR, e);
        }
    }

    public ActivityStateOverviewType getActivityStateTree() {
        return rootTask.getPropertyRealValueOrClone(PATH_ACTIVITY_STATE_TREE, ActivityStateOverviewType.class);
    }

    public void purge(OperationResult result) throws ActivityExecutionException {
        updateActivityStateTree(
                purgeStateRecursively(
                        getActivityStateTree()
                ),
                result);

        LOGGER.trace("State tree after purging: {}", lazy(this::getActivityStateTree));
    }

    /**
     * Updates the activity state tree (including writing to the repository).
     */
    private void updateActivityStateTree(ActivityStateOverviewType value, OperationResult result)
            throws ActivityExecutionException {
        try {
            rootTask.setItemRealValues(PATH_ACTIVITY_STATE_TREE, value);
            rootTask.flushPendingModifications(result);
        } catch (CommonException e) {
            throw new ActivityExecutionException("Couldn't update activity state tree", FATAL_ERROR, PERMANENT_ERROR, e);
        }
    }

    /** Returns the new value of the state (either the updated original one or null if the value is to be deleted. */
    private ActivityStateOverviewType purgeStateRecursively(ActivityStateOverviewType state) {
        if (state == null || isTransient(state)) {
            return null;
        }

        state.setRealizationState(null);
        state.setResultStatus(null);

        state.getActivity().replaceAll(this::purgeStateRecursively);
        state.getActivity().removeIf(Objects::isNull);
        return state;
    }

    private boolean isTransient(@NotNull ActivityStateOverviewType state) {
        return state.getPersistence() == null ||
                state.getPersistence() == ActivityStatePersistenceType.SINGLE_REALIZATION;
    }

    private @NotNull List<ItemDelta<?, ?>> createOverviewReplaceDeltas(ActivityStateOverviewType overview, boolean changed)
            throws SchemaException {
        return changed ? createOverviewReplaceDeltas(overview) : List.of();
    }

    private @NotNull List<ItemDelta<?, ?>> createOverviewReplaceDeltas(ActivityStateOverviewType overview)
            throws SchemaException {
        return beans.prismContext.deltaFor(TaskType.class)
                .item(F_ACTIVITY_STATE, F_TREE, ActivityTreeStateType.F_ACTIVITY).replace(overview)
                .asItemDeltas();
    }

    private void modifyRootTask(@NotNull ModificationsSupplier<TaskType> modificationsSupplier, OperationResult result)
            throws ActivityExecutionException {
        try {
            modifyRootTaskUnchecked(modificationsSupplier, result);
        } catch (Exception e) {
            throw new ActivityExecutionException("Couldn't update the activity tree", FATAL_ERROR, PERMANENT_ERROR, e);
        }
    }

    private void modifyRootTaskUnchecked(@NotNull ModificationsSupplier<TaskType> modificationsSupplier, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        try {
            beans.plainRepositoryService.modifyObjectDynamically(TaskType.class, rootTask.getOid(), null,
                    modificationsSupplier, null, result);
        } catch (ObjectAlreadyExistsException e) {
            throw new SystemException("Unexpected ObjectAlreadyExistsException: " + e.getMessage(), e);
        }
    }
}
