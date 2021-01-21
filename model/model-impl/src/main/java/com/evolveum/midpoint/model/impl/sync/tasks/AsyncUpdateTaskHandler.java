/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks;

import com.evolveum.midpoint.model.impl.ModelConstants;
import com.evolveum.midpoint.model.impl.sync.tasks.SyncTaskHelper.TargetInfo;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.schema.constants.Channel;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Task handler for controlled processing of asynchronous updates.
 */
@Component
public class AsyncUpdateTaskHandler implements TaskHandler {

    private static final Trace LOGGER = TraceManager.getTrace(AsyncUpdateTaskHandler.class);

    public static final String HANDLER_URI = ModelConstants.NS_SYNCHRONIZATION_TASK_PREFIX + "/async-update/handler-3";

    @Autowired private TaskManager taskManager;
    @Autowired private ProvisioningService provisioningService;
    @Autowired private SyncTaskHelper helper;

    private static final String CONTEXT = "Async Update";

    @PostConstruct
    private void initialize() {
        taskManager.registerHandler(HANDLER_URI, this);
    }

    @NotNull
    @Override
    public StatisticsCollectionStrategy getStatisticsCollectionStrategy() {
        return new StatisticsCollectionStrategy()
                .fromStoredValues()
                .maintainIterationStatistics()
                .maintainSynchronizationStatistics()
                .maintainActionsExecutedStatistics();
    }

    @Override
    public TaskRunResult run(RunningTask task, TaskPartitionDefinitionType partition) {
        OperationResult opResult = new OperationResult(OperationConstants.ASYNC_UPDATE);
        TaskRunResult runResult = new TaskRunResult();
        runResult.setOperationResult(opResult);

        try {
            try {
                TargetInfo targetInfo = helper.getTargetInfo(LOGGER, task, opResult, CONTEXT);

                ModelImplUtils.clearRequestee(task); // todo is this needed?
                provisioningService.processAsynchronousUpdates(targetInfo.coords, task, opResult);
            } catch (Throwable t) {
                throw helper.convertException(t, partition);
            }

            return helper.processFinish(runResult);
        } catch (TaskException e) {
            return helper.processTaskException(e, LOGGER, CONTEXT, runResult);
        }
    }

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.ASYNCHRONOUS_UPDATE;
    }

    @Override
    public String getArchetypeOid() {
        return SystemObjectsType.ARCHETYPE_ASYNC_UPDATE_TASK.value();
    }

    @Override
    public String getDefaultChannel() {
        return Channel.ASYNC_UPDATE.getUri();
    }
}
