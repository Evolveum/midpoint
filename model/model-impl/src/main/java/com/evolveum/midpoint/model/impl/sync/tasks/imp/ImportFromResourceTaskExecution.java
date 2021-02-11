/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks.imp;

import com.evolveum.midpoint.model.impl.sync.tasks.SyncTaskHelper;
import com.evolveum.midpoint.model.impl.sync.tasks.SynchronizationObjectsFilterImpl;
import com.evolveum.midpoint.repo.common.task.AbstractTaskExecution;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.TaskException;
import com.evolveum.midpoint.task.api.TaskWorkBucketProcessingResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketType;

import javax.xml.namespace.QName;

/**
 * Execution of an import task.
 *
 * Currently contains just generic synchronization-related information (resource, object class, kind, intent).
 */
public class ImportFromResourceTaskExecution
        extends AbstractTaskExecution<ImportFromResourceTaskHandler, ImportFromResourceTaskExecution> {

    private static final Trace LOGGER = TraceManager.getTrace(ImportFromResourceTaskHandler.class);

    private SyncTaskHelper.TargetInfo targetInfo;
    private SynchronizationObjectsFilterImpl objectsFilter;

    public ImportFromResourceTaskExecution(ImportFromResourceTaskHandler taskHandler,
            RunningTask localCoordinatorTask,
            WorkBucketType workBucket,
            TaskPartitionDefinitionType partDefinition,
            TaskWorkBucketProcessingResult previousRunResult) {
        super(taskHandler, localCoordinatorTask, workBucket, partDefinition, previousRunResult);
    }

    @Override
    protected void initialize(OperationResult opResult) throws TaskException, CommunicationException, SchemaException,
            ObjectNotFoundException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        super.initialize(opResult);
        targetInfo = taskHandler.getSyncTaskHelper().getTargetInfo(LOGGER, localCoordinatorTask,
                opResult, taskHandler.getTaskTypeName());
        objectsFilter = targetInfo.getObjectFilter(localCoordinatorTask);
    }

    public String getResourceOid() {
        return targetInfo.getResource().getOid();
    }

    public QName getObjectClassName() {
        return targetInfo.getObjectClassDefinition().getTypeName();
    }

    public SynchronizationObjectsFilterImpl getObjectsFilter() {
        return objectsFilter;
    }

    public ResourceType getResource() {
        return targetInfo.getResource();
    }

    public SyncTaskHelper.TargetInfo getTargetInfo() {
        return targetInfo;
    }
}
