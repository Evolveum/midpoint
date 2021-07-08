/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Utility methods related to activity state and activity work state.
 *
 * Does NOT deal with execution across task trees. See {@link ActivityTreeUtil} for that.
 */
@SuppressWarnings("WeakerAccess")
public class ActivityStateUtil {

    private static final Trace LOGGER = TraceManager.getTrace(ActivityStateUtil.class);

    /**
     * @return True if the progress of the task can be determined by looking only at the task itself.
     * Currently this is true for tasks without delegation.
     */
    public static boolean isProgressAvailableLocally(@NotNull TaskType task) {
        return !hasDelegatedActivity(task);
    }

    /**
     * Is there any local activity that is delegated?
     */
    public static boolean hasDelegatedActivity(@NotNull TaskType task) {
        return hasDelegatedActivity(task.getActivityState());
    }

    /**
     * Is there any local activity that is delegated?
     */
    public static boolean hasDelegatedActivity(@Nullable TaskActivityStateType taskActivityState) {
        return taskActivityState != null &&
                getLocalStatesStream(taskActivityState.getActivity())
                        .anyMatch(ActivityStateUtil::isDelegated);
    }

    /**
     * This is to determine if this task should be managed as a tree root (and not as a plain task).
     * We would like to see that without looking for subtasks, though. So we have to look at the activity
     * state and see if there are any delegations or distributions.
     */
    public static boolean isManageableTreeRoot(@NotNull TaskType task) {
        return hasDelegatedActivity(task) || hasLocalDistributedActivity(task);
    }

    /**
     * Is there any distributed activity in this task (locally)?
     */
    public static boolean hasLocalDistributedActivity(@NotNull TaskType task) {
        return hasLocalDistributedActivity(task.getActivityState());
    }

    /**
     * Is there any distributed activity in this task?
     */
    public static boolean hasLocalDistributedActivity(@Nullable TaskActivityStateType taskActivityState) {
        return taskActivityState != null &&
                getLocalStatesStream(taskActivityState.getActivity())
                    .anyMatch(ActivityStateUtil::isDistributed);
    }

    /**
     * Finds a state of an activity, given the activity path. Assumes local execution.
     */
    public static ActivityStateType getActivityState(@Nullable TaskActivityStateType taskState,
            @NotNull ActivityPath activityPath) {
        if (taskState != null) {
            return getActivityStateInternal(
                    taskState,
                    getStateItemPath(taskState, activityPath));
        } else {
            return null;
        }
    }

    /**
     * Finds a state of an activity, given the activity path. Assumes local execution.
     */
    public static ActivityStateType getActivityState(@NotNull TaskType task, @NotNull ActivityPath activityPath) {
        return getActivityState(task.getActivityState(), activityPath);
    }

    /**
     * Finds a state of an activity, given the state item path. Assumes local execution.
     */
    public static ActivityStateType getActivityState(@NotNull TaskType task, @NotNull ItemPath stateItemPath) {
        TaskActivityStateType workState = task.getActivityState();
        if (workState != null) {
            return getActivityStateInternal(workState, stateItemPath);
        } else {
            return null;
        }
    }

    /**
     * Finds a state of an activity, given the activity path. Assumes local execution.
     * Fails if there is no state object.
     */
    public static @NotNull ActivityStateType getActivityStateRequired(@NotNull TaskActivityStateType taskState,
            @NotNull ActivityPath activityPath) {
        return getActivityStateRequired(
                taskState,
                getStateItemPath(taskState, activityPath));
    }

    /**
     * Finds a state of an activity, given the state item path. Assumes local execution.
     * Fails if there is no state object.
     */
    public static @NotNull ActivityStateType getActivityStateRequired(@NotNull TaskActivityStateType taskState,
            @NotNull ItemPath stateItemPath) {
        return MiscUtil.requireNonNull(
                getActivityStateInternal(taskState, stateItemPath),
                () -> new IllegalArgumentException("No activity state at prism item path '" + stateItemPath + "'"));
    }

    private static ActivityStateType getActivityStateInternal(@NotNull TaskActivityStateType taskState,
            @NotNull ItemPath stateItemPath) {
        Object stateObject = taskState.asPrismContainerValue().find(stateItemPath.rest());
        if (stateObject == null) {
            return null;
        } else if (stateObject instanceof PrismContainer<?>) {
            return ((PrismContainer<?>) stateObject).getRealValue(ActivityStateType.class);
        } else if (stateObject instanceof PrismContainerValue<?>) {
            //noinspection unchecked
            return ((PrismContainerValue<ActivityStateType>) stateObject).asContainerable(ActivityStateType.class);
        } else {
            throw new IllegalArgumentException("Path '" + stateItemPath + "' does not point to activity state but instead"
                    + " to an instance of " + stateObject.getClass());
        }
    }

