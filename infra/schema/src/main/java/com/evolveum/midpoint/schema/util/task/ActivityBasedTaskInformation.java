/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElseGet;

import static com.evolveum.midpoint.schema.util.task.ActivityProgressInformationBuilder.InformationSource.FULL_STATE_PREFERRED;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.xml.datatype.XMLGregorianCalendar;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Implementation of {@link TaskInformation} based on new, activity-based tasks.
 * Assumes knowledge of both root and current task.
 */
public class ActivityBasedTaskInformation extends TaskInformation {

    /**
     * Progress information for the current task and its children.
     *
     * It can be "activity-based" or determined from legacy sources.
     */
    @NotNull private final ActivityProgressInformation progressInformation;

    /** Activity state for the task's local root activity. */
    @NotNull private final ActivityStateType localRootActivityState;

    private ActivityBasedTaskInformation(
            @NotNull TaskType task,
            @NotNull ActivityWorkersInformation workersInformation,
            @NotNull OperationResultStatusType overallStatus,
            @NotNull ActivityProgressInformation progressInformation,
            @NotNull ActivityStateType localRootActivityState) {
        super(task, workersInformation, overallStatus);
        this.progressInformation = progressInformation;
        this.localRootActivityState = localRootActivityState;
    }

    /**
     * Precondition: isActivityBasedRoot(rootTask) i.e. hasActivityState (among others)
     */
    static @NotNull ActivityBasedTaskInformation fromActivityBasedRootTask(@NotNull TaskType rootTask) {
        ActivityStateOverviewType stateOverview = requireNonNull(
                ActivityStateOverviewUtil.getStateOverview(rootTask), "no state overview");
        ActivityWorkersInformation workers =
                ActivityWorkersInformation.fromActivityStateOverview(stateOverview);
        return new ActivityBasedTaskInformation(
                rootTask,
                workers,
                computeStatus(stateOverview, rootTask.getResultStatus(), workers),
                ActivityProgressInformation.fromRootTask(rootTask, FULL_STATE_PREFERRED),
                getLocalRootActivityState(rootTask));
    }

    private static @NotNull ActivityStateType getLocalRootActivityState(@NotNull TaskType task) {
        return requireNonNullElseGet( // null only in very rare conditions
                task.getActivityState().getActivity(),
                () -> new ActivityStateType());
    }

    static @NotNull ActivityBasedTaskInformation fromActivityBasedSubtask(@NotNull TaskType task, @NotNull TaskType rootTask) {
        ActivityPath activityPath = ActivityStateUtil.getLocalRootPath(task.getActivityState());
        ActivityStateOverviewType activityStateOverview = ActivityStateOverviewUtil.getStateOverview(rootTask, activityPath);
        if (activityStateOverview != null) {
            ActivityWorkersInformation workers =
                    ActivityWorkersInformation.fromActivityStateOverview(activityStateOverview);
            ActivityProgressInformation progress = ActivityProgressInformation.fromRootTask(rootTask, FULL_STATE_PREFERRED)
                    .find(activityPath);
            return new ActivityBasedTaskInformation(
                    task,
                    workers,
                    computeStatus(activityStateOverview, task.getResultStatus(), workers),
                    progress != null ? progress : ActivityProgressInformation.unknown(activityPath),
                    getLocalRootActivityState(task));
        } else {
            return new ActivityBasedTaskInformation(
                    task,
                    ActivityWorkersInformation.empty(),
                    OperationResultStatusType.UNKNOWN,
                    ActivityProgressInformation.unknown(activityPath),
                    new ActivityStateType());
        }
    }

    @Override
    public String getProgressDescription(boolean longForm) {
        return progressInformation.toHumanReadableString(longForm);
    }

    @Override
    public double getProgress() {
        if (progressInformation.isComplete()) {
            // MID-10287 standardize the progress to 1.0 when the task is complete (100%).
            return 1.0;
        }

        // We need to list only leaf activities. Then compare them to the completed ones.
        // Current drawback is that activities and their sub activities are created when they
        // start, not during the task creation. This means that progress is not accurate for
        // the whole task duration - 100% mark is "running away" as new sub activities are created.
        List<ActivityProgressInformation> leafActivities = progressInformation.getChildren().stream()
                .map(a -> {
                    List<ActivityProgressInformation> l = new ArrayList<>(List.of(a));
                    l.addAll(a.getChildren());
                    return l;
                })
                .flatMap(List::stream)
                .filter(a -> a.getChildren().isEmpty())
                .toList();

        long completed = leafActivities.stream()
                .filter(ActivityProgressInformation::isComplete)
                .count();

        return (double) completed / leafActivities.size();
    }

    @Override
    public boolean isComplete() {
        return progressInformation.isComplete();
    }

