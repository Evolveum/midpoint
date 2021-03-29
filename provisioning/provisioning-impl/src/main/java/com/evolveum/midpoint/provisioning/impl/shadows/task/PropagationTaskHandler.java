/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.shadows.task;

import javax.annotation.PostConstruct;

import com.evolveum.midpoint.repo.common.task.AbstractTaskExecution;
import com.evolveum.midpoint.repo.common.task.PartExecutionClass;
import com.evolveum.midpoint.repo.common.task.TaskExecutionClass;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.impl.shadows.ShadowsFacade;
import com.evolveum.midpoint.repo.common.task.AbstractTaskHandler;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationConstants;

/**
 * Task handler for provisioning propagation of one resource.
 *
 * We assume that there will be few resources with a lot of changes each.
 *
 * @author Radovan Semancik
 */
@Component
@TaskExecutionClass(PropagationTaskHandler.TaskExecution.class)
@PartExecutionClass(PropagationTaskPartExecution.class)
public class PropagationTaskHandler extends AbstractTaskHandler
        <PropagationTaskHandler, PropagationTaskHandler.TaskExecution> {

    public static final String HANDLER_URI = SchemaConstants.NS_PROVISIONING_TASK + "/propagation/handler-3";

    private static final Trace LOGGER = TraceManager.getTrace(PropagationTaskHandler.class);

    // WARNING! This task handler is efficiently singleton!
    // It is a spring bean and it is supposed to handle all search task instances
    // Therefore it must not have task-specific fields. It can only contain fields specific to
    // all tasks of a specified type

    @Autowired TaskManager taskManager;
    @Autowired ProvisioningService provisioningService;
    @Autowired
    ShadowsFacade shadowsFacade;

    public PropagationTaskHandler() {
        super(LOGGER, "Provisioning propagation", OperationConstants.PROVISIONING_PROPAGATION);
        globalReportingOptions.setPreserveStatistics(false);
        globalReportingOptions.setEnableSynchronizationStatistics(false);
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
            extends AbstractTaskExecution<PropagationTaskHandler, TaskExecution> {

        public TaskExecution(PropagationTaskHandler taskHandler,
                RunningTask localCoordinatorTask, WorkBucketType workBucket,
                TaskPartitionDefinitionType partDefinition,
                TaskWorkBucketProcessingResult previousRunResult) {
            super(taskHandler, localCoordinatorTask, workBucket, partDefinition, previousRunResult);
        }
    }
}
