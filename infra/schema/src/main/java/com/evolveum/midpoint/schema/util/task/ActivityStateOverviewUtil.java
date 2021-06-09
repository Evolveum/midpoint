/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Nullable;

public class ActivityStateOverviewUtil {

    public static final ItemPath ACTIVITY_TREE_STATE_OVERVIEW_PATH =
            ItemPath.create(TaskType.F_ACTIVITY_STATE, TaskActivityStateType.F_TREE_OVERVIEW);

    public static @NotNull ActivityStateOverviewType findOrCreateEntry(@NotNull ActivityStateOverviewType current,
            @NotNull ActivityPath path) {
        if (path.isEmpty()) {
            return current;
        }
        return findOrCreateEntry(
                findOrCreateEntry(current, path.first()),
                path.rest());
    }

    public static @NotNull ActivityStateOverviewType findOrCreateEntry(@NotNull ActivityStateOverviewType current,
            String identifier) {
        List<ActivityStateOverviewType> matching = current.getActivity().stream()
                .filter(a -> Objects.equals(a.getIdentifier(), identifier))
                .collect(Collectors.toList());
        if (matching.isEmpty()) {
            ActivityStateOverviewType newEntry = new ActivityStateOverviewType()
                    .identifier(identifier);
            current.getActivity().add(newEntry);
            return newEntry;
        } else if (matching.size() == 1) {
            return matching.get(0);
        } else {
            throw new IllegalStateException("State overview entry " + current + " contains " + matching.size() + " entries " +
                    "for activity identifier '" + identifier + "': " + matching);
        }
    }

    public static boolean containsFailedExecution(@NotNull TaskType task) {
        return task.getActivityState() != null &&
                containsFailedExecution(task.getActivityState().getTreeOverview());
    }

    public static boolean containsFailedExecution(@Nullable ActivityStateOverviewType stateOverview) {
        return stateOverview != null &&
                (isExecutionFailed(stateOverview) ||
                        stateOverview.getActivity().stream().anyMatch(ActivityStateOverviewUtil::containsFailedExecution));
    }

    private static boolean isExecutionFailed(@NotNull ActivityStateOverviewType stateOverview) {
        return stateOverview.getExecutionState() == ActivityExecutionStateType.NOT_EXECUTING &&
                (stateOverview.getResultStatus() == OperationResultStatusType.FATAL_ERROR ||
                stateOverview.getResultStatus() == OperationResultStatusType.PARTIAL_ERROR);
    }

    @NotNull
    public static ActivityStateOverviewType getOrCreateTreeOverview(@NotNull TaskType taskBean) {
        return taskBean.getActivityState() != null && taskBean.getActivityState().getTreeOverview() != null ?
                taskBean.getActivityState().getTreeOverview() : new ActivityStateOverviewType();
    }

    public static ActivityStateOverviewType getTreeOverview(@NotNull TaskType taskBean) {
        return taskBean.getActivityState() != null ? taskBean.getActivityState().getTreeOverview() : null;
    }

    public static void clearFailedState(@NotNull ActivityStateOverviewType state) {
        doClearFailedState(state);
        state.getActivity().forEach(ActivityStateOverviewUtil::clearFailedState);
    }

    private static void doClearFailedState(@NotNull ActivityStateOverviewType state) {
        if (isExecutionFailed(state)) {
            state.setExecutionState(null);
            state.setResultStatus(null);
        }
    }
}
