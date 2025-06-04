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
import javax.xml.datatype.XMLGregorianCalendar;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.RepositoryService.ModificationsSupplier;
import com.evolveum.midpoint.repo.common.activity.run.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.LocalizationUtil;
import com.evolveum.midpoint.schema.util.task.ActivityStateOverviewUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Represents the activity tree state overview that is stored in the root task.
 *
 * This class does _not_ hold the state itself. Instead, it contains methods that update the state in the task.
 * See e.g. {@link #recordLocalRunStart(LocalActivityRun, OperationResult)} and its brethren.
 */
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
     * Records the start of local activity run in the tree overview.
     */
    public void recordLocalRunStart(@NotNull LocalActivityRun<?, ?, ?> run, @NotNull OperationResult result)
            throws ActivityRunException {

        modifyRootTask(existingTaskBean -> {
            var taskBean = existingTaskBean.clone(); // todo check if the code below can change the data
            boolean progressVisible = run.shouldUpdateProgressInStateOverview();
            ActivityStateOverviewType overview = getOrCreateStateOverview(taskBean);
            ActivityStateOverviewType entry = findOrCreateEntry(overview, run.getActivityPath());
            if (run.isWorker()) {
                LOGGER.trace("Not updating global activity entry fields because we are the worker: {}", run);
            } else {
                entry.realizationState(ActivitySimplifiedRealizationStateType.IN_PROGRESS)
                        .resultStatus(OperationResultStatusType.IN_PROGRESS)
                        .progressInformationVisibility(progressVisible ? VISIBLE : HIDDEN)
                        .persistence(run.getActivity().getActivityStateDefinition().getPersistence());
            }
            ActivityTaskStateOverviewType taskEntry =
                    findOrCreateTaskEntry(entry, run.getRunningTask().getSelfReference())
                            .bucketsProcessingRole(run.getActivityState().getBucketingRole())
                            .executionState(ActivityTaskExecutionStateType.RUNNING)
                            .node(beans.taskManager.getNodeId())
                            .resultStatus(run.getCurrentResultStatusBean());
            if (progressVisible && run.isProgressSupported()) {
                taskEntry.progress(run.getActivityState().getLiveProgress().getOverview());
            }
            // bucket progress is updated on bucketing operations
            return createOverviewReplaceDeltas(overview);
        }, result);

        itemProgressUpdatedTimestamp = 0; // new activity -> new item progress
    }

    /**
     * Records the start of distributed activity realization (NOT run).
     * It sets the fields that are not set by workers in their local runs.
     */
    public void recordDistributedActivityRealizationStart(
            @NotNull DistributingActivityRun<?, ?, ?> run, @NotNull OperationResult result) throws ActivityRunException {

        modifyRootTask(existingTaskBean -> {
            var taskBean = existingTaskBean.clone(); // todo check if the code below can change the data
            ActivityStateOverviewType overview = getOrCreateStateOverview(taskBean);
            findOrCreateEntry(overview, run.getActivityPath())
                    .realizationState(ActivitySimplifiedRealizationStateType.IN_PROGRESS)
                    .resultStatus(OperationResultStatusType.IN_PROGRESS)
                    .progressInformationVisibility(VISIBLE) // workers are always in subtasks
                    .persistence(run.getActivity().getActivityStateDefinition().getPersistence());
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
    public void recordChildren(
            @NotNull LocalActivityRun<?, ?, ?> run, List<Activity<?, ?>> children, @NotNull OperationResult result)
            throws ActivityRunException {

        modifyRootTask(existingTaskBean -> {
            var taskBean = existingTaskBean.clone(); // todo check if the code below can change the data
            ActivityStateOverviewType overview = getOrCreateStateOverview(taskBean);
            ActivityStateOverviewType entry = findOrCreateEntry(overview, run.getActivityPath());
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
     * Records the finish of a local activity run.
     *
     * Note that run result can be null only in the case of (very rare) uncaught exception.
     */
    public void recordLocalRunFinish(
            @NotNull LocalActivityRun<?, ?, ?> run, @Nullable ActivityRunResult runResult, @NotNull OperationResult result)
            throws ActivityRunException {

        modifyRootTask(existingTaskBean -> {
            var taskBean = existingTaskBean.clone(); // todo check if the code below can change the data
            boolean progressVisible = run.shouldUpdateProgressInStateOverview();

            ActivityStateOverviewType overview = getOrCreateStateOverview(taskBean);
            ActivityStateOverviewType entry = findOrCreateEntry(overview, run.getActivityPath());
            if (run.isWorker()) {
                LOGGER.trace("Not updating global activity entry fields because we are the worker: {}", run);
            } else if (runResult == null) {
                LOGGER.trace("Run result is null. This is strange, most probably an error. "
                        + "We won't update the realization state, only the result status");
                entry.resultStatus(OperationResultStatusType.FATAL_ERROR);
            } else {
                entry.setRealizationState(runResult.getSimplifiedRealizationState());
                entry.setResultStatus(runResult.getOperationResultStatusBean());
            }
            // bucket progress is updated on bucketing operations
            ActivityTaskStateOverviewType stateOverview = findOrCreateTaskEntry(entry, run.getRunningTask().getSelfReference())
                    .progress(progressVisible && run.isProgressSupported() ?
                            run.getActivityState().getLiveProgress().getOverview() : null)
                    .executionState(ActivityTaskExecutionStateType.NOT_RUNNING)
                    .resultStatus(run.getCurrentResultStatusBean());

            recordStateOverviewMessageFromRunResult(stateOverview, runResult);

            return createOverviewReplaceDeltas(overview);
        }, result);
    }

    private void recordStateOverviewMessageFromOperationResult(ActivityTaskStateOverviewType stateOverview, OperationResult result) {
        if (result == null || !(result.isWarning() || result.isPartialError() || result.isError())) {
            return;
        }

        stateOverview
                .message(result.getMessage())
                .userFriendlyMessage(LocalizationUtil.createLocalizableMessageType(result.getUserFriendlyMessage()));
    }

    private void recordStateOverviewMessageFromRunResult(ActivityTaskStateOverviewType stateOverview, ActivityRunResult runResult) {
        if (runResult == null || runResult.getThrowable() == null) {
            return;
        }

        if (!runResult.isError() && runResult.getOperationResultStatusBean() != OperationResultStatusType.WARNING) {
            // clear message if there's no warning/error
            stateOverview
                    .message(null)
                    .userFriendlyMessage(null);
            return;
        }

        if (runResult.getThrowable() instanceof CommonException ce) {
            stateOverview
                    .message(ce.getLocalizedMessage())
                    .userFriendlyMessage(LocalizationUtil.createLocalizableMessageType(ce.getUserFriendlyMessage()));
        } else {
            stateOverview
                    .message(runResult.getThrowable().getMessage());
        }
    }

    /**
     * Records the finish of distributed activity realization (NOT run).
     * It sets the fields that are not set by workers in their local runs.
     */
    public void recordDistributedActivityRealizationFinish(
            @NotNull DistributingActivityRun<?, ?, ?> run, @NotNull ActivityRunResult runResult, @NotNull OperationResult result)
            throws ActivityRunException {
        modifyRootTask(existingTaskBean -> {
            var taskBean = existingTaskBean.clone(); // todo check if the code below can change the data
            ActivityStateOverviewType overview = getOrCreateStateOverview(taskBean);
            findOrCreateEntry(overview, run.getActivityPath())
                    .realizationState(runResult.getSimplifiedRealizationState())
                    .resultStatus(runResult.getOperationResultStatusBean());
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

        modifyRootTaskUnchecked(existingTaskBean -> {
            var taskBean = existingTaskBean.clone(); // todo check if the code below can change the data
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

    private void updateActivityTreeOnTaskDead(
            @NotNull ActivityStateOverviewType activity, @NotNull Task task, @NotNull Holder<Boolean> changed) {
        updateActivityForTaskDead(activity, task, changed);
        activity.getActivity().forEach(child -> updateActivityTreeOnTaskDead(child, task, changed));
    }

    private void updateActivityForTaskDead(
            @NotNull ActivityStateOverviewType activity, @NotNull Task task, @NotNull Holder<Boolean> changed) {
        activity.getTask().stream()
                .filter(taskOverview -> task.getOid().equals(getOid(taskOverview.getTaskRef())))
                .filter(taskOverview -> taskOverview.getExecutionState() == ActivityTaskExecutionStateType.RUNNING)
                .forEach(taskOverview -> {
                    taskOverview.setExecutionState(ActivityTaskExecutionStateType.NOT_RUNNING);
                    // TODO in the future we can switch IN_PROGRESS result status to UNKNOWN. But not now.
                    changed.setValue(true);
                });
    }

    /**
     * Updates bucket (and item) progress information in the activity tree.
     * Also clears the "stalled since" flag.
     *
     * Note that there can be a race condition when updating buckets. If an update that was generated earlier
     * (containing smaller # of completed buckets) is applied after an update generated later (larger #
     * of completed buckets), incorrect results may be stored in the overview. Therefore we use a hack
     * with {@link #isBefore(BucketProgressOverviewType, BucketProgressOverviewType)} method.
     */
    public void updateBucketAndItemProgress(
            @NotNull LocalActivityRun<?, ?, ?> run,
            @NotNull BucketProgressOverviewType bucketProgress,
            @NotNull OperationResult result)
            throws ActivityRunException {

        if (!run.shouldUpdateProgressInStateOverview()) {
            return;
        }

        modifyRootTask(existingTaskBean -> {
            var taskBean = existingTaskBean.clone(); // todo check if the code below can change the data
            ActivityStateOverviewType overview = getOrCreateStateOverview(taskBean);
            ActivityStateOverviewType entry = findOrCreateEntry(overview, run.getActivityPath());
            if (isBefore(bucketProgress, entry.getBucketProgress())) {
                LOGGER.debug("Updated bucket progress ({}) is 'before' the stored one ({}) - not updating",
                        bucketProgress, entry.getBucketProgress());
            } else {
                entry.setBucketProgress(bucketProgress.clone());
            }
            ActivityTaskStateOverviewType stateOverview = findOrCreateTaskEntry(entry, run.getRunningTask().getSelfReference())
                    .progress(run.isProgressSupported() ?
                            run.getActivityState().getLiveProgress().getOverview() : null)
                    .stalledSince((XMLGregorianCalendar) null)
                    .resultStatus(run.getCurrentResultStatusBean());

            recordStateOverviewMessageFromOperationResult(stateOverview, run.getRunningTask().getResult());

            return createOverviewReplaceDeltas(overview);
        }, result);

        LOGGER.trace("Bucket and item progress updated in {}", run);
        itemProgressUpdatedTimestamp = System.currentTimeMillis();
    }

    private boolean isBefore(@NotNull BucketProgressOverviewType bucket1, @Nullable BucketProgressOverviewType bucket2) {
        return bucket2 != null &&
                Objects.equals(bucket1.getTotalBuckets(), bucket2.getTotalBuckets()) &&
                bucket1.getCompleteBuckets() != null && bucket2.getCompleteBuckets() != null &&
                bucket1.getCompleteBuckets() < bucket2.getCompleteBuckets();
    }

    /** Assumes that the activity run is still in progress. (I.e. also clear the "stalled since" flag.) */
    public void updateItemProgressIfTimePassed(
            @NotNull LocalActivityRun<?, ?, ?> run, long interval, OperationResult result)
            throws SchemaException, ObjectNotFoundException {

        if (!run.shouldUpdateProgressInStateOverview() || !run.isProgressSupported()) {
            LOGGER.trace("Item progress update skipped in {}", run);
            return;
        }

        if (System.currentTimeMillis() < itemProgressUpdatedTimestamp + interval) {
            LOGGER.trace("Item progress update interval was not reached yet, skipping updating in {}", run);
            return;
        }

        modifyRootTaskUnchecked(existingTaskBean -> {
            var taskBean = existingTaskBean.clone(); // todo check if the code below can change the data
            ActivityStateOverviewType overview = getOrCreateStateOverview(taskBean);
            ActivityStateOverviewType entry = findOrCreateEntry(overview, run.getActivityPath());
            ActivityTaskStateOverviewType stateOverview = findOrCreateTaskEntry(entry, run.getRunningTask().getSelfReference())
                    .stalledSince((XMLGregorianCalendar) null)
                    .progress(run.getActivityState().getLiveProgress().getOverview())
                    .resultStatus(run.getCurrentResultStatusBean());

            recordStateOverviewMessageFromOperationResult(stateOverview, run.getRunningTask().getResult());

            return createOverviewReplaceDeltas(overview);
        }, result);

        LOGGER.trace("Item progress updated in {}", run);
        itemProgressUpdatedTimestamp = System.currentTimeMillis();
    }

    /** Finds all occurrences of the task in "running" activities and marks them as stalled. */
    public void markTaskStalled(@NotNull String taskOid, long stalledSince, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        modifyRootTaskUnchecked(existingTaskBean -> {
            var taskBean = existingTaskBean.clone(); // todo check if the code below can change the data
            ActivityStateOverviewType overview = getStateOverview(taskBean);
            if (overview == null) {
                return List.of();
            }
            Holder<Boolean> changed = new Holder<>(false);
            ActivityStateOverviewUtil.acceptStateOverviewVisitor(overview, state -> {
                if (state.getRealizationState() == ActivitySimplifiedRealizationStateType.IN_PROGRESS) {
                    for (ActivityTaskStateOverviewType taskInfo : state.getTask()) {
                        if (taskInfo.getExecutionState() == ActivityTaskExecutionStateType.RUNNING &&
                                taskOid.equals(getOid(taskInfo.getTaskRef()))) {
                            taskInfo.setStalledSince(XmlTypeConverter.createXMLGregorianCalendar(stalledSince));
                            changed.setValue(true);
                        }
                    }
                }
            });
            return createOverviewReplaceDeltas(overview, changed.getValue());
        }, result);
    }

    public ActivityTreeRealizationStateType getRealizationState() {
        return rootTask.getPropertyRealValue(PATH_REALIZATION_STATE, ActivityTreeRealizationStateType.class);
    }

    /**
     * Updates the realization state (including writing to the repository).
     */
    void updateRealizationState(ActivityTreeRealizationStateType value, OperationResult result)
            throws ActivityRunException {
        try {
            rootTask.setItemRealValues(PATH_REALIZATION_STATE, value);
            rootTask.flushPendingModifications(result);
        } catch (CommonException e) {
            throw new ActivityRunException("Couldn't update tree realization state", FATAL_ERROR, PERMANENT_ERROR, e);
        }
    }

    private ActivityStateOverviewType getActivityStateTree() {
        return rootTask.getPropertyRealValueOrClone(PATH_ACTIVITY_STATE_TREE, ActivityStateOverviewType.class);
    }

    void purge(OperationResult result) throws ActivityRunException {
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
            throws ActivityRunException {
        try {
            rootTask.setItemRealValues(PATH_ACTIVITY_STATE_TREE, value);
            rootTask.flushPendingModifications(result);
        } catch (CommonException e) {
            throw new ActivityRunException("Couldn't update activity state tree", FATAL_ERROR, PERMANENT_ERROR, e);
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
            throws ActivityRunException {
        try {
            modifyRootTaskUnchecked(modificationsSupplier, result);
        } catch (Exception e) {
            throw new ActivityRunException("Couldn't update the activity tree", FATAL_ERROR, PERMANENT_ERROR, e);
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

    public void updateTaskRunIdentifier(String identifier, OperationResult result) throws ActivityRunException {
        updateActivityStateTree(
                updateTaskRunIdentifierRecursively(getActivityStateTree(), identifier),
                result);
    }

    private ActivityStateOverviewType updateTaskRunIdentifierRecursively(ActivityStateOverviewType state, String identifier) {
        if (state == null) {
            return null;
        }

        state.setTaskRunIdentifier(identifier);

        for (ActivityStateOverviewType child : state.getActivity()) {
            updateTaskRunIdentifierRecursively(child, identifier);
        }

        state.getActivity().removeIf(Objects::isNull);

        return state;
    }

    public void createActivityExecution(OperationResult result) throws ActivityRunException {
        updateActivityStateTree(
                createActivityExecutionRecursively(getActivityStateTree()),
                result);
    }

    private ActivityStateOverviewType createActivityExecutionRecursively(ActivityStateOverviewType state) {
        if (state == null) {
            return null;
        }

        ActivityExecutionType history = new ActivityExecutionType();
        history.setTaskRunIdentifier(state.getTaskRunIdentifier());
        history.setResultStatus(state.getResultStatus());
//        history.setRunStartTimestamp(state.get());
//        history.setRunEndTimestamp();
//        history.setPolicies();
        // todo more

        state.getActivityExecution().add(history);

        for (ActivityStateOverviewType child : state.getActivity()) {
            createActivityExecutionRecursively(child);
        }

        return state;
    }
}
