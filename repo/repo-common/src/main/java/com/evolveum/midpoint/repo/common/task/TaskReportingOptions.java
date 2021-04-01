/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicReference;

import com.evolveum.midpoint.task.api.StatisticsCollectionStrategy;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskLoggingOptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskReportingOptionsType;

import org.jetbrains.annotations.NotNull;

/**
 * Options that drive state, progress, and error reporting of a search-iterative task.
 * Factored out to provide better separation of concerns.
 *
 *
 * TODO finish
 */
@Experimental
public class TaskReportingOptions implements Cloneable, Serializable {

    private boolean defaultDetermineExpectedTotal = true;
    private TaskLoggingOptionType defaultBucketCompletionLogging = TaskLoggingOptionType.BRIEF;
    private TaskLoggingOptionType defaultItemCompletionLogging = TaskLoggingOptionType.NONE;

    private boolean preserveStatistics = true;
    private boolean enableSynchronizationStatistics = false;
    private boolean enableActionsExecutedStatistics = true;
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
    private final AtomicReference<TaskReportingOptionsType> instanceReportingOptions = new AtomicReference<>();

    public void setDefaultDetermineExpectedTotal(boolean value) {
        this.defaultDetermineExpectedTotal = value;
    }

    public void setDefaultBucketCompletionLogging(TaskLoggingOptionType value) {
        this.defaultBucketCompletionLogging = value;
    }

    public void setDefaultItemCompletionLogging(TaskLoggingOptionType value) {
        this.defaultItemCompletionLogging = value;
    }

    public boolean isPreserveStatistics() {
        return preserveStatistics;
    }

    public void setPreserveStatistics(boolean preserveStatistics) {
        this.preserveStatistics = preserveStatistics;
    }

    public boolean isEnableSynchronizationStatistics() {
        return enableSynchronizationStatistics;
    }

    public void setEnableSynchronizationStatistics(boolean enableSynchronizationStatistics) {
        this.enableSynchronizationStatistics = enableSynchronizationStatistics;
    }

    public boolean isEnableActionsExecutedStatistics() {
        return enableActionsExecutedStatistics;
    }

    public void setEnableActionsExecutedStatistics(boolean enableActionsExecutedStatistics) {
        this.enableActionsExecutedStatistics = enableActionsExecutedStatistics;
    }

    public boolean isLogErrors() {
        return logErrors;
    }

    public void setLogErrors(boolean logErrors) {
        this.logErrors = logErrors;
    }

    boolean isSkipWritingOperationExecutionRecords() {
        return skipWritingOperationExecutionRecords;
    }

    public void setSkipWritingOperationExecutionRecords(boolean skipWritingOperationExecutionRecords) {
        this.skipWritingOperationExecutionRecords = skipWritingOperationExecutionRecords;
    }

    StatisticsCollectionStrategy getStatisticsCollectionStrategy() {
        // Note: All of these "new" tasks use structured progress.
        return new StatisticsCollectionStrategy(!isPreserveStatistics(),
                isEnableSynchronizationStatistics(), isEnableActionsExecutedStatistics(), true);
    }

    public TaskReportingOptions clone() {
        try {
            return (TaskReportingOptions) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }

    TaskReportingOptions cloneWithConfiguration(TaskReportingOptionsType configuration) {
        TaskReportingOptions clone = clone();
        clone.applyConfiguration(configuration);
        return clone;
    }

    private void applyConfiguration(TaskReportingOptionsType instanceOptions) {
        if (instanceOptions != null) {
            TaskReportingOptionsType instanceOptionsClone = instanceOptions.clone();
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
        return !preserveStatistics;
    }

    @NotNull
    public TaskLoggingOptionType getBucketCompletionLogging() {
        TaskReportingOptionsType options = instanceReportingOptions.get();
        if (options != null && options.getLogging() != null && options.getLogging().getBucketCompletion() != null) {
            return options.getLogging().getBucketCompletion();
        } else {
            return defaultBucketCompletionLogging;
        }
    }

    @NotNull
    public TaskLoggingOptionType getItemCompletionLogging() {
        TaskReportingOptionsType options = instanceReportingOptions.get();
        if (options != null && options.getLogging() != null && options.getLogging().getItemCompletion() != null) {
            return options.getLogging().getItemCompletion();
        } else {
            return defaultItemCompletionLogging;
        }
    }

    public boolean isDetermineExpectedTotal() {
        TaskReportingOptionsType options = instanceReportingOptions.get();
        if (options != null && options.isDetermineExpectedTotal() != null) {
            return options.isDetermineExpectedTotal();
        } else {
            return defaultDetermineExpectedTotal;
        }
    }
}
