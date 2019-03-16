/*
 * Copyright (c) 2010-2019 Evolveum
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
import com.evolveum.midpoint.model.impl.sync.SyncTaskHelper.TargetInfo;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AsyncUpdateListeningActivityInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AsyncUpdateListeningActivityStatusType.ALIVE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AsyncUpdateListeningActivityStatusType.DOWN;

/**
 * Task handler for controlled processing of asynchronous updates.
 */
@Component
public class AsyncUpdateTaskHandler implements TaskHandler {

	private static final transient Trace LOGGER = TraceManager.getTrace(AsyncUpdateTaskHandler.class);

	public static final String HANDLER_URI = ModelConstants.NS_SYNCHRONIZATION_TASK_PREFIX + "/async-update/handler-3";

	private static final long LISTENER_CHECK_INTERVAL = 60000L;
	private static final long TASK_CHECK_INTERVAL = 100L;

	@Autowired private PrismContext prismContext;
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

		final String CTX = "Async Update";

		TargetInfo targetInfo = helper.getTargetInfo(LOGGER, task, opResult, runResult, CTX);
		if (targetInfo == null) {
			return runResult;
		}

		String listeningActivityHandle = null;
		try {
			ModelImplUtils.clearRequestee(task);        // todo is this needed?
			listeningActivityHandle = provisioningService.startListeningForAsyncUpdates(targetInfo.coords, task, opResult);
			LOGGER.info("Started listening for async updates on {} with handle {}", targetInfo.resource, listeningActivityHandle);

			long lastCheck = 0;

			while (task.canRun()) {
				if (System.currentTimeMillis() >= lastCheck + LISTENER_CHECK_INTERVAL) {
					lastCheck = System.currentTimeMillis();
					AsyncUpdateListeningActivityInformationType info = provisioningService
							.getAsyncUpdatesListeningActivityInformation(listeningActivityHandle, task, opResult);
					LOGGER.info("Listening activity {} state:\n{}", listeningActivityHandle,    // todo trace
							prismContext.xmlSerializer().root(new QName("info")).serializeRealValue(info));
					if (isAllDown(info)) {
						throw new SystemException("All listening activities are down, suspending the task: " + task);
					}
					String issues = getIssues(info);
					if (StringUtils.isNotEmpty(issues)) {
						LOGGER.warn("Some or all listening activities have issues: {}", issues);
					}
				}
				Thread.sleep(TASK_CHECK_INTERVAL);
			}
		} catch (RuntimeException | ObjectNotFoundException | SchemaException | CommunicationException | ConfigurationException |
				ExpressionEvaluationException | InterruptedException e) {
			helper.processException(LOGGER, e, opResult, runResult, partition, CTX);
			return runResult;
		} finally {
			if (listeningActivityHandle != null) {
				LOGGER.info("Stopping listening activity {}", listeningActivityHandle);
				provisioningService.stopListeningForAsyncUpdates(listeningActivityHandle, task, opResult);
			}
		}

		opResult.computeStatus();

		// This "run" is finished. But the task goes on ...
		runResult.setRunResultStatus(TaskRunResult.TaskRunResultStatus.FINISHED);
		return runResult;
	}

	private String getIssues(AsyncUpdateListeningActivityInformationType info) {
		return stream(info)
				.filter(i -> i.getStatus() != null && i.getStatus() != ALIVE)
				.map(i -> getName(i) + " (" + i.getStatus() + ")")
				.collect(Collectors.joining(", "));
	}

	private String getName(AsyncUpdateListeningActivityInformationType info) {
		return info.getName() != null ? info.getName() : "[unnamed]";
	}

	private boolean isAllDown(AsyncUpdateListeningActivityInformationType info) {
		return stream(info)
				.allMatch(i -> i.getStatus() == null || i.getStatus() == DOWN);
	}

	public Stream<AsyncUpdateListeningActivityInformationType> stream(AsyncUpdateListeningActivityInformationType root) {
		if (root.getSubActivity().isEmpty()) {
			return Stream.of(root);
		} else {
			return root.getSubActivity().stream()
					.map(childNode -> stream(childNode))
					.reduce(Stream.of(root), (s1, s2) -> Stream.concat(s1, s2)) ;
		}
	}

	@Override
	public String getCategoryName(Task task) {
		return TaskCategory.ASYNCHRONOUS_UPDATE;
	}
}
