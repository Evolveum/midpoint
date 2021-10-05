/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.api;

import com.evolveum.midpoint.schema.result.OperationResult;

import com.evolveum.midpoint.schema.statistics.ProgressCollector;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.NotNull;

/**
 * "Statistics collection" aspect of a running task.
 *
 * Definition: Statistics are:
 *
 * 1. operational statistics (OperationStatsType),
 * 2. higher-level information (residing in activity: progress and activity statistics),
 * 3. legacy progress.
 *
 * BEWARE: Thread-local statistics (repo, caching, ...) need to be updated ONLY from the thread to which they are related.
 * This is controlled by `updateThreadLocalStatistics` parameter. Be sure to set it to `true` only when running in appropriate
 * thread. (There is a check for that, but we should not rely on it too much.)
 *
 * The process of updating statistics is multi-level:
 *
 * 1. From thread-local structures to task.statistics field. (This is the thread-critical part.) This has to be done for
 * low-level statistics that are gathered for current thread: repo, cache, etc. See {@link #refreshThreadLocalStatistics()}.
 *
 * 2. From task.statistics to task.prism. Here the subtask aggregation usually takes place. Can be done from any thread.
 * See {@link #updateOperationStatsInTaskPrism(boolean)}.
 *
 * 3. From task.prism to the repository. This takes a lot of time, so it is driven by time interval.
 * See {@link #updateAndStoreStatisticsIntoRepository(boolean, OperationResult)} and
 * {@link #storeStatisticsIntoRepositoryIfTimePassed(Runnable, OperationResult)}
 * methods.
 *
 * Statistics collection is always started by calling {@link #startCollectingStatistics(StatisticsCollectionStrategy)} method.
 */
public interface RunningTaskStatisticsCollector extends ProgressCollector {

    /**
     * Initializes the process of collecting statistics in Statistics object embedded in the task.
     */
    void startCollectingStatistics(@NotNull StatisticsCollectionStrategy strategy);

    /**
     * Re-initializes process of collecting statistics from zero values.
     */
    void restartCollectingStatisticsFromZero();

    /**
     * Refreshes thread-local statistics held in `task.statistics` from their respective thread-local stores.
     *
     * *Call from the thread that executes the task ONLY! Otherwise wrong data might be recorded.*
     */
    void refreshThreadLocalStatistics();

    /**
     * Updates operational statistics in prism object, based on existing values in Statistics objects
     * for the current task and its lightweight subtasks. If `updateThreadLocalStatistics` is true,
     * also updates this task Statistics using thread-local collectors. (*MUST* be called from the correct thread!)
     */
    void updateOperationStatsInTaskPrism(boolean updateThreadLocalStatistics);

    /**
     * Stores statistics from `task.prism` to the repository, if the specified time interval passed.
     *
     * The time interval is there to avoid excessive repository operations. (Writing a large task can take quite a long time.)
     *
     * @param additionalUpdater A code that is called to update the task with other related data, for example
     * activity-related statistics. The code should add its changes in the form of task pending modifications
     * that will be written by the main method afterwards.
     *
     * @return true if the time passed and the update was carried out
     */
    boolean storeStatisticsIntoRepositoryIfTimePassed(Runnable additionalUpdater, OperationResult result) throws SchemaException, ObjectNotFoundException;

    /**
     * Stores statistics from `task.prism` to the repository. Costly operation.
     */
    void storeStatisticsIntoRepository(OperationResult result) throws SchemaException, ObjectNotFoundException;

    /**
     * Convenience method: Updates the statistics in `task.prism`, and stores them into the repository. Costly operation.
     */
    void updateAndStoreStatisticsIntoRepository(boolean updateThreadLocalStatistics, OperationResult result) throws SchemaException, ObjectNotFoundException;

    /**
     * Convenience method: Increments the legacy progress. Updates the statistics all the way through and stores them
     * to repo if the time interval came.
     *
     * Beware:
     *
     * 1. If structured progress is enabled, there is no point in increasing legacy progress.
     * This method is to be used only for handlers other than AbstractTaskHandler.
     *
     * 2. Because this encompasses thread-local stats update, *CALL ONLY FROM THE THREAD EXECUTING THE TASK!*
     */
    void incrementLegacyProgressAndStoreStatisticsIfTimePassed(OperationResult result) throws SchemaException, ObjectNotFoundException;

    /**
     * Sets the interval for storing statistics into the repository.
     */
    void setStatisticsRepoStoreInterval(long interval);
}