    @Override
    public OperationResultStatusType getTaskHealthStatus() {
        return workersInformation.getHealthStatus();
    }

    @Override
    public LocalizableMessage getTaskHealthDescription() {
        int executing = workersInformation.getWorkersExecuting();
        int failed = workersInformation.getWorkersFailed();
        int stalled = workersInformation.getWorkersStalled();

        if (getTask().getExecutionState() == TaskExecutionStateType.CLOSED) {
            return null;
        }

        if (executing == 0) {
            return new SingleLocalizableMessage("ActivityBasedTaskInformation.taskStatusDescription.zeroExecuting");
        }

        if (failed == 0 && stalled == 0) {
            return new SingleLocalizableMessage(
                    "ActivityBasedTaskInformation.taskStatusDescription.allExecuting",
                    new Object[] { executing });
        }

        if (failed > 0 && stalled == 0) {
            return new SingleLocalizableMessage(
                    "ActivityBasedTaskInformation.taskStatusDescription.someFailed",
                    new Object[] { failed, executing });
        }

        if (failed == 0 && stalled > 0) {
            return new SingleLocalizableMessage(
                    "ActivityBasedTaskInformation.taskStatusDescription.someStalled",
                    new Object[] { stalled, executing });
        }

        if (failed > 0 && stalled > 0) {
            return new SingleLocalizableMessage(
                    "ActivityBasedTaskInformation.taskStatusDescription.someFailedAndStalled",
                    new Object[] { failed, stalled, executing });
        }

        return null;
    }

    @Override
    public List<String> getTaskHealthMessages() {
        return workersInformation.getHealthMessages();
    }

    @Override
    public List<LocalizableMessage> getTaskHealthUserFriendlyMessages() {
        return workersInformation.getUserFriendlyHealthMessages();
    }

    @Override
    public Integer getAllErrors() {
        return progressInformation.getErrorsRecursive();
    }

    @Override
    public XMLGregorianCalendar getStartTimestamp() {
        if (isTrivial()) {
            return localRootActivityState.getRunStartTimestamp();
        } else {
            return localRootActivityState.getRealizationStartTimestamp();
        }
    }

    @Override
    public XMLGregorianCalendar getEndTimestamp() {
        if (isTrivial()) {
            return localRootActivityState.getRunEndTimestamp();
        } else {
            return localRootActivityState.getRealizationEndTimestamp();
        }
    }

    /**
     * Is the task trivial, i.e. can its start/end timestamp be taken from the execution start/end?
     *
     * The guiding condition is that each execution of a trivial task runs from the start, so they may not be bucketed,
     * distributed, composite, nor delegated.
     */
    private boolean isTrivial() {
        return !BucketingUtil.hasNonTrivialBuckets(localRootActivityState) && // not bucketed
                !BucketingUtil.isCoordinator(localRootActivityState) && // not distributed
                localRootActivityState.getActivity().isEmpty() && // not composite
                !(localRootActivityState.getWorkState() instanceof DelegationWorkStateType); // not delegated
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(super.debugDump(indent));
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "progress", progressInformation, indent + 1);
        return sb.toString();
    }

    /** We assume the token is directly in the task provided. */
    @Override
    public Object getLiveSyncToken() {
        if (task.getActivityState() == null) {
            return null;
        }
        return ActivityTreeUtil.getAllLocalStates(task.getActivityState()).stream()
                .filter(s -> s.getWorkState() instanceof LiveSyncWorkStateType)
                .map(s -> ((LiveSyncWorkStateType) s.getWorkState()).getToken())
                .filter(Objects::nonNull)
                .findFirst().orElse(null);
    }

    public @NotNull ActivityProgressInformation getProgressInformation() {
        return progressInformation;
    }

    @Override
    public @Nullable ActivityStatePersistenceType getRootActivityStatePersistence() {
        if (localRootActivityState == null) {
            return null;
        }

        return localRootActivityState.getPersistence();
    }

    @Override
    public TaskResultStatus getTaskUserFriendlyStatus() {
        if (overallStatus == null || overallStatus == OperationResultStatusType.UNKNOWN) {
            return TaskResultStatus.UNKNOWN;
        }

        TaskExecutionStateType executionState = task.getExecutionState();

        if (!isComplete()) {
            if (executionState == TaskExecutionStateType.SUSPENDED) {
                return TaskResultStatus.NOT_FINISHED;
            } else {
                return TaskResultStatus.IN_PROGRESS;
            }
        }

        if (overallStatus == OperationResultStatusType.FATAL_ERROR
                || overallStatus == OperationResultStatusType.PARTIAL_ERROR
                || overallStatus == OperationResultStatusType.HANDLED_ERROR) {
            return TaskResultStatus.ERROR;
        }

        return TaskResultStatus.SUCCESS;
    }
}
