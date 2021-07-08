/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.List;

/**
 * Methods related to bucketing part of an activity state and activity distribution definition.
 */
public class BucketingUtil {

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

    static int getCompleteBucketsNumber(ActivityBucketingStateType bucketing) {
        if (bucketing == null) {
            return 0;
        }
        Integer max = null;
        int notComplete = 0;
        for (WorkBucketType bucket : bucketing.getBucket()) {
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
    public static boolean isCoordinator(ActivityStateType state) {
        return state != null &&
                state.getBucketing() != null &&
                state.getBucketing().getBucketsProcessingRole() == BucketsProcessingRoleType.COORDINATOR;
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

    public static BucketsProcessingRoleType getBucketsProcessingRole(TaskActivityStateType taskState, ItemPath stateItemPath) {
        ActivityBucketingStateType bucketing = ActivityStateUtil.getActivityStateRequired(taskState, stateItemPath)
                .getBucketing();
        return bucketing != null ? bucketing.getBucketsProcessingRole() : null;
    }

    public static boolean isStandalone(TaskActivityStateType taskState, ItemPath stateItemPath) {
        BucketsProcessingRoleType bucketsProcessingRole = getBucketsProcessingRole(taskState, stateItemPath);
        return bucketsProcessingRole == null || bucketsProcessingRole == BucketsProcessingRoleType.STANDALONE;
    }

    public static boolean isScavenger(TaskActivityStateType taskState, ActivityPath activityPath) {
        ActivityBucketingStateType bucketing = ActivityStateUtil.getActivityStateRequired(taskState, activityPath).getBucketing();
        return bucketing != null && Boolean.TRUE.equals(bucketing.isScavenger());
    }

    public static boolean isWorkComplete(ActivityStateType state) {
        return state != null && state.getBucketing() != null && Boolean.TRUE.equals(state.getBucketing().isWorkComplete());
    }
}
