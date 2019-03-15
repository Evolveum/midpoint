/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.model.impl.sync;

import com.evolveum.midpoint.model.impl.ModelConstants;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

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
	//@Autowired private CounterManager counterManager;
	@Autowired private SyncTaskHelper helper;

	private static final transient Trace LOGGER = TraceManager.getTrace(LiveSyncTaskHandler.class);

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
		
//		counterManager.registerCounter(task, true);

		OperationResult opResult = new OperationResult(OperationConstants.LIVE_SYNC);
		TaskRunResult runResult = new TaskRunResult();
		runResult.setOperationResult(opResult);

		if (task.getChannel() == null) {
			task.setChannel(SchemaConstants.CHANGE_CHANNEL_LIVE_SYNC_URI);
		}

		final String CTX = "Live Sync";

		ResourceShadowDiscriminator coords = helper.getCoords(LOGGER, task, opResult, runResult, CTX);
		if (coords == null) {
			return runResult;
		}

		int changesProcessed;

		try {
			// Calling synchronize(..) in provisioning.
			// This will detect the changes and notify model about them.
			// It will use extension of task to store synchronization state
            ModelImplUtils.clearRequestee(task);
			changesProcessed = provisioningService.synchronize(coords, task, partition, opResult);
		} catch (Throwable t) {
			helper.processException(LOGGER, t, opResult, runResult, partition, CTX);
			return runResult;
		}

        opResult.createSubresult(OperationConstants.LIVE_SYNC_STATISTICS).recordStatus(OperationResultStatus.SUCCESS, "Changes processed: " + changesProcessed);
        opResult.computeStatus();

        // This "run" is finished. But the task goes on ...
		runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);
		LOGGER.trace("LiveSyncTaskHandler.run stopping (resource {})", coords.getResourceOid());
		return runResult;
	}
	
    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.LIVE_SYNCHRONIZATION;
    }
}
