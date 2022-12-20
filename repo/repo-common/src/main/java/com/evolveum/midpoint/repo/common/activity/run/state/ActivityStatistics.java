/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.state;

import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.repo.common.activity.run.state.actions.ActionsExecutedCollectorImpl;
import com.evolveum.midpoint.repo.common.activity.run.state.actions.ActivityActionsExecuted;
import com.evolveum.midpoint.repo.common.activity.run.state.sync.ActivitySynchronizationStatistics;
import com.evolveum.midpoint.repo.common.activity.run.state.sync.SynchronizationStatisticsCollectorImpl;
import com.evolveum.midpoint.task.api.Task;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.path.ItemPath;

public class ActivityStatistics {

    @NotNull private static final ItemPath ITEM_PROCESSING_STATISTICS_PATH =
            ItemPath.create(ActivityStateType.F_STATISTICS, ActivityStatisticsType.F_ITEM_PROCESSING);
    @NotNull private static final ItemPath SYNCHRONIZATION_STATISTICS_PATH =
            ItemPath.create(ActivityStateType.F_STATISTICS, ActivityStatisticsType.F_SYNCHRONIZATION);
    @NotNull private static final ItemPath ACTIONS_EXECUTED_PATH =
            ItemPath.create(ActivityStateType.F_STATISTICS, ActivityStatisticsType.F_ACTIONS_EXECUTED);
    @NotNull private static final ItemPath BUCKET_MANAGEMENT_STATISTICS_PATH =
            ItemPath.create(ActivityStateType.F_STATISTICS, ActivityStatisticsType.F_BUCKET_MANAGEMENT);

    @NotNull private final CurrentActivityState<?> activityState;

    @NotNull private final ActivityItemProcessingStatistics itemProcessing;
    @NotNull private final ActivitySynchronizationStatistics synchronizationStatistics;
    @NotNull private final ActivityActionsExecuted actionsExecuted;
    @NotNull private final ActivityBucketManagementStatistics bucketManagement;

    ActivityStatistics(@NotNull CurrentActivityState<?> activityState) {
        this.activityState = activityState;
        this.itemProcessing = new ActivityItemProcessingStatistics(activityState);
        this.synchronizationStatistics = new ActivitySynchronizationStatistics();
        this.actionsExecuted = new ActivityActionsExecuted();
        this.bucketManagement = new ActivityBucketManagementStatistics(activityState);
    }

    public void initialize() {
        itemProcessing.initialize(getStoredItemProcessing());
        synchronizationStatistics.initialize(getStoredSynchronizationStatistics());
        actionsExecuted.initialize(getStoredActionsExecuted());
        bucketManagement.initialize(getStoredBucketManagement());
    }

    public @NotNull ActivityItemProcessingStatistics getLiveItemProcessing() {
        return itemProcessing;
    }

    public @NotNull ActivityBucketManagementStatistics getLiveBucketManagement() {
        return bucketManagement;
    }

    private ActivityItemProcessingStatisticsType getStoredItemProcessing() {
        return activityState.getItemRealValueClone(
                ITEM_PROCESSING_STATISTICS_PATH, ActivityItemProcessingStatisticsType.class);
    }

    private ActivitySynchronizationStatisticsType getStoredSynchronizationStatistics() {
        return activityState.getItemRealValueClone(
                SYNCHRONIZATION_STATISTICS_PATH, ActivitySynchronizationStatisticsType.class);
    }

    private ActivityActionsExecutedType getStoredActionsExecuted() {
        return activityState.getItemRealValueClone(
                ACTIONS_EXECUTED_PATH, ActivityActionsExecutedType.class);
    }

    private ActivityBucketManagementStatisticsType getStoredBucketManagement() {
        return activityState.getItemRealValueClone(
                BUCKET_MANAGEMENT_STATISTICS_PATH, ActivityBucketManagementStatisticsType.class);
    }

    /**
     * Writes current values to the running task: into the memory and to repository.
     */
    void writeToTaskAsPendingModifications() throws ActivityRunException {
        if (activityState.getActivityRun().areStatisticsSupported()) {
            activityState.setItemRealValues(ITEM_PROCESSING_STATISTICS_PATH, itemProcessing.getValueCopy());
            if (activityState.getActivityRun().areSynchronizationStatisticsSupported()) {
                activityState.setItemRealValues(SYNCHRONIZATION_STATISTICS_PATH, synchronizationStatistics.getValueCopy());
            }
            if (activityState.getActivityRun().areActionsExecutedStatisticsSupported()) {
                activityState.setItemRealValues(ACTIONS_EXECUTED_PATH, actionsExecuted.getValueCopy());
            }
            if (activityState.getActivity().getDistributionDefinition().hasBuckets()) {
                activityState.setItemRealValues(BUCKET_MANAGEMENT_STATISTICS_PATH, bucketManagement.getValueCopy());
            }
        }
    }

    public @NotNull CurrentActivityState<?> getActivityState() {
        return activityState;
    }

    public void startCollectingSynchronizationStatistics(
            @NotNull Task task, @NotNull String processingIdentifier, SynchronizationSituationType situationOnStart) {
        task.startCollectingSynchronizationStatistics(
                new SynchronizationStatisticsCollectorImpl(synchronizationStatistics, processingIdentifier, situationOnStart));
    }

    public void stopCollectingSynchronizationStatistics(@NotNull Task task, @NotNull QualifiedItemProcessingOutcomeType outcome) {
        task.stopCollectingSynchronizationStatistics(outcome);
    }

    public void startCollectingActionsExecuted(@NotNull Task task) {
        task.startCollectingActionsExecuted(
                new ActionsExecutedCollectorImpl(actionsExecuted));
    }

    public void stopCollectingActionsExecuted(@NotNull Task task) {
        task.stopCollectingActionsExecuted();
    }
}
