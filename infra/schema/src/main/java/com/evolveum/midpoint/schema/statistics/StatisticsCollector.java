/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

/**
 * An object that receives various statistics and state information, processes them and provides
 * them back to appropriate clients.
 *
 * Currently this functionality is bound to Task interface. However, this may change in the future.
 */
public interface StatisticsCollector
        extends ProvisioningStatisticsCollector, NotificationStatisticsCollector, MappingStatisticsCollector,
        IterativeOperationCollector, ObjectActionsCollector, SynchronizationInformationCollector {

    /**
     * Records a state message.
     */
    void recordStateMessage(String message);

}
