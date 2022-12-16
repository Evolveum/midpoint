/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run;

import java.io.Serializable;

import com.evolveum.midpoint.repo.common.activity.definition.ActivityReportingDefinitionDefaultValuesProvider;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

/**
 * Reporting characteristics of an activity.
 */
@Experimental
public class ActivityReportingCharacteristics implements ActivityReportingDefinitionDefaultValuesProvider, Serializable, Cloneable {

    /** Whether we want to determine bucket size (used for really bucketed activities). */
    @NotNull private ActivityItemCountingOptionType determineBucketSizeDefault = ActivityItemCountingOptionType.NEVER;

    /** Whether we want to determine the overall size. */
    @NotNull private ActivityOverallItemCountingOptionType determineOverallSizeDefault =
            ActivityOverallItemCountingOptionType.WHEN_IN_REPOSITORY;

    /** How to log bucket completion. */
    @NotNull private ActivityEventLoggingOptionType bucketCompletionLoggingDefault = ActivityEventLoggingOptionType.BRIEF;

    /** How to log item completion. */
    @NotNull private final ActivityEventLoggingOptionType itemCompletionLoggingDefault = ActivityEventLoggingOptionType.NONE;

    /** Whether statistics (item, synchronization, actions executed, etc) are supported. */
    private boolean statisticsSupported = true;

    /** Whether activity progress is supported. */
    private boolean progressSupported = true;

    /**
     * True if the activity is capable of distinguishing between uncommitted and committed progress items.
     * A typical example of committing progress items is when a bucket is marked as complete: this ensures that items
     * that were processed will not be reprocessed again.
     *
     * If an activity has no progress commit points, then all progress is registered as committed.
     */
    private boolean progressCommitPointsSupported = true;

    /** Are run records (i.e. times of individual runs) supported? They are needed e.g. for throughput computation. */
    private boolean runRecordsSupported;

    /** Whether to report synchronization statistics. */
    private boolean synchronizationStatisticsSupported;

    /** Whether to report "actions executed" statistics. */
    private boolean actionsExecutedStatisticsSupported;

    /** Whether to log items whose processing ended with an error. */
    private boolean logErrors = true;

    /**
     * If true, operation execution records are NOT written.
     *
     * This is useful e.g. for multi-propagation tasks that iterate over resources
     * (because there is a questionable value of writing such records to ResourceType objects).
     *
     * And also for other tasks.
     */
    private boolean skipWritingOperationExecutionRecords;

    @Override
    public ActivityReportingCharacteristics clone() {
        try {
            return (ActivityReportingCharacteristics) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public @NotNull ActivityItemCountingOptionType getDetermineBucketSizeDefault() {
        return determineBucketSizeDefault;
    }

    public ActivityReportingCharacteristics determineBucketSizeDefault(@NotNull ActivityItemCountingOptionType value) {
        this.determineBucketSizeDefault = value;
        return this;
    }

    @Override
    public @NotNull ActivityOverallItemCountingOptionType getDetermineOverallSizeDefault() {
        return determineOverallSizeDefault;
    }

    public ActivityReportingCharacteristics determineOverallSizeDefault(@NotNull ActivityOverallItemCountingOptionType value) {
        this.determineOverallSizeDefault = value;
        return this;
    }

    @Override
    public @NotNull ActivityEventLoggingOptionType getBucketCompletionLoggingDefault() {
        return bucketCompletionLoggingDefault;
    }

    public ActivityReportingCharacteristics bucketCompletionLoggingDefault(@NotNull ActivityEventLoggingOptionType value) {
        this.bucketCompletionLoggingDefault = value;
        return this;
    }

    @Override
    public @NotNull ActivityEventLoggingOptionType getItemCompletionLoggingDefault() {
        return itemCompletionLoggingDefault;
    }

    boolean areStatisticsSupported() {
        return statisticsSupported;
    }

    public ActivityReportingCharacteristics statisticsSupported(boolean value) {
        statisticsSupported = value;
        return this;
    }

    boolean isProgressSupported() {
        return progressSupported;
    }

    public ActivityReportingCharacteristics progressSupported(boolean value) {
        progressSupported = value;
        return this;
    }

    boolean areProgressCommitPointsSupported() {
        return progressCommitPointsSupported;
    }

    public ActivityReportingCharacteristics progressCommitPointsSupported(boolean value) {
        progressCommitPointsSupported = value;
        return this;
    }

    boolean areRunRecordsSupported() {
        return statisticsSupported && runRecordsSupported; // Not sure about combining conditions like this.
    }

    ActivityReportingCharacteristics runRecordsSupported(boolean value) {
        runRecordsSupported = value;
        return this;
    }

    public boolean areSynchronizationStatisticsSupported() {
        return synchronizationStatisticsSupported;
    }

    public ActivityReportingCharacteristics synchronizationStatisticsSupported(boolean value) {
        this.synchronizationStatisticsSupported = value;
        return this;
    }

    public boolean areActionsExecutedStatisticsSupported() {
        return actionsExecutedStatisticsSupported;
    }

    public ActivityReportingCharacteristics actionsExecutedStatisticsSupported(boolean value) {
        this.actionsExecutedStatisticsSupported = value;
        return this;
    }

    public boolean isLogErrors() {
        return logErrors;
    }

    public ActivityReportingCharacteristics logErrors(boolean value) {
        this.logErrors = value;
        return this;
    }

    public boolean isSkipWritingOperationExecutionRecords() {
        return skipWritingOperationExecutionRecords;
    }

    public ActivityReportingCharacteristics skipWritingOperationExecutionRecords(boolean value) {
        this.skipWritingOperationExecutionRecords = value;
        return this;
    }
}