    public static ActivityPathType getLocalRootPathBean(TaskActivityStateType taskState) {
        return taskState != null ? taskState.getLocalRoot() : null;
    }

    public static ActivityPath getLocalRootPath(TaskActivityStateType taskState) {
        return ActivityPath.fromBean(getLocalRootPathBean(taskState));
    }

    /**
     * Determines state item path for a given activity path. Assumes local execution.
     * Fails if the state is not there.
     */
    @NotNull
    public static ItemPath getStateItemPath(@NotNull TaskActivityStateType workState, @NotNull ActivityPath activityPath) {
        ActivityPath localRootPath = getLocalRootPath(workState);
        LOGGER.trace("getWorkStatePath: activityPath = {}, localRootPath = {}", activityPath, localRootPath);
        stateCheck(activityPath.startsWith(localRootPath), "Activity (%s) is not within the local tree (%s)",
                activityPath, localRootPath);

        ActivityStateType currentWorkState = workState.getActivity();
        ItemPath currentWorkStatePath = ItemPath.create(TaskType.F_ACTIVITY_STATE, TaskActivityStateType.F_ACTIVITY);
        List<String> localIdentifiers = activityPath.getIdentifiers().subList(localRootPath.size(), activityPath.size());
        for (String identifier : localIdentifiers) {
            stateCheck(currentWorkState != null, "Current work state is not present; path = %s", currentWorkStatePath);
            currentWorkState = findChildActivityStateRequired(currentWorkState, identifier);
            stateCheck(currentWorkState.getId() != null, "Activity work state without ID: %s", currentWorkState);
            currentWorkStatePath = currentWorkStatePath.append(ActivityStateType.F_ACTIVITY, currentWorkState.getId());
        }
        LOGGER.trace(" -> resulting work state path: {}", currentWorkStatePath);
        return currentWorkStatePath;
    }

    public static boolean isLocal(@NotNull ActivityPath activityPath, @NotNull TaskActivityStateType taskActivityState) {
        return activityPath.startsWith(
                getLocalRootPath(taskActivityState));
    }

    /**
     * Returns child activity state - failing if not unique or not existing.
     */
    @NotNull
    public static ActivityStateType findChildActivityStateRequired(ActivityStateType state, String identifier) {
        List<ActivityStateType> matching = state.getActivity().stream()
                .filter(child -> Objects.equals(child.getIdentifier(), identifier))
                .collect(Collectors.toList());
        return MiscUtil.extractSingletonRequired(matching,
                () -> new IllegalStateException("More than one matching activity state for " + identifier + " in " + state),
                () -> new IllegalStateException("No matching activity state for " + identifier + " in " + state));
    }

    /**
     * Returns true if the activity is complete.
     */
    public static boolean isComplete(@NotNull ActivityStateType state) {
        return state.getRealizationState() == ActivityRealizationStateType.COMPLETE;
    }

    public static boolean isDelegated(@NotNull ActivityStateType state) {
        return state.getRealizationState() == ActivityRealizationStateType.IN_PROGRESS_DELEGATED ||
                state.getWorkState() instanceof DelegationWorkStateType &&
                ((DelegationWorkStateType) state.getWorkState()).getTaskRef() != null;
    }

    public static boolean isDistributed(@NotNull ActivityStateType state) {
        return state.getRealizationState() == ActivityRealizationStateType.IN_PROGRESS_DISTRIBUTED ||
                BucketingUtil.isCoordinator(state);
    }

    public static @Nullable Object getSyncTokenRealValue(@NotNull TaskType task, @NotNull ActivityPath path)
            throws SchemaException {
        ActivityStateType activityState = getActivityState(task.getActivityState(), path);
        if (activityState == null) {
            return null;
        }
        AbstractActivityWorkStateType workState = activityState.getWorkState();
        if (workState == null) {
            return null;
        } else if (workState instanceof LiveSyncWorkStateType) {
            return ((LiveSyncWorkStateType) workState).getToken();
        } else {
            throw new SchemaException("No live sync work state present in " + task + ", activity path '" + path + "': " +
                    MiscUtil.getClass(workState));
        }
    }

    public static @Nullable Object getRootSyncTokenRealValue(@NotNull TaskType task) throws SchemaException {
        return getSyncTokenRealValue(task, ActivityPath.empty());
    }

    public static @NotNull Object getRootSyncTokenRealValueRequired(@NotNull TaskType task) throws SchemaException {
        return MiscUtil.requireNonNull(
                getSyncTokenRealValue(task, ActivityPath.empty()),
                () -> "No live sync token value in " + task);
    }

    public static Stream<ActivityStateType> getLocalStatesStream(ActivityStateType root) {
        if (root == null) {
            return Stream.empty();
        } else {
            return Stream.concat(
                    Stream.of(root),
                    root.getActivity().stream());
        }
    }
}
