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

package com.evolveum.midpoint.model.impl.sync;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IterativeTaskInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationalInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationInformationType;

/**
 * @author Pavol Mederly
 */
public class TaskHandlerUtil {

    private static final transient Trace LOGGER = TraceManager.getTrace(TaskHandlerUtil.class);

    public static void initAllStatistics(Task task) {
        fetchAllStatistics(task, false, true, true);
    }

    public static void fetchAllStatistics(Task task) {
        fetchAllStatistics(task, true, true, true);
    }

    public static void fetchAllStatistics(Task task, boolean preserveStatistics, boolean enableIterationStatistics, boolean enableSynchronizationStatistics) {
        if (preserveStatistics) {
            fetchOperationalInformation(task);
            if (enableIterationStatistics) {
                fetchIterativeTaskInformation(task);
            }
            if (enableSynchronizationStatistics) {
                fetchSynchronizationInformation(task);
            }
        } else {
            if (enableIterationStatistics) {
                task.resetIterativeTaskInformation(null);
            }
            if (enableSynchronizationStatistics) {
                task.resetSynchronizationInformation(null);
            }
        }
    }

    public static void storeAllStatistics(Task task) {
        storeAllStatistics(task, true, true);
    }

    public static void storeAllStatistics(Task task, boolean enableIterationStatistics, boolean enableSynchronizationStatistics) {
        try {
            storeOperationalInformation(task);
            if (enableIterationStatistics) {
                storeIterativeTaskInformation(task);
            }
            if (enableSynchronizationStatistics) {
                storeSynchronizationInformation(task);
            }
            task.savePendingModifications(task.getResult());
        } catch (SchemaException|ObjectNotFoundException |ObjectAlreadyExistsException |RuntimeException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't store statistical information into task {}", e, task);
        }
    }

    public static void fetchOperationalInformation(Task task) {
        PrismProperty<OperationalInformationType> property = task.getExtensionProperty(SchemaConstants.MODEL_EXTENSION_OPERATIONAL_INFORMATION_PROPERTY_NAME);
        if (property == null || property.isEmpty()) {
            task.resetOperationalInformation(null);
        } else {
            task.resetOperationalInformation(property.getValue().getValue());
        }
    }

    public static void fetchSynchronizationInformation(Task task) {
        PrismProperty<SynchronizationInformationType> property = task.getExtensionProperty(SchemaConstants.MODEL_EXTENSION_SYNCHRONIZATION_INFORMATION_PROPERTY_NAME);
        if (property == null || property.isEmpty()) {
            task.resetSynchronizationInformation(null);
        } else {
            task.resetSynchronizationInformation(property.getValue().getValue());
        }
    }

    public static void fetchIterativeTaskInformation(Task task) {
        PrismProperty<IterativeTaskInformationType> property = task.getExtensionProperty(SchemaConstants.MODEL_EXTENSION_ITERATIVE_TASK_INFORMATION_PROPERTY_NAME);
        if (property == null || property.isEmpty()) {
            task.resetIterativeTaskInformation(null);
        } else {
            task.resetIterativeTaskInformation(property.getValue().getValue());
        }
    }

    public static void storeOperationalInformation(Task task) throws SchemaException {
        OperationalInformationType operationalInformationType = task.getAggregateOperationalInformation();
        task.setExtensionPropertyValue(SchemaConstants.MODEL_EXTENSION_OPERATIONAL_INFORMATION_PROPERTY_NAME, operationalInformationType);
    }

    public static void storeSynchronizationInformation(Task task) throws SchemaException {
        SynchronizationInformationType synchronizationInformationType = task.getAggregateSynchronizationInformation();
        task.setExtensionPropertyValue(SchemaConstants.MODEL_EXTENSION_SYNCHRONIZATION_INFORMATION_PROPERTY_NAME, synchronizationInformationType);
    }

    public static void storeIterativeTaskInformation(Task task) throws SchemaException {
        IterativeTaskInformationType iterativeTaskInformationType = task.getAggregateIterativeTaskInformation();
        task.setExtensionPropertyValue(SchemaConstants.MODEL_EXTENSION_ITERATIVE_TASK_INFORMATION_PROPERTY_NAME, iterativeTaskInformationType);
    }
}
