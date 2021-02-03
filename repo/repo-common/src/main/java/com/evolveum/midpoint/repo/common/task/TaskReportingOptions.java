/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import com.evolveum.midpoint.task.api.StatisticsCollectionStrategy;
import com.evolveum.midpoint.util.annotation.Experimental;

import java.io.Serializable;

/**
 * Options that drive state, progress, and error reporting of a search-iterative task.
 * Factored out to provide better separation of concerns.
 *
 * TODO finish
 */
@Experimental
public class TaskReportingOptions implements Cloneable, Serializable {

    private boolean logFinishInfo = true;
    private boolean countObjectsOnStart = true;         // todo make configurable per task instance (if necessary)
    private boolean preserveStatistics = true;
    private boolean enableIterationStatistics = true;   // beware, this controls whether task stores these statistics; see also recordIterationStatistics in AbstractSearchIterativeResultHandler
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

    public boolean isLogFinishInfo() {
        return logFinishInfo;
    }

    public void setLogFinishInfo(boolean logFinishInfo) {
        this.logFinishInfo = logFinishInfo;
    }

    public boolean isCountObjectsOnStart() {
        return countObjectsOnStart;
    }

    public void setCountObjectsOnStart(boolean countObjectsOnStart) {
        this.countObjectsOnStart = countObjectsOnStart;
    }

    public boolean isPreserveStatistics() {
        return preserveStatistics;
    }

    public void setPreserveStatistics(boolean preserveStatistics) {
        this.preserveStatistics = preserveStatistics;
    }

    public boolean isEnableIterationStatistics() {
        return enableIterationStatistics;
    }

    public void setEnableIterationStatistics(boolean enableIterationStatistics) {
        this.enableIterationStatistics = enableIterationStatistics;
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

    public boolean isSkipWritingOperationExecutionRecords() {
        return skipWritingOperationExecutionRecords;
    }

    public void setSkipWritingOperationExecutionRecords(boolean skipWritingOperationExecutionRecords) {
        this.skipWritingOperationExecutionRecords = skipWritingOperationExecutionRecords;
    }

    public StatisticsCollectionStrategy getStatisticsCollectionStrategy() {
        return new StatisticsCollectionStrategy(!isPreserveStatistics(), isEnableIterationStatistics(),
                isEnableSynchronizationStatistics(), isEnableActionsExecutedStatistics());
    }

    public TaskReportingOptions clone() {
        try {
            return (TaskReportingOptions) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }
}
