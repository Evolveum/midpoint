/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicReference;

import com.evolveum.midpoint.repo.common.activity.definition.ActivityReportingDefinition;
import com.evolveum.midpoint.task.api.StatisticsCollectionStrategy;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityReportingDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityEventLoggingOptionType;

import org.jetbrains.annotations.NotNull;

/**
 * Options that drive state, progress, and error reporting of a search-iterative task.
 * Factored out to provide better separation of concerns.
 *
 * TODO reconcile name with {@link ActivityReportingDefinition}
 * TODO finish
 */
@Experimental
public class ActivityReportingOptions implements Cloneable, Serializable {

    private boolean defaultDetermineExpectedTotal = true;
    private ActivityEventLoggingOptionType defaultBucketCompletionLogging = ActivityEventLoggingOptionType.BRIEF;
    private ActivityEventLoggingOptionType defaultItemCompletionLogging = ActivityEventLoggingOptionType.NONE;

    private boolean persistentStatistics;
    private boolean enableSynchronizationStatistics;
    private boolean enableActionsExecutedStatistics;
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

    /**
     * Options related to the specific task instance. Must be immutable because of the thread safety.
     *
     * Actually when it is modified, only a single thread is executing. So maybe the use of {@link AtomicReference}
     * is a bit overkill.
     */
    private final AtomicReference<ActivityReportingDefinitionType> instanceReportingOptions = new AtomicReference<>();

    public void setDefaultDetermineExpectedTotal(boolean value) {
        this.defaultDetermineExpectedTotal = value;
    }

    public ActivityReportingOptions defaultDetermineExpectedTotal(boolean value) {
        setDefaultDetermineExpectedTotal(value);
        return this;
    }

    public void setDefaultBucketCompletionLogging(ActivityEventLoggingOptionType value) {
        this.defaultBucketCompletionLogging = value;
    }

    public ActivityReportingOptions defaultBucketCompletionLogging(ActivityEventLoggingOptionType value) {
        setDefaultBucketCompletionLogging(value);
        return this;
    }

    public void setDefaultItemCompletionLogging(ActivityEventLoggingOptionType value) {
        this.defaultItemCompletionLogging = value;
    }

    public boolean isPersistentStatistics() {
        return persistentStatistics;
    }

    public void setPersistentStatistics(boolean persistentStatistics) {
        this.persistentStatistics = persistentStatistics;
    }

    public ActivityReportingOptions persistentStatistics(boolean value) {
        setPersistentStatistics(value);
        return this;
    }

    public boolean isEnableSynchronizationStatistics() {
        return enableSynchronizationStatistics;
    }

    public void setEnableSynchronizationStatistics(boolean enableSynchronizationStatistics) {
        this.enableSynchronizationStatistics = enableSynchronizationStatistics;
    }

    public ActivityReportingOptions enableSynchronizationStatistics(boolean value) {
        setEnableSynchronizationStatistics(value);
        return this;
    }

    public boolean isEnableActionsExecutedStatistics() {
        return enableActionsExecutedStatistics;
    }

    public void setEnableActionsExecutedStatistics(boolean enableActionsExecutedStatistics) {
        this.enableActionsExecutedStatistics = enableActionsExecutedStatistics;
    }

    public ActivityReportingOptions enableActionsExecutedStatistics(boolean value) {
        setEnableActionsExecutedStatistics(value);
        return this;
    }

    public boolean isLogErrors() {
        return logErrors;
    }

    public void setLogErrors(boolean logErrors) {
        this.logErrors = logErrors;
    }

    public ActivityReportingOptions logErrors(boolean value) {
        setLogErrors(value);
        return this;
    }

    boolean isSkipWritingOperationExecutionRecords() {
        return skipWritingOperationExecutionRecords;
    }

    public void setSkipWritingOperationExecutionRecords(boolean skipWritingOperationExecutionRecords) {
        this.skipWritingOperationExecutionRecords = skipWritingOperationExecutionRecords;
    }

    public ActivityReportingOptions skipWritingOperationExecutionRecords(boolean value) {
        setSkipWritingOperationExecutionRecords(value);
        return this;
    }

    public ActivityReportingOptions clone() {
        try {
            return (ActivityReportingOptions) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }

    // TODO method name
    ActivityReportingOptions cloneWithConfiguration(ActivityReportingDefinitionType configuration) {
        ActivityReportingOptions clone = clone();
        clone.applyConfiguration(configuration);
        return clone;
    }

    // TODO method name
    private void applyConfiguration(ActivityReportingDefinitionType instanceOptions) {
        if (instanceOptions != null) {
            ActivityReportingDefinitionType instanceOptionsClone = instanceOptions.clone();
            instanceOptionsClone.asPrismContainerValue().freeze();
            instanceReportingOptions.set(instanceOptionsClone);
        } else {
            instanceReportingOptions.set(null);
        }
    }

    /**
     * Temporary implementation.
     * See also {@link StatisticsCollectionStrategy#isCollectExecutions()}.
     */
    public boolean isCollectExecutions() {
        return !persistentStatistics;
    }

    @NotNull
    public ActivityEventLoggingOptionType getBucketCompletionLogging() {
        ActivityReportingDefinitionType options = instanceReportingOptions.get();
        if (options != null && options.getLogging() != null && options.getLogging().getBucketCompletion() != null) {
            return options.getLogging().getBucketCompletion();
        } else {
            return defaultBucketCompletionLogging;
        }
    }

    @NotNull
    public ActivityEventLoggingOptionType getItemCompletionLogging() {
        ActivityReportingDefinitionType options = instanceReportingOptions.get();
        if (options != null && options.getLogging() != null && options.getLogging().getItemCompletion() != null) {
            return options.getLogging().getItemCompletion();
        } else {
            return defaultItemCompletionLogging;
        }
    }

    public boolean isDetermineExpectedTotal() {
        ActivityReportingDefinitionType options = instanceReportingOptions.get();
        if (options != null && options.isDetermineExpectedTotal() != null) {
            return options.isDetermineExpectedTotal();
        } else {
            return defaultDetermineExpectedTotal;
        }
    }
}
