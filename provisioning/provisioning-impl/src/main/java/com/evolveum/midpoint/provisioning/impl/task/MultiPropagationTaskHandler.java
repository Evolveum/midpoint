/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.task;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.provisioning.impl.ShadowCache;
import com.evolveum.midpoint.repo.common.task.AbstractSearchIterativeTaskExecution;
import com.evolveum.midpoint.repo.common.task.AbstractSearchIterativeTaskHandler;
import com.evolveum.midpoint.repo.common.task.PartExecutionClass;
import com.evolveum.midpoint.repo.common.task.TaskExecutionClass;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.task.api.TaskWorkBucketProcessingResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketType;

/**
 * Task handler for provisioning propagation of many resources.
 *
 * The search in this task handler is somehow reversed. The task is searching for resources
 * and then the task internally looks for pending changes.
 *
 * Here we assume that there will be large number of resources, but there will be much smaller
 * number of changes.
 *
 * @author Radovan Semancik
 *
 */
@Component
@TaskExecutionClass(MultiPropagationTaskHandler.TaskExecution.class)
@PartExecutionClass(MultiPropagationTaskPartExecution.class)
public class MultiPropagationTaskHandler
        extends AbstractSearchIterativeTaskHandler
        <MultiPropagationTaskHandler, MultiPropagationTaskHandler.TaskExecution> {

    public static final String HANDLER_URI = SchemaConstants.NS_PROVISIONING_TASK + "/propagation/multi-handler-3";

    // WARNING! This task handler is efficiently singleton!
     // It is a spring bean and it is supposed to handle all search task instances
     // Therefore it must not have task-specific fields. It can only contain fields specific to
     // all tasks of a specified type

    @Autowired private ShadowCache shadowCache;

    public MultiPropagationTaskHandler() {
        super("Provisioning propagation (multi)", OperationConstants.PROVISIONING_PROPAGATION);
        reportingOptions.setPreserveStatistics(false);
        reportingOptions.setEnableSynchronizationStatistics(false);
        reportingOptions.setSkipWritingOperationExecutionRecords(true); // to avoid resource change (invalidates the caches)
    }

    @PostConstruct
    private void initialize() {
        taskManager.registerHandler(HANDLER_URI, this);
    }

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.SYSTEM;
    }

    @Override
    public String getArchetypeOid() {
        return SystemObjectsType.ARCHETYPE_SYSTEM_TASK.value();
    }

    public ShadowCache getShadowCache() {
        return shadowCache;
    }

    /** Just to make Java compiler happy. */
    protected static class TaskExecution
            extends AbstractSearchIterativeTaskExecution<MultiPropagationTaskHandler, MultiPropagationTaskHandler.TaskExecution> {

        public TaskExecution(MultiPropagationTaskHandler taskHandler,
                RunningTask localCoordinatorTask, WorkBucketType workBucket,
                TaskPartitionDefinitionType partDefinition,
                TaskWorkBucketProcessingResult previousRunResult) {
            super(taskHandler, localCoordinatorTask, workBucket, partDefinition, previousRunResult);
        }
    }
}
