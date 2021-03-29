/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.shadows.task;

import javax.annotation.PostConstruct;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.provisioning.impl.shadows.ShadowsFacade;
import com.evolveum.midpoint.repo.common.task.AbstractTaskExecution;
import com.evolveum.midpoint.repo.common.task.AbstractTaskHandler;
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
        extends AbstractTaskHandler
        <MultiPropagationTaskHandler, MultiPropagationTaskHandler.TaskExecution> {

    private static final Trace LOGGER = TraceManager.getTrace(MultiPropagationTaskHandler.class);

    public static final String HANDLER_URI = SchemaConstants.NS_PROVISIONING_TASK + "/propagation/multi-handler-3";

    // WARNING! This task handler is efficiently singleton!
     // It is a spring bean and it is supposed to handle all search task instances
     // Therefore it must not have task-specific fields. It can only contain fields specific to
     // all tasks of a specified type

    @Autowired private ShadowsFacade shadowsFacade;

    public MultiPropagationTaskHandler() {
        super(LOGGER, "Provisioning propagation (multi)", OperationConstants.PROVISIONING_PROPAGATION);
        globalReportingOptions.setPreserveStatistics(false);
        globalReportingOptions.setEnableSynchronizationStatistics(false);
        globalReportingOptions.setSkipWritingOperationExecutionRecords(true); // to avoid resource change (invalidates the caches)
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

    public ShadowsFacade getShadowCache() {
        return shadowsFacade;
    }

    /** Just to make Java compiler happy. */
    protected static class TaskExecution
            extends AbstractTaskExecution<MultiPropagationTaskHandler, TaskExecution> {

        public TaskExecution(MultiPropagationTaskHandler taskHandler,
                RunningTask localCoordinatorTask, WorkBucketType workBucket,
                TaskPartitionDefinitionType partDefinition,
                TaskWorkBucketProcessingResult previousRunResult) {
            super(taskHandler, localCoordinatorTask, workBucket, partDefinition, previousRunResult);
        }
    }
}
