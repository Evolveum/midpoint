/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.statistics;

/**
 * An object that receives various statistics and state information, processes them and provides
 * them back to appropriate clients.
 *
 * Currently this functionality is bound to Task interface. However, this may change in the future.
 */
public interface StatisticsCollector
        extends NotificationStatisticsCollector, MappingStatisticsCollector,
        TaskActionsExecutedCollector, TaskSynchronizationStatisticsCollector,
        TaskIterativeOperationCollector {

    /**
     * Records a state message.
     */
    void recordStateMessage(String message);

}
