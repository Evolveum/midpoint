/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.util.task.ActivityItemProcessingStatisticsUtil;
import com.evolveum.midpoint.schema.util.task.ActivityPath;

import com.evolveum.midpoint.schema.util.task.ActivityStateUtil;
import com.evolveum.midpoint.schema.util.task.TaskTreeUtil;
import com.evolveum.midpoint.util.TreeNode;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.evolveum.midpoint.schema.util.task.ActivityTreeUtil.*;

public class ActivityStatisticsUtil {

    public static ActivityStatisticsType findOrCreateMatchingInfo(
            @NotNull ActivityStatisticsType current, ActivityPath activityPath, boolean create) {
        if (activityPath.isEmpty()) {
            return current;
        }

        ActivityStatisticsType childInfo = findOrCreateMatchingInfo(current.getActivity(), activityPath.first(), create);
        return findOrCreateMatchingInfo(
                childInfo,
                activityPath.rest(),
                create);
    }

    private static ActivityStatisticsType findOrCreateMatchingInfo(List<ActivityStatisticsType> infos,
            String identifier, boolean create) {
        return findMatchingInfo(infos, identifier)
                .orElseGet(
                        () -> create ? add(infos, new ActivityStatisticsType().identifier(identifier)) : null);
    }

    private static Optional<ActivityStatisticsType> findMatchingInfo(
            @NotNull List<ActivityStatisticsType> list, String id) {
        return list.stream()
                .filter(item -> Objects.equals(item.getIdentifier(), id))
                .findFirst();
    }

    /** Like {@link List#add(Object)} but returns the value. */
    private static <T> T add(List<T> list, T value) {
        list.add(value);
        return value;
    }

    /**
     * Returns the total number of items processed in all activities in this physical task.
     * Used e.g. to provide "iterations" for task internal performance counters.
     */
    public static Integer getAllItemsProcessed(TaskActivityStateType taskActivityState) {
        if (taskActivityState != null) {
            return ActivityItemProcessingStatisticsUtil.getItemsProcessed(
                    getAllLocalStates(taskActivityState));
        } else {
            return null;
        }
    }

    /**
     * Returns the total number of failures in all activities in this physical task.
     */
    public static Integer getAllFailures(@Nullable TaskActivityStateType taskActivityState) {
        if (taskActivityState != null) {
            return ActivityItemProcessingStatisticsUtil.getItemsProcessedWithFailure(
                    getAllLocalStates(taskActivityState));
        } else {
            return null;
        }
    }

    public static List<SynchronizationSituationTransitionType> getSynchronizationTransitions(
            @NotNull TreeNode<ActivityStateInContext> tree) {
        List<SynchronizationSituationTransitionType> unmerged = tree.getAllDataDepthFirst().stream()
                .flatMap(ActivityStateInContext::getAllStatesStream)
                .flatMap(ActivityStatisticsUtil::getSynchronizationTransitionsStream)
                .collect(Collectors.toList());
        return ActivitySynchronizationStatisticsUtil.summarize(unmerged);
    }

    @NotNull
    private static Stream<SynchronizationSituationTransitionType> getSynchronizationTransitionsStream(
            @NotNull ActivityStateType state) {
        if (state == null || state.getStatistics() == null) {
            return Stream.empty();
        }

        ActivityStatisticsType statistics = state.getStatistics();

        return statistics.getSynchronization() != null ? statistics.getSynchronization().getTransition().stream() : Stream.empty();
    }

    public static List<ObjectActionsExecutedEntryType> getResultingActionsExecuted(
            @NotNull TreeNode<ActivityStateInContext> tree) {
        List<ObjectActionsExecutedEntryType> unmerged = tree.getAllDataDepthFirst().stream()
                .flatMap(ActivityStateInContext::getAllStatesStream)
                .flatMap(ActivityStatisticsUtil::getResultingActionsExecuted)
                .collect(Collectors.toList());
        return ActionsExecutedInformationUtil.summarize(unmerged);
    }

    @NotNull
    private static Stream<ObjectActionsExecutedEntryType> getResultingActionsExecuted(
            @NotNull ActivityStateType state) {
        return state.getStatistics() != null &&
                state.getStatistics().getActionsExecuted() != null ?
                state.getStatistics().getActionsExecuted().getResultingObjectActionsEntry().stream() : Stream.empty();
    }

    public static List<ObjectActionsExecutedEntryType> getAllActionsExecuted(
            @NotNull TreeNode<ActivityStateInContext> tree) {
        List<ObjectActionsExecutedEntryType> unmerged = tree.getAllDataDepthFirst().stream()
                .flatMap(ActivityStateInContext::getAllStatesStream)
                .flatMap(ActivityStatisticsUtil::getAllActionsExecuted)
                .collect(Collectors.toList());
        return ActionsExecutedInformationUtil.summarize(unmerged);
    }

