/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.MoreObjects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Methods related to bucketing part of an activity state and activity distribution definition.
 */
public class BucketingUtil {

    public static WorkBucketType findBucketByNumber(List<WorkBucketType> buckets, int sequentialNumber) {
        return buckets.stream()
                .filter(b -> b.getSequentialNumber() == sequentialNumber)
                .findFirst().orElse(null);
    }

    public static @NotNull WorkBucketType findBucketByNumberRequired(List<WorkBucketType> buckets, int sequentialNumber) {
        return buckets.stream()
                .filter(b -> b.getSequentialNumber() == sequentialNumber)
                .findFirst().orElseThrow(
                        () -> new IllegalStateException("No bucket #" + sequentialNumber + " found"));
    }

    // beware: do not call this on prism structure directly (it does not support setting values)
    public static void sortBucketsBySequentialNumber(List<WorkBucketType> buckets) {
        buckets.sort(Comparator.comparingInt(WorkBucketType::getSequentialNumber));
    }

    @Nullable
    public static AbstractWorkSegmentationType getWorkSegmentationConfiguration(BucketsDefinitionType buckets) {
        if (buckets != null) {
            return MiscUtil.getFirstNonNull(
                    buckets.getNumericSegmentation(),
                    buckets.getStringSegmentation(),
                    buckets.getOidSegmentation(),
                    buckets.getExplicitSegmentation(),
                    buckets.getImplicitSegmentation(),
                    buckets.getSegmentation());
        } else {
            return null;
        }
    }

    static int getCompleteBucketsNumber(ActivityBucketingStateType bucketing) {
        if (bucketing != null) {
            return getCompleteBucketsNumber(bucketing.getBucket());
        } else {
            return 0;
        }
    }

    /** Returns the number of buckets that are marked as COMPLETE. They may be implicitly present. */
    public static int getCompleteBucketsNumber(@NotNull List<WorkBucketType> buckets) {
        Integer max = null;
        int notComplete = 0;
        for (WorkBucketType bucket : buckets) {
            if (max == null || bucket.getSequentialNumber() > max) {
                max = bucket.getSequentialNumber();
            }
            if (bucket.getState() != WorkBucketStateType.COMPLETE) {
                notComplete++;
            }
        }
        if (max == null) {
            return 0;
        } else {
            // what is not listed is assumed to be complete
            return max - notComplete;
        }
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
        } else if (bucket.getContent() instanceof StringValueWorkBucketContentType) {
            StringValueWorkBucketContentType stringValue = (StringValueWorkBucketContentType) bucket.getContent();
            return !stringValue.getValue().isEmpty();
        } else if (bucket.getContent() instanceof FilterWorkBucketContentType) {
            FilterWorkBucketContentType filtered = (FilterWorkBucketContentType) bucket.getContent();
            return !filtered.getFilter().isEmpty();
        } else if (AbstractWorkBucketContentType.class.equals(bucket.getContent().getClass())) {
            return false;
        } else {
            throw new AssertionError("Unsupported bucket content: " + bucket.getContent());
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static boolean isCoordinator(@Nullable ActivityStateType state) {
        return getBucketsProcessingRole(state) == BucketsProcessingRoleType.COORDINATOR;
    }

    public static boolean isStandalone(@Nullable ActivityStateType state) {
        return getBucketsProcessingRole(state) == BucketsProcessingRoleType.STANDALONE;
    }

    public static @NotNull List<WorkBucketType> getBuckets(@NotNull TaskActivityStateType taskState,
            @NotNull ActivityPath activityPath) {
        return getBuckets(
                ActivityStateUtil.getActivityStateRequired(taskState, activityPath));
    }

    public static @NotNull List<WorkBucketType> getBuckets(@NotNull ActivityStateType state) {
        ActivityBucketingStateType bucketing = state.getBucketing();
        return bucketing != null ? bucketing.getBucket() : List.of();
    }

    public static Integer getNumberOfBuckets(@NotNull ActivityStateType state) {
        ActivityBucketingStateType bucketing = state.getBucketing();
        return bucketing != null ? bucketing.getNumberOfBuckets() : null;
    }

    /** A little guesswork for now. */
    @SuppressWarnings("unused") // Expected to be used later.
    public static boolean hasNonTrivialBuckets(@NotNull ActivityStateType state) {
        ActivityBucketingStateType bucketing = state.getBucketing();
        if (bucketing == null) {
            return false;
        }
        if (bucketing.getNumberOfBuckets() != null && bucketing.getNumberOfBuckets() > 1) {
            return true;
        }
        List<WorkBucketType> buckets = bucketing.getBucket();
        if (buckets.size() > 1) {
            return true;
        } else {
            return buckets.size() == 1 &&
                    buckets.get(0).getContent() != null &&
                    !(buckets.get(0).getContent() instanceof NullWorkBucketContentType);
        }
    }

    private static @NotNull BucketsProcessingRoleType getBucketsProcessingRole(@Nullable ActivityStateType state) {
        ActivityBucketingStateType bucketing = state != null ? state.getBucketing() : null;
        return MoreObjects.firstNonNull(
                bucketing != null ? bucketing.getBucketsProcessingRole() : null,
                BucketsProcessingRoleType.STANDALONE);
    }

    public static boolean isScavenger(TaskActivityStateType taskState, ActivityPath activityPath) {
        ActivityBucketingStateType bucketing = ActivityStateUtil.getActivityStateRequired(taskState, activityPath).getBucketing();
        return bucketing != null && Boolean.TRUE.equals(bucketing.isScavenger());
    }

    public static boolean isInScavengingPhase(TaskActivityStateType taskState, ActivityPath activityPath) {
        ActivityBucketingStateType bucketing = ActivityStateUtil.getActivityStateRequired(taskState, activityPath).getBucketing();
        return bucketing != null && Boolean.TRUE.equals(bucketing.isScavenging());
    }

    public static boolean isWorkComplete(ActivityStateType state) {
        return state != null && state.getBucketing() != null && Boolean.TRUE.equals(state.getBucketing().isWorkComplete());
    }

    public static @Nullable String getWorkerOid(@NotNull WorkBucketType bucket) {
        return bucket.getWorkerRef() != null ? bucket.getWorkerRef().getOid() : null;
    }

    public static boolean isDelegatedTo(@NotNull WorkBucketType bucket, @NotNull String workerOid) {
        return bucket.getState() == WorkBucketStateType.DELEGATED && workerOid.equals(getWorkerOid(bucket));
    }

    public static @NotNull Set<Integer> getSequentialNumbers(@NotNull Collection<WorkBucketType> buckets) {
        return buckets.stream()
                .map(WorkBucketType::getSequentialNumber)
                .collect(Collectors.toSet());
    }
}
