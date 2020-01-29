/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.sync;

import javax.annotation.PostConstruct;

import com.evolveum.midpoint.prism.PrismContext;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.impl.ModelConstants;
import com.evolveum.midpoint.model.impl.sync.SyncTaskHelper.TargetInfo;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.StatisticsCollectionStrategy;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.task.api.TaskHandler;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;
/**
 * The task handler for a live synchronization.
 *
 *  This handler takes care of executing live synchronization "runs". It means that the handler "run" method will
 *  be called every few seconds. The responsibility is to scan for changes that happened since the last run.
 *
 * @author Radovan Semancik
 *
 */
@Component
public class LiveSyncTaskHandler implements TaskHandler {

    public static final String HANDLER_URI = ModelConstants.NS_SYNCHRONIZATION_TASK_PREFIX + "/live-sync/handler-3";

    @Autowired private TaskManager taskManager;
    @Autowired private ProvisioningService provisioningService;
    @Autowired private PrismContext prismContext;
    @Autowired private SyncTaskHelper helper;

    private static final Trace LOGGER = TraceManager.getTrace(LiveSyncTaskHandler.class);

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
        LOGGER.trace("LiveSyncTaskHandler.run starting");


        OperationResult opResult = new OperationResult(OperationConstants.LIVE_SYNC);
        TaskRunResult runResult = new TaskRunResult();
        runResult.setOperationResult(opResult);

        if (task.getChannel() == null) {
            task.setChannel(SchemaConstants.CHANGE_CHANNEL_LIVE_SYNC_URI);
        }

        final String CTX = "Live Sync";

        TargetInfo targetInfo = helper.getTargetInfo(LOGGER, task, opResult, runResult, CTX);
        if (targetInfo == null) {
            return runResult;
        }

        int changesProcessed;

        try {
            // Calling synchronize(..) in provisioning.
            // This will detect the changes and notify model about them.
            // It will use extension of task to store synchronization state
            ModelImplUtils.clearRequestee(task);
            changesProcessed = provisioningService.synchronize(targetInfo.coords, task, partition, opResult);
        } catch (Throwable t) {
            helper.processException(LOGGER, t, opResult, runResult, partition, CTX);
            return runResult;
        }

        opResult.createSubresult(OperationConstants.LIVE_SYNC_STATISTICS).recordStatus(OperationResultStatus.SUCCESS, "Changes processed: " + changesProcessed);
        opResult.computeStatus();

        // This "run" is finished. But the task goes on ...
        runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);
        LOGGER.trace("LiveSyncTaskHandler.run stopping (resource {})", targetInfo.resource);
        return runResult;
    }

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.LIVE_SYNCHRONIZATION;
    }
}
