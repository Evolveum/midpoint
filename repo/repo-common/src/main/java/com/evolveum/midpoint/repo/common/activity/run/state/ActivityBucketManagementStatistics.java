/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.state;

import static com.evolveum.midpoint.schema.statistics.ActivityBucketManagementStatisticsUtil.addTo;
import static com.evolveum.midpoint.util.MiscUtil.or0;

import java.util.function.BiConsumer;
import java.util.function.Function;

import com.evolveum.midpoint.xml.ns._public.common.common_3.BucketManagementOperationStatisticsType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityBucketManagementStatisticsType;

public class ActivityBucketManagementStatistics extends Initializable {

    /** Current value. Guarded by this. */
    @NotNull private final ActivityBucketManagementStatisticsType value = new ActivityBucketManagementStatisticsType();

    ActivityBucketManagementStatistics(CurrentActivityState<?> activityState) {
    }

    public void initialize(ActivityBucketManagementStatisticsType initialValue) {
        doInitialize(() -> {
            addTo(value, initialValue);
        });
    }

    /** Returns a current value of this statistics. It is copied because of thread safety issues. */
    public synchronized ActivityBucketManagementStatisticsType getValueCopy() {
        assertInitialized();
        return value.clone(); // TODO use clone without id when migrating this to container
    }

    public synchronized void register(String situation, long totalTime, int conflictCount, long conflictWastedTime,
            int bucketWaitCount, long bucketWaitTime, int bucketsReclaimed) {
        assertInitialized();
        BucketManagementOperationStatisticsType operation = null;
        for (BucketManagementOperationStatisticsType op : value.getOperation()) {
            if (op.getName().equals(situation)) {
                operation = op;
                break;
            }
        }
        if (operation == null) {
            operation = new BucketManagementOperationStatisticsType();
            operation.setName(situation);
            value.getOperation().add(operation);
        }
        operation.setCount(or0(operation.getCount()) + 1);
        addTime(operation, totalTime, BucketManagementOperationStatisticsType::getTotalTime,
                BucketManagementOperationStatisticsType::getMinTime,
                BucketManagementOperationStatisticsType::getMaxTime,
                BucketManagementOperationStatisticsType::setTotalTime,
                BucketManagementOperationStatisticsType::setMinTime,
                BucketManagementOperationStatisticsType::setMaxTime);
        if (conflictCount > 0 || conflictWastedTime > 0) {
            operation.setConflictCount(or0(operation.getConflictCount()) + conflictCount);
            addTime(operation, conflictWastedTime,
                    BucketManagementOperationStatisticsType::getTotalWastedTime,
                    BucketManagementOperationStatisticsType::getMinWastedTime,
                    BucketManagementOperationStatisticsType::getMaxWastedTime,
                    BucketManagementOperationStatisticsType::setTotalWastedTime,
                    BucketManagementOperationStatisticsType::setMinWastedTime,
                    BucketManagementOperationStatisticsType::setMaxWastedTime);
        }
        if (bucketWaitCount > 0 || bucketsReclaimed > 0 || bucketWaitTime > 0) {
            operation.setBucketWaitCount(or0(operation.getBucketWaitCount()) + bucketWaitCount);
            operation.setBucketsReclaimed(or0(operation.getBucketsReclaimed()) + bucketsReclaimed);
            addTime(operation, bucketWaitTime, BucketManagementOperationStatisticsType::getTotalWaitTime,
                    BucketManagementOperationStatisticsType::getMinWaitTime,
                    BucketManagementOperationStatisticsType::getMaxWaitTime,
                    BucketManagementOperationStatisticsType::setTotalWaitTime,
                    BucketManagementOperationStatisticsType::setMinWaitTime,
                    BucketManagementOperationStatisticsType::setMaxWaitTime);
        }
    }

    private void addTime(BucketManagementOperationStatisticsType operation,
            long time, Function<BucketManagementOperationStatisticsType, Long> getterTotal,
            Function<BucketManagementOperationStatisticsType, Long> getterMin,
            Function<BucketManagementOperationStatisticsType, Long> getterMax,
            BiConsumer<BucketManagementOperationStatisticsType, Long> setterTotal,
            BiConsumer<BucketManagementOperationStatisticsType, Long> setterMin,
            BiConsumer<BucketManagementOperationStatisticsType, Long>  setterMax) {
        setterTotal.accept(operation, or0(getterTotal.apply(operation)) + time);
        Long min = getterMin.apply(operation);
        if (min == null || time < min) {
            setterMin.accept(operation, time);
        }
        Long max = getterMax.apply(operation);
        if (max == null || time > max) {
            setterMax.accept(operation, time);
        }
    }
}
