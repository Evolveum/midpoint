/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.task;

import javax.annotation.PostConstruct;

import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.provisioning.impl.ShadowCache;
import com.evolveum.midpoint.repo.common.task.AbstractSearchIterativeTaskHandler;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;

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
public class MultiPropagationTaskHandler extends AbstractSearchIterativeTaskHandler<ResourceType, MultiPropagationResultHandler> {

    public static final String HANDLER_URI = SchemaConstants.NS_PROVISIONING_TASK + "/propagation/multi-handler-3";

    // WARNING! This task handler is efficiently singleton!
     // It is a spring bean and it is supposed to handle all search task instances
     // Therefore it must not have task-specific fields. It can only contain fields specific to
     // all tasks of a specified type

    @Autowired private TaskManager taskManager;
    @Autowired private ShadowCache shadowCache;

    private static final Trace LOGGER = TraceManager.getTrace(MultiPropagationTaskHandler.class);

    public MultiPropagationTaskHandler() {
        super("Provisioning propagation (multi)", OperationConstants.PROVISIONING_PROPAGATION);
        setLogFinishInfo(true);
        setPreserveStatistics(false);
        setEnableSynchronizationStatistics(false);
    }

    @PostConstruct
    private void initialize() {
        taskManager.registerHandler(HANDLER_URI, this);
    }

    @Override
    protected MultiPropagationResultHandler createHandler(TaskPartitionDefinitionType partition, TaskRunResult runResult, RunningTask coordinatorTask,
            OperationResult opResult) {

        MultiPropagationResultHandler handler = new MultiPropagationResultHandler(coordinatorTask, getTaskOperationPrefix(), taskManager, repositoryService, shadowCache);
        return handler;
    }

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.SYSTEM;
    }

    @Override
    protected Class<? extends ObjectType> getType(Task task) {
        return ResourceType.class;
    }

    @Override
    public String getArchetypeOid() {
        return SystemObjectsType.ARCHETYPE_SYSTEM_TASK.value();
    }
}
