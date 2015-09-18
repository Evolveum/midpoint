/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.schema.statistics;

import com.evolveum.midpoint.schema.statistics.IterativeTaskInformation;
import com.evolveum.midpoint.schema.statistics.OperationalInformation;
import com.evolveum.midpoint.schema.statistics.ProvisioningOperation;
import com.evolveum.midpoint.schema.statistics.SynchronizationInformation;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IterativeTaskInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationalInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationInformationType;

import javax.xml.namespace.QName;

/**
 * An object that receives various statistics and state information, processes them and provides
 * them back to appropriate clients.
 *
 * A bit experimental. We need to think out what kind of statistics and state information we'd like to collect.
 *
 * Currently this functionality is bound to Task interface. However, this may change in the future.
 *
 * @author Pavol Mederly
 */
public interface StatisticsCollector {

    /**
     * Gets information from the current task.
     */

    OperationalInformation getOperationalInformation();

    SynchronizationInformation getSynchronizationInformation();

    IterativeTaskInformation getIterativeTaskInformation();

    /**
     * Gets information from the current task and its transient subtasks (aka worker threads).
     */

    OperationalInformationType getAggregateOperationalInformation();

    IterativeTaskInformationType getAggregateIterativeTaskInformation();

    SynchronizationInformationType getAggregateSynchronizationInformation();

    /**
     * Records various kinds of operational information.
     */

    void recordState(String message);

    void recordProvisioningOperation(String resourceOid, String resourceName, QName objectClassName, ProvisioningOperation operation, boolean success, int count, long duration);

    void recordNotificationOperation(String transportName, boolean success, long duration);

    void recordMappingOperation(String objectOid, String objectName, String mappingName, long duration);

    /**
     * Records information about iterative processing of objects.
     */

    void recordIterativeOperationStart(String objectName, String objectDisplayName, QName objectType, String objectOid);

    void recordIterativeOperationStart(ShadowType shadow);

    void recordIterativeOperationEnd(String objectName, String objectDisplayName, QName objectType, String objectOid, long started, Throwable exception);

    void recordIterativeOperationEnd(ShadowType shadow, long started, Throwable exception);

    /**
     * Records information about synchronization events.
     */

    void recordSynchronizationOperationStart(String objectName, String objectDisplayName, QName objectType, String objectOid);

    void recordSynchronizationOperationEnd(String objectName, String objectDisplayName, QName objectType, String objectOid, long started, Throwable exception, SynchronizationInformation increment);

    /**
     * Sets initial values for statistics.
     */

    void resetOperationalInformation(OperationalInformationType value);

    void resetSynchronizationInformation(SynchronizationInformationType value);

    void resetIterativeTaskInformation(IterativeTaskInformationType value);
}
