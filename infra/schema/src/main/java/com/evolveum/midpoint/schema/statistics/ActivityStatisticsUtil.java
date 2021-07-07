/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

import com.evolveum.midpoint.schema.util.task.ActivityItemProcessingStatisticsUtil;
import com.evolveum.midpoint.schema.util.task.ActivityPath;

import com.evolveum.midpoint.util.TreeNode;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

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
        return state.getStatistics() != null &&
                state.getStatistics().getSynchronization() != null ?
                state.getStatistics().getSynchronization().getTransition().stream() : Stream.empty();
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
}