    @NotNull
    private static Stream<ObjectActionsExecutedEntryType> getAllActionsExecuted(
            @NotNull ActivityStateType state) {
        return state.getStatistics() != null &&
                state.getStatistics().getActionsExecuted() != null ?
                state.getStatistics().getActionsExecuted().getObjectActionsEntry().stream() : Stream.empty();
    }

    /**
     * Returns all paths in activity states that point to the statistics.
     * Local to the current task!
     */
    public static List<ItemPath> getAllStatisticsPaths(@NotNull TaskType task) {
        return getStatePathsStream(task)
                .map(path -> path.append(ActivityStateType.F_STATISTICS))
                .collect(Collectors.toList());
    }

    public static Stream<ItemPath> getStatePathsStream(@NotNull TaskType task) {
        ItemPath rootPath = ItemPath.create(TaskType.F_ACTIVITY_STATE, TaskActivityStateType.F_ACTIVITY);
        ActivityStateType rootActivity = task.getActivityState() != null ? task.getActivityState().getActivity() : null;

        if (rootActivity == null) {
            return Stream.empty();
        } else {
            return Stream.concat(
                    Stream.of(rootPath),
                    getStatePathsStream(rootActivity.getActivity(), rootPath.append(ActivityStateType.F_ACTIVITY)));
        }
    }

    private static Stream<ItemPath> getStatePathsStream(@NotNull List<ActivityStateType> states, @NotNull ItemPath path) {
        return states.stream()
                .map(state -> path.append(state.getId()));
    }


    /**
     * Summarizes activity statistics from a task tree.
     */
    public static ActivityStatisticsType getActivityStatsFromTree(@NotNull TaskType root, @NotNull ActivityPath path) {
        ActivityStatisticsType aggregate = new ActivityStatisticsType(PrismContext.get())
                .itemProcessing(new ActivityItemProcessingStatisticsType(PrismContext.get()))
                .synchronization(new ActivitySynchronizationStatisticsType(PrismContext.get()))
                .actionsExecuted(new ActivityActionsExecutedType())
                .bucketManagement(new ActivityBucketManagementStatisticsType());

        Stream<TaskType> tasks = TaskTreeUtil.getAllTasksStream(root);
        tasks.forEach(task -> {
            ActivityStateType localState = ActivityStateUtil.getActivityState(task, path);
            if (localState != null && localState.getStatistics() != null) {
                ActivityStatisticsType localStatistics = localState.getStatistics();
                ActivityItemProcessingStatisticsUtil.addTo(aggregate.getItemProcessing(), localStatistics.getItemProcessing());
                ActivitySynchronizationStatisticsUtil.addTo(aggregate.getSynchronization(), localStatistics.getSynchronization());
                ActionsExecutedInformationUtil.addTo(aggregate.getActionsExecuted(), localStatistics.getActionsExecuted());
                ActivityBucketManagementStatisticsUtil.addTo(aggregate.getBucketManagement(), localStatistics.getBucketManagement());
            }
        });
        return aggregate;
    }

    public static String format(@Nullable ActivityStatisticsType statistics) {
        if (statistics == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (statistics.getItemProcessing() != null) {
            sb.append("Item processing\n\n")
                    .append(ActivityItemProcessingStatisticsUtil.format(statistics.getItemProcessing()))
                    .append("\n");
        }
        // The second condition (some transitions present) is a workaround for synchronization info being present
        // in task even if it's not in the repository. (Do GUI wrappers do that?)
        if (statistics.getSynchronization() != null && !statistics.getSynchronization().getTransition().isEmpty()) {
            sb.append("Synchronization\n\n")
                    .append(ActivitySynchronizationStatisticsUtil.format(statistics.getSynchronization()))
                    .append("\n");
        }
        if (statistics.getActionsExecuted() != null) {
            sb.append("Actions executed\n\n")
                    .append(ActionsExecutedInformationUtil.format(statistics.getActionsExecuted()))
                    .append("\n");
        }
        if (statistics.getBucketManagement() != null) {
            sb.append("Bucket management\n\n")
                    .append(ActivityBucketManagementStatisticsUtil.format(statistics.getBucketManagement()))
                    .append("\n");
        }
        return sb.toString();
    }

    public static @NotNull Collection<ProcessedItemType> getItemsBeingProcessed(@NotNull ActivityStateType activityState) {
        var statistics = activityState.getStatistics();
        var itemProcessing = statistics != null ? statistics.getItemProcessing() : null;
        return itemProcessing != null ? itemProcessing.getCurrent() : List.of();
    }
}
