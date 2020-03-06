/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync;

import com.evolveum.midpoint.model.impl.ModelConstants;
import com.evolveum.midpoint.model.impl.sync.SyncTaskHelper.TargetInfo;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
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

        if (task.getChannel() == null) {
            task.setChannel(SchemaConstants.CHANGE_CHANNEL_ASYNC_UPDATE_URI);
        }

        final String ctx = "Async Update";

        TargetInfo targetInfo = helper.getTargetInfo(LOGGER, task, opResult, runResult, ctx);
        if (targetInfo == null) {
            return runResult;
        }

        try {
            ModelImplUtils.clearRequestee(task);        // todo is this needed?
            provisioningService.processAsynchronousUpdates(targetInfo.coords, task, opResult);
        } catch (RuntimeException | ObjectNotFoundException | SchemaException | CommunicationException | ConfigurationException |
                ExpressionEvaluationException e) {
            helper.processException(LOGGER, e, opResult, runResult, partition, ctx);
            return runResult;
        }

        opResult.computeStatus();
        runResult.setRunResultStatus(TaskRunResult.TaskRunResultStatus.FINISHED);
        return runResult;
    }

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.ASYNCHRONOUS_UPDATE;
    }
}
