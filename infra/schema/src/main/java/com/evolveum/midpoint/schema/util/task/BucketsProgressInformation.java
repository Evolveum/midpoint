/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskWorkStateType;

import java.io.Serializable;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

/**
 * Task progress counted in buckets.
 */
public class BucketsProgressInformation implements DebugDumpable, Serializable {

    /**
     * Number of completed buckets.
     */
    private final int completedBuckets;

    /**
     * Expected number of buckets.
     */
    private final Integer expectedBuckets;

    private BucketsProgressInformation(Integer expectedBuckets, int completedBuckets) {
        this.expectedBuckets = expectedBuckets;
        this.completedBuckets = completedBuckets;
    }

    static BucketsProgressInformation fromWorkState(TaskWorkStateType workState) {
        if (workState == null) {
            return new BucketsProgressInformation(null, 0);
        } else {
            return new BucketsProgressInformation(workState.getNumberOfBuckets(),
                    TaskWorkStateUtil.getCompleteBucketsNumber(workState));
        }
    }

    public Integer getExpectedBuckets() {
        return expectedBuckets;
    }

    public int getCompletedBuckets() {
        return completedBuckets;
    }

    @Override
    public String toString() {
        return "BucketsProgressInformation{" +
                "completedBuckets=" + completedBuckets +
                ", expectedBuckets=" + expectedBuckets +
                '}';
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpWithLabelLn(sb, "Completed buckets", completedBuckets, indent);
        DebugUtil.debugDumpWithLabel(sb, "Expected buckets", expectedBuckets, indent);
        return sb.toString();
    }

    public float getPercentage() {
        if (expectedBuckets != null && expectedBuckets > 0) {
            return (float) getCompletedBuckets() / expectedBuckets;
        } else {
            return Float.NaN;
        }
    }

    public void checkConsistence() {
        stateCheck(expectedBuckets == null || completedBuckets <= expectedBuckets,
                "There are more completed buckets (%d) than expected buckets (%d)", completedBuckets, expectedBuckets);
    }
}
