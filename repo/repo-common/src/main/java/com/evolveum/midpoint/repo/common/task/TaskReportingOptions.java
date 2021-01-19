/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import com.evolveum.midpoint.task.api.StatisticsCollectionStrategy;

/**
 * Options that drive state, progress, and error reporting of a search-iterative task.
 * Factored out to provide better separation of concerns.
 *
 * TODO finish
 */
public class TaskReportingOptions {

    private boolean logFinishInfo = true;
    private boolean countObjectsOnStart = true;         // todo make configurable per task instance (if necessary)
    private boolean preserveStatistics = true;
    private boolean enableIterationStatistics = true;   // beware, this controls whether task stores these statistics; see also recordIterationStatistics in AbstractSearchIterativeResultHandler
    private boolean enableSynchronizationStatistics = false;
    private boolean enableActionsExecutedStatistics = true;
    private boolean logErrors = true;

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

    public StatisticsCollectionStrategy getStatisticsCollectionStrategy() {
        return new StatisticsCollectionStrategy(!isPreserveStatistics(), isEnableIterationStatistics(),
                isEnableSynchronizationStatistics(), isEnableActionsExecutedStatistics());
    }
}
