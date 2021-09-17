/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Objects;

import static com.evolveum.midpoint.util.MiscUtil.or0;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityStateOverviewProgressInformationVisibilityType.HIDDEN;

/**
 * Task progress counted in buckets.
 */
public class BucketsProgressInformation implements DebugDumpable, Serializable {

    /**
     * Number of complete buckets.
     */
    private final int completeBuckets;

    /**
     * Expected number of buckets.
     */
    private final Integer expectedBuckets;

    private BucketsProgressInformation(Integer expectedBuckets, int completeBuckets) {
        this.expectedBuckets = expectedBuckets;
        this.completeBuckets = completeBuckets;
    }

    static BucketsProgressInformation fromFullState(ActivityStateType state) {
        if (state == null || state.getBucketing() == null) {
            return null;
        } else if (state.getBucketing().getBucketsProcessingRole() == BucketsProcessingRoleType.WORKER) {
            return null; // Workers do not have complete information
        } else {
            ActivityBucketingStateType bucketing = state.getBucketing();
            return new BucketsProgressInformation(bucketing.getNumberOfBuckets(),
                    BucketingUtil.getCompleteBucketsNumber(bucketing));
        }
    }

    public static @Nullable BucketsProgressInformation fromOverview(@NotNull ActivityStateOverviewType overview) {
        if (overview.getProgressInformationVisibility() == HIDDEN) {
            return null;
        }

        BucketProgressOverviewType bucketProgress = overview.getBucketProgress();
        if (bucketProgress != null) {
            return new BucketsProgressInformation(
                    bucketProgress.getTotalBuckets(),
                    or0(bucketProgress.getCompleteBuckets()));
        } else {
            return null;
        }
    }

    public Integer getExpectedBuckets() {
        return expectedBuckets;
    }

    public int getCompleteBuckets() {
        return completeBuckets;
    }

    @Override
    public String toString() {
        return "BucketsProgressInformation{" +
                "completeBuckets=" + completeBuckets +
                ", expectedBuckets=" + expectedBuckets +
                '}';
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpWithLabelLn(sb, "Complete buckets", completeBuckets, indent);
        DebugUtil.debugDumpWithLabel(sb, "Expected buckets", expectedBuckets, indent);
        return sb.toString();
    }

    public float getPercentage() {
        if (expectedBuckets != null && expectedBuckets > 0) {
            return (float) getCompleteBuckets() / expectedBuckets;
        } else {
            return Float.NaN;
        }
    }

    public void checkConsistence() {
        stateCheck(expectedBuckets == null || completeBuckets <= expectedBuckets,
                "There are more completed buckets (%d) than expected buckets (%d)", completeBuckets, expectedBuckets);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BucketsProgressInformation that = (BucketsProgressInformation) o;
        return completeBuckets == that.completeBuckets && Objects.equals(expectedBuckets, that.expectedBuckets);
    }

    @Override
    public int hashCode() {
        return Objects.hash(completeBuckets, expectedBuckets);
    }
}
