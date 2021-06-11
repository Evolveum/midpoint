/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task;

import static java.util.Collections.singleton;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Utility methods related to task work state and work state management.
 */
public class ActivityStateUtil {

    private static final Trace LOGGER = TraceManager.getTrace(ActivityStateUtil.class);

    @Nullable
    public static Integer getPartitionSequentialNumber(@NotNull TaskType taskType) {
        return null;// TODO
        //return taskType.getWorkManagement() != null ? taskType.getWorkManagement().getPartitionSequentialNumber() : null;
    }

    /**
     * @return True if the task is a partitioned master.
     */
    public static boolean isPartitionedMaster(TaskType task) {
        return BucketingUtil.getKind(task) == TaskKindType.PARTITIONED_MASTER;
    }

    public static boolean isManageableTreeRoot(TaskType taskType) {
        return BucketingUtil.isCoordinator(taskType) || isPartitionedMaster(taskType);
    }

    public static boolean isWorkStateHolder(TaskType taskType) {
        return (BucketingUtil.isCoordinator(taskType) || BucketingUtil.hasBuckets(taskType)) && !BucketingUtil.isCoordinatedWorker(taskType);
    }

    public static boolean isAllWorkComplete(TaskType task) {
        return task.getActivityState() != null && Boolean.TRUE.equals(task.getActivityState().isAllWorkComplete());
    }

    public static ActivityStateType getActivityWorkState(@NotNull TaskType task, @NotNull ItemPath path) {
        TaskActivityStateType workState = task.getActivityState();
        if (workState != null) {
            return getActivityWorkStateInternal(workState, path);
        } else {
            return null;
        }
    }

    @NotNull
    public static ActivityStateType getActivityWorkStateRequired(@NotNull TaskActivityStateType workState,
            @NotNull ActivityPath activityPath) {
        return getActivityWorkStateRequired(
                workState,
                getWorkStatePath(workState, activityPath));
    }

    @NotNull
    public static ActivityStateType getActivityWorkStateRequired(@NotNull TaskActivityStateType workState,
            @NotNull ItemPath workStatePath) {
        return MiscUtil.requireNonNull(
                getActivityWorkStateInternal(workState, workStatePath),
                () -> new IllegalArgumentException("No activity work state at prism item path '" + workStatePath + "'"));
    }

    private static ActivityStateType getActivityWorkStateInternal(@NotNull TaskActivityStateType workState,
            @NotNull ItemPath workStatePath) {
        Object object = workState.asPrismContainerValue().find(workStatePath.rest());
        if (object == null) {
            return null;
        } else if (object instanceof PrismContainer<?>) {
            return ((PrismContainer<?>) object).getRealValue(ActivityStateType.class);
        } else if (object instanceof PrismContainerValue<?>) {
            //noinspection unchecked
            return ((PrismContainerValue<ActivityStateType>) object).asContainerable(ActivityStateType.class);
        } else {
            throw new IllegalArgumentException("Path '" + workStatePath + "' does not point to activity work state but instead"
                    + " to an instance of " + object.getClass());
        }
    }

    public static ActivityDefinitionType getPartDefinition(ActivityDefinitionType part, String partId) {
        if (part == null) {
            return null;
        } else {
            return getPartDefinition(singleton(part), partId);
        }
    }

    public static ActivityDefinitionType getPartDefinition(Collection<ActivityDefinitionType> parts, String partId) {
        for (ActivityDefinitionType partDef : parts) {
            if (java.util.Objects.equals(partDef.getIdentifier(), partId)) {
                return partDef;
            }
            if (partDef.getComposition() != null) {
                List<ActivityDefinitionType> children = partDef.getComposition().getActivity();
                ActivityDefinitionType inChildren = getPartDefinition(children, partId);
                if (inChildren != null) {
                    return inChildren;
                }
            }
        }
        return null;
    }

    public static boolean isStandalone(TaskActivityStateType workState, ItemPath statePath) {
        BucketsProcessingRoleType bucketsProcessingRole = BucketingUtil.getBucketsProcessingRole(workState, statePath);
        return bucketsProcessingRole == null || bucketsProcessingRole == BucketsProcessingRoleType.STANDALONE;
    }

    public static WorkDistributionType getWorkDistribution(ActivityDefinitionType work, String partId) {
        ActivityDefinitionType partDef = getPartDefinition(work, partId);
        return partDef != null ? partDef.getDistribution() : null;
    }

    public static String getCurrentActivityId(TaskActivityStateType workState) {
        return workState != null ? workState.getCurrentPartId() : null;
    }

    public static boolean isScavenger(TaskActivityStateType taskWorkState, ActivityPath activityPath) {
        ActivityBucketingStateType bucketing = getActivityWorkStateRequired(taskWorkState, activityPath).getBucketing();
        return bucketing != null && Boolean.TRUE.equals(bucketing.isScavenger());
    }

    public static ActivityPathType getLocalRootPathBean(TaskActivityStateType workState) {
        return workState != null ? workState.getLocalRoot() : null;
    }

    public static ActivityPath getLocalRootPath(TaskActivityStateType workState) {
        return ActivityPath.fromBean(getLocalRootPathBean(workState));
    }

    @NotNull
    public static ItemPath getWorkStatePath(@NotNull TaskActivityStateType workState, @NotNull ActivityPath activityPath) {
        ActivityPath localRootPath = getLocalRootPath(workState);
        LOGGER.trace("getWorkStatePath: activityPath = {}, localRootPath = {}", activityPath, localRootPath);
        stateCheck(activityPath.startsWith(localRootPath), "Activity (%s) is not within the local tree (%s)",
                activityPath, localRootPath);

        ActivityStateType currentWorkState = workState.getActivity();
        ItemPath currentWorkStatePath = ItemPath.create(TaskType.F_ACTIVITY_STATE, TaskActivityStateType.F_ACTIVITY);
        List<String> localIdentifiers = activityPath.getIdentifiers().subList(localRootPath.size(), activityPath.size());
        for (String identifier : localIdentifiers) {
            stateCheck(currentWorkState != null, "Current work state is not present; path = %s", currentWorkStatePath);
            currentWorkState = findActivityStateRequired(currentWorkState, identifier);
            stateCheck(currentWorkState.getId() != null, "Activity work state without ID: %s", currentWorkState);
            currentWorkStatePath = currentWorkStatePath.append(ActivityStateType.F_ACTIVITY, currentWorkState.getId());
        }
        LOGGER.trace(" -> resulting work state path: {}", currentWorkStatePath);
        return currentWorkStatePath;
    }

    @NotNull
    public static ActivityStateType findActivityStateRequired(ActivityStateType state, String identifier) {
        List<ActivityStateType> matching = state.getActivity().stream()
                .filter(child -> Objects.equals(child.getIdentifier(), identifier))
                .collect(Collectors.toList());
        return MiscUtil.extractSingletonRequired(matching,
                () -> new IllegalStateException("More than one matching activity work state for " + identifier + " in " + state),
                () -> new IllegalStateException("No matching activity work state for " + identifier + " in " + state));
    }

    @NotNull
    private static List<String> getLocalRootSegments(@NotNull TaskActivityStateType workState) {
        return workState.getLocalRoot() != null ? workState.getLocalRoot().getIdentifier() : List.of();
    }

    public static boolean isComplete(@NotNull ActivityStateType state) {
        return state.getRealizationState() == ActivityRealizationStateType.COMPLETE;
    }
}
