/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.api;

/**
 * Describes how task statistics (including progress and structured progress) are to be collected.
 */
public class StatisticsCollectionStrategy {

    /**
     * If true, all statistics are reset when the task starts from scratch.
     * By starting from scratch we mean e.g. when all the work is done, and the task is started.
     *
     * The usual value here is true. Notable exceptions are live sync or async update tasks.
     */
    private boolean startFromZero;

    /**
     * Whether the task should maintain synchronization statistics.
     */
    private boolean maintainSynchronizationStatistics;

    /**
     * Whether the task should maintain "actions executed" statistics.
     */
    private boolean maintainActionsExecutedStatistics;

    /**
     * Whether the task should maintain structured progress;
     */
    private boolean maintainStructuredProgress;

    public StatisticsCollectionStrategy() {
    }

    public StatisticsCollectionStrategy(boolean startFromZero, boolean maintainSynchronizationStatistics,
            boolean maintainActionsExecutedStatistics, boolean maintainStructuredProgress) {
        this.startFromZero = startFromZero;
        this.maintainSynchronizationStatistics = maintainSynchronizationStatistics;
        this.maintainActionsExecutedStatistics = maintainActionsExecutedStatistics;
        this.maintainStructuredProgress = maintainStructuredProgress;
    }

    public boolean isStartFromZero() {
        return startFromZero;
    }

    public boolean isMaintainSynchronizationStatistics() {
        return maintainSynchronizationStatistics;
    }

    public boolean isMaintainActionsExecutedStatistics() {
        return maintainActionsExecutedStatistics;
    }

    public boolean isMaintainStructuredProgress() {
        return maintainStructuredProgress;
    }

    public StatisticsCollectionStrategy fromZero() {
        this.startFromZero = true;
        return this;
    }

    public StatisticsCollectionStrategy fromStoredValues() {
        this.startFromZero = false;
        return this;
    }

    public StatisticsCollectionStrategy maintainSynchronizationStatistics() {
        this.maintainSynchronizationStatistics = true;
        return this;
    }

    public StatisticsCollectionStrategy maintainActionsExecutedStatistics() {
        this.maintainActionsExecutedStatistics = true;
        return this;
    }
}
