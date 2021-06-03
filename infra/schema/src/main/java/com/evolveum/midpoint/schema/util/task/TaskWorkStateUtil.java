/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task;

import static java.util.Collections.singleton;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Utility methods related to task work state and work state management.
 */
public class TaskWorkStateUtil {

    public static WorkBucketType findBucketByNumber(List<WorkBucketType> buckets, int sequentialNumber) {
        return buckets.stream()
                .filter(b -> b.getSequentialNumber() == sequentialNumber)
                .findFirst().orElse(null);
    }

    // beware: do not call this on prism structure directly (it does not support setting values)
    public static void sortBucketsBySequentialNumber(List<WorkBucketType> buckets) {
        buckets.sort(Comparator.comparingInt(WorkBucketType::getSequentialNumber));
    }

    @Nullable
    public static AbstractWorkSegmentationType getWorkSegmentationConfiguration(WorkBucketsManagementType buckets) {
        if (buckets != null) {
            return MiscUtil.getFirstNonNull(
                    buckets.getNumericSegmentation(),
                    buckets.getStringSegmentation(),
                    buckets.getOidSegmentation(),
                    buckets.getExplicitSegmentation(),
                    buckets.getSegmentation());
        } else {
            return null;
        }
    }

    public static int getCompleteBucketsNumber(TaskType taskType) {
        return getCompleteBucketsNumber(taskType.getWorkState());
    }

    public static int getCompleteBucketsNumber(TaskWorkStateType workState) {
        if (workState == null) {
            return 0;
        }
        Integer max = null;
        int notComplete = 0;
        // TODO
//        for (WorkBucketType bucket : workState.getBucket()) {
//            if (max == null || bucket.getSequentialNumber() > max) {
//                max = bucket.getSequentialNumber();
//            }
//            if (bucket.getState() != WorkBucketStateType.COMPLETE) {
//                notComplete++;
//            }
//        }
        if (max == null) {
            return 0;
        } else {
            // what is not listed is assumed to be complete
            return max - notComplete;
        }
    }

    @Nullable
    public static Integer getExpectedBuckets(TaskType task) {
        return null; // TODO task.getWorkState() != null ? task.getWorkState().getNumberOfBuckets() : null;
    }

    private static Integer getFirstBucketNumber(@NotNull TaskWorkStateType workState) {
        return null; // TODO
//        return workState.getBucket().stream()
//                .map(WorkBucketType::getSequentialNumber)
//                .min(Integer::compareTo).orElse(null);
    }

    @Nullable
    public static WorkBucketType getLastBucket(List<WorkBucketType> buckets) {
        WorkBucketType lastBucket = null;
        for (WorkBucketType bucket : buckets) {
            if (lastBucket == null || lastBucket.getSequentialNumber() < bucket.getSequentialNumber()) {
                lastBucket = bucket;
            }
        }
        return lastBucket;
    }

    public static boolean hasLimitations(WorkBucketType bucket) {
        if (bucket == null || bucket.getContent() == null || bucket.getContent() instanceof NullWorkBucketContentType) {
            return false;
        }
        if (bucket.getContent() instanceof NumericIntervalWorkBucketContentType) {
            NumericIntervalWorkBucketContentType numInterval = (NumericIntervalWorkBucketContentType) bucket.getContent();
            return numInterval.getTo() != null || numInterval.getFrom() != null && !BigInteger.ZERO.equals(numInterval.getFrom());
        } else if (bucket.getContent() instanceof StringIntervalWorkBucketContentType) {
            StringIntervalWorkBucketContentType stringInterval = (StringIntervalWorkBucketContentType) bucket.getContent();
            return stringInterval.getTo() != null || stringInterval.getFrom() != null;
        } else if (bucket.getContent() instanceof StringPrefixWorkBucketContentType) {
            StringPrefixWorkBucketContentType stringPrefix = (StringPrefixWorkBucketContentType) bucket.getContent();
            return !stringPrefix.getPrefix().isEmpty();
        } else if (bucket.getContent() instanceof FilterWorkBucketContentType) {
            FilterWorkBucketContentType filtered = (FilterWorkBucketContentType) bucket.getContent();
            return !filtered.getFilter().isEmpty();
        } else if (AbstractWorkBucketContentType.class.equals(bucket.getContent().getClass())) {
            return false;
        } else {
            throw new AssertionError("Unsupported bucket content: " + bucket.getContent());
        }
    }

    @Nullable
    public static Integer getPartitionSequentialNumber(@NotNull TaskType taskType) {
        return null;// TODO
        //return taskType.getWorkManagement() != null ? taskType.getWorkManagement().getPartitionSequentialNumber() : null;
    }

    /**
     * @return True if the task is a coordinator (in the bucketing sense).
     */
    public static boolean isCoordinator(TaskType task) {
        return getKind(task) == TaskKindType.COORDINATOR;
    }

    /**
     * @return True if the task is a worker (in the bucketing sense).
     */
    public static boolean isWorker(TaskType task) {
        return getKind(task) == TaskKindType.WORKER;
    }

    /**
     * @return True if the task is a partitioned master.
     */
    public static boolean isPartitionedMaster(TaskType task) {
        return getKind(task) == TaskKindType.PARTITIONED_MASTER;
    }

    /**
     * @return Task kind: standalone, coordinator, worker, partitioned master.
     */
    @NotNull
    public static TaskKindType getKind(TaskType task) {
        return TaskKindType.STANDALONE; // TODO
//        if (task.getWorkManagement() != null && task.getWorkManagement().getTaskKind() != null) {
//            return task.getWorkManagement().getTaskKind();
//        } else {
//            return TaskKindType.STANDALONE;
//        }
    }

