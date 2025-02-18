/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task;

import static java.util.Objects.requireNonNull;

import static com.evolveum.midpoint.schema.util.task.ActivityProgressInformationBuilder.InformationSource.FULL_STATE_PREFERRED;

import static java.util.Objects.requireNonNullElseGet;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.DebugUtil;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.List;
import java.util.Objects;

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
                () -> new ActivityStateType(PrismContext.get()));
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
                    new ActivityStateType(PrismContext.get()));
        }
    }

    @Override
    public String getProgressDescription(boolean longForm) {
        return progressInformation.toHumanReadableString(longForm);
    }

    @Override
    public double getProgress() {
        if (progressInformation.isComplete()){
            return 1.0;
        }

//        List<ActivityProgressInformation> children = progressInformation.getChildren();
//        long completed = children.stream().filter(ActivityProgressInformation::isComplete).count();
//        return (double) completed / children.size();

        return -1;// todo implement
    }

    @Override
    public boolean isComplete() {
        return progressInformation.isComplete();
    }

    @Override
    public LocalizableMessage getTaskStatusDescription() {
        return null;    // todo implement
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
}
