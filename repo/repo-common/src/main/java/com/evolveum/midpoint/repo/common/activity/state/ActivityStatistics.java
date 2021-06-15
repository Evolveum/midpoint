/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.state;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityBucketManagementStatisticsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityItemProcessingStatisticsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityStatisticsType;

public class ActivityStatistics {

    @NotNull private static final ItemPath ITEM_PROCESSING_STATISTICS_PATH = ItemPath.create(ActivityStateType.F_STATISTICS, ActivityStatisticsType.F_ITEM_PROCESSING);
    @NotNull private static final ItemPath BUCKET_MANAGEMENT_STATISTICS_PATH = ItemPath.create(ActivityStateType.F_STATISTICS, ActivityStatisticsType.F_BUCKET_MANAGEMENT);

    @NotNull private final ActivityState<?> activityState;

    @NotNull private final ActivityItemProcessingStatistics itemProcessing;
    @NotNull private final ActivityBucketManagementStatistics bucketManagement;

    ActivityStatistics(@NotNull ActivityState<?> activityState) {
        this.activityState = activityState;
        this.itemProcessing = new ActivityItemProcessingStatistics(activityState);
        this.bucketManagement = new ActivityBucketManagementStatistics(activityState);
    }

    public void initialize() {
        itemProcessing.initialize(getStoredItemProcessing());
        bucketManagement.initialize(getStoredBucketManagement());
    }

    public @NotNull ActivityItemProcessingStatistics getLiveItemProcessing() {
        return itemProcessing;
    }

    public @NotNull ActivityBucketManagementStatistics getLiveBucketManagement() {
        return bucketManagement;
    }

    public ActivityItemProcessingStatisticsType getStoredItemProcessing() {
        return activityState.getItemRealValueClone(ITEM_PROCESSING_STATISTICS_PATH,
                ActivityItemProcessingStatisticsType.class);
    }

    public ActivityBucketManagementStatisticsType getStoredBucketManagement() {
        return activityState.getItemRealValueClone(BUCKET_MANAGEMENT_STATISTICS_PATH,
                ActivityBucketManagementStatisticsType.class);
    }

    /**
     * Writes current values to the running task: into the memory and to repository.
     */
    void writeToTaskAsPendingModifications() throws ActivityExecutionException {
        // TODO We should use the dynamic modification approach in order to provide most current values to the task
        //  (in case of update conflicts). But let's wait for the new repo with this.
        if (activityState.getActivityExecution().supportsStatistics()) {
            activityState.setItemRealValues(ITEM_PROCESSING_STATISTICS_PATH, itemProcessing.getValueCopy());
            activityState.setItemRealValues(BUCKET_MANAGEMENT_STATISTICS_PATH, bucketManagement.getValueCopy());
        }
    }

    public @NotNull ActivityState<?> getActivityState() {
        return activityState;
    }
}