    public static boolean isManageableTreeRoot(TaskType taskType) {
        return isCoordinator(taskType) || isPartitionedMaster(taskType);
    }

    public static boolean isWorkStateHolder(TaskType taskType) {
        return (isCoordinator(taskType) || hasBuckets(taskType)) && !isCoordinatedWorker(taskType);
    }

    static boolean hasBuckets(TaskType taskType) {
        if (taskType.getWorkState() == null) {
            return false;
        }
        //TODO
        return false;
//        if (taskType.getWorkState().getNumberOfBuckets() != null && taskType.getWorkState().getNumberOfBuckets() > 1) {
//            return true;
//        }
//        List<WorkBucketType> buckets = taskType.getWorkState().getBucket();
//        if (buckets.size() > 1) {
//            return true;
//        } else {
//            return buckets.size() == 1 && buckets.get(0).getContent() != null;
//        }
    }

    private static boolean isCoordinatedWorker(TaskType taskType) {
        return false;//TODO
        //return taskType.getWorkManagement() != null && TaskKindType.WORKER == taskType.getWorkManagement().getTaskKind();
    }

    public static boolean isAllWorkComplete(TaskType task) {
        return task.getWorkState() != null && Boolean.TRUE.equals(task.getWorkState().isAllWorkComplete());
    }

    @NotNull
    public static List<WorkBucketType> getBuckets(@NotNull TaskWorkStateType workState, ActivityPath activityPath) {
        return getActivityWorkState(workState, activityPath)
                .getBucket();
    }

    @NotNull
    public static ActivityWorkStateType getActivityWorkState(@NotNull TaskWorkStateType workState,
            @NotNull ActivityPath activityPath) {
        return getActivityWorkState(workState,
                getWorkStatePath(workState, activityPath));
    }

    @NotNull
    public static ActivityWorkStateType getActivityWorkState(@NotNull TaskWorkStateType workState,
            @NotNull ItemPath workStatePath) {
        //noinspection unchecked
        return ((PrismContainer<ActivityWorkStateType>) workState.asPrismContainerValue()
                .findItem(workStatePath.rest(), PrismContainer.class))
                .getRealValue(ActivityWorkStateType.class);
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

    public static boolean isStandalone(TaskWorkStateType workState, ItemPath statePath) {
        BucketsProcessingRoleType bucketsProcessingRole = getBucketsProcessingRole(workState, statePath);
        return bucketsProcessingRole == null || bucketsProcessingRole == BucketsProcessingRoleType.STANDALONE;
    }

    public static BucketsProcessingRoleType getBucketsProcessingRole(TaskWorkStateType taskWorkState, ItemPath statePath) {
        return TaskWorkStateUtil.getActivityWorkState(taskWorkState, statePath)
                .getBucketsProcessingRole();
    }

    public static WorkDistributionType getWorkDistribution(ActivityDefinitionType work, String partId) {
        ActivityDefinitionType partDef = getPartDefinition(work, partId);
        return partDef != null ? partDef.getDistribution() : null;
    }

    public static String getCurrentActivityId(TaskWorkStateType workState) {
        return workState != null ? workState.getCurrentPartId() : null;
    }

    public static boolean isScavenger(TaskWorkStateType workState, ActivityPath activityPath) {
        ActivityWorkStateType partWorkState = getActivityWorkState(workState, activityPath);
        return Boolean.TRUE.equals(partWorkState.isScavenger());
    }

    public static ActivityPathType getLocalRootPathBean(TaskWorkStateType workState) {
        return workState != null ? workState.getLocalRoot() : null;
    }

    public static ActivityPath getLocalRootPath(TaskWorkStateType workState) {
        return ActivityPath.fromBean(getLocalRootPathBean(workState));
    }

    @NotNull
    public static ItemPath getWorkStatePath(@NotNull TaskWorkStateType workState, @NotNull ActivityPath activityPath) {
        ActivityPath localRootPath = getLocalRootPath(workState);
        stateCheck(activityPath.startsWith(localRootPath), "Activity (%s) is not within the local tree (%s)",
                activityPath, localRootPath);

        ActivityWorkStateType currentWorkState = workState.getActivity();
        ItemPath currentWorkStatePath = ItemPath.create(TaskType.F_WORK_STATE, TaskWorkStateType.F_ACTIVITY);
        for (String identifier : localRootPath.getIdentifiers()) {
            stateCheck(currentWorkState != null, "Current work state is not present; path = %s", currentWorkStatePath);
            List<ActivityWorkStateType> matching = currentWorkState.getActivity().stream()
                    .filter(state -> Objects.equals(state.getIdentifier(), identifier))
                    .collect(Collectors.toList());
            var context = currentWorkState;
            currentWorkState = MiscUtil.extractSingletonRequired(matching,
                    () -> new IllegalStateException("More than one matching activity work state for " + identifier + " in " + context),
                    () -> new IllegalStateException("No matching activity work state for " + identifier + " in " + context));
            stateCheck(currentWorkState.getId() != null, "Activity work state without ID: %s", currentWorkState);
            currentWorkStatePath = currentWorkStatePath.append(ActivityWorkStateType.F_ACTIVITY, currentWorkState.getId());
        }
        return currentWorkStatePath;
    }

    @NotNull
    private static List<String> getLocalRootSegments(@NotNull TaskWorkStateType workState) {
        return workState.getLocalRoot() != null ? workState.getLocalRoot().getIdentifier() : List.of();
    }

}
