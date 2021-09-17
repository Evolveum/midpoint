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
     * If true, all statistics are automatically reset when the task run starts.
     * This occurs for normal (scheduled) starts but also e.g. after resuming.
     *
     * This flag should be set to false either for tasks that typically work in recurring mode with a short execution
     * and short scheduling period, OR for activity-based tasks that manage clearing of the task statistics themselves.
     */
    private boolean startFromZero = true;

    public StatisticsCollectionStrategy() {
    }

    public boolean isStartFromZero() {
        return startFromZero;
    }

    /**
     * Just for clarity.
     */
    @SuppressWarnings("WeakerAccess")
    public StatisticsCollectionStrategy fromZero() {
        this.startFromZero = true;
        return this;
    }

    public StatisticsCollectionStrategy fromStoredValues() {
        this.startFromZero = false;
        return this;
    }
}
