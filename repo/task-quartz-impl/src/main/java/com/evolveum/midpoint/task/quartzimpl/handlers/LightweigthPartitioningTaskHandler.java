/*
 * Copyright (c) 2010-2018 Evolveum
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
package com.evolveum.midpoint.task.quartzimpl.handlers;

import java.util.Comparator;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.StatisticsCollectionStrategy;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskConstants;
import com.evolveum.midpoint.task.api.TaskHandler;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerQuartzImpl;
import com.evolveum.midpoint.task.quartzimpl.TaskQuartzImpl;
import com.evolveum.midpoint.task.quartzimpl.execution.HandlerExecutor;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionsDefinitionType;

/**
 * @author katka
 *
 */
@Component
public class LightweigthPartitioningTaskHandler implements TaskHandler {
	
	private static final transient Trace LOGGER  = TraceManager.getTrace(LightweigthPartitioningTaskHandler.class);
	
	private static final String HANDLER_URI = TaskConstants.LIGHTWEIGTH_PARTITIONING_TASK_HANDLER_URI;
	
	@Autowired private PrismContext prismContext;
	@Autowired private TaskManagerQuartzImpl taskManager;
	@Autowired private HandlerExecutor handlerExecutor;
//	@Autowired private TaskManager taskManager;

	
	@PostConstruct
	private void initialize() {
		taskManager.registerHandler(HANDLER_URI, this);
	}
	
	public TaskRunResult run(Task task, TaskPartitionDefinitionType taskPartition) {
		OperationResult opResult = new OperationResult(LightweigthPartitioningTaskHandler.class.getName()+".run");
		TaskRunResult runResult = new TaskRunResult();
		
		runResult.setProgress(task.getProgress());
		runResult.setOperationResult(opResult);
		
		
		TaskPartitionsDefinitionType partitionsDefinition = task.getWorkManagement().getPartitions();
		List<TaskPartitionDefinitionType> partitions = partitionsDefinition.getPartition();
		Comparator<TaskPartitionDefinitionType> comparator =
				(partition1, partition2) -> {
					
					Validate.notNull(partition1);
					Validate.notNull(partition2);
					
					Integer index1 = partition1.getIndex();
					Integer index2 = partition2.getIndex();
					
					if (index1 == null) {
						if (index2 == null) {
							return 0;
						}
						return -1;
					}
					
					if (index2 == null) {
						return -1;
					}
					
					return index1.compareTo(index2);
				};
				
		partitions.sort(comparator);
		for (TaskPartitionDefinitionType partition : partitions) {
			TaskHandler handler = taskManager.getHandler(partition.getHandlerUri());
			TaskRunResult subHandlerResult = handlerExecutor.executeHandler((TaskQuartzImpl) task, partition, handler, opResult);
//			TaskRunResult subHandlerResult = handler.run(task, partition);
			OperationResult subHandlerOpResult = subHandlerResult.getOperationResult();
			opResult.addSubresult(subHandlerOpResult);
			if (subHandlerResult != null) {
				runResult = subHandlerResult;
				runResult.setProgress(task.getProgress());
			}
			
			if (!canContinue(task, subHandlerResult)) {
				break;
			}
			
			if (subHandlerOpResult.isError()) {
				break;
			}
		}
		
		runResult.setProgress(runResult.getProgress() + 1);
		opResult.computeStatusIfUnknown();
			
		return runResult;
	}
	
	private boolean canContinue(Task task, TaskRunResult runResult) {
		if (!task.canRun() || runResult.getRunResultStatus() == TaskRunResultStatus.INTERRUPTED) {
            // first, if a task was interrupted, we do not want to change its status
            LOGGER.trace("Task was interrupted, exiting the execution cycle. Task = {}", task);
            return true;
        } else if (runResult.getRunResultStatus() == TaskRunResultStatus.TEMPORARY_ERROR) {
            LOGGER.trace("Task encountered temporary error, continuing with the execution cycle. Task = {}", task);
            return false;
        } else if (runResult.getRunResultStatus() == TaskRunResultStatus.RESTART_REQUESTED) {
            // in case of RESTART_REQUESTED we have to get (new) current handler and restart it
            // this is implemented by pushHandler and by Quartz
            LOGGER.trace("Task returned RESTART_REQUESTED state, exiting the execution cycle. Task = {}", task);
            return true;
        } else if (runResult.getRunResultStatus() == TaskRunResultStatus.PERMANENT_ERROR) {
            LOGGER.info("Task encountered permanent error, suspending the task. Task = {}", task);
            return false;
        } else if (runResult.getRunResultStatus() == TaskRunResultStatus.FINISHED) {
            LOGGER.trace("Task handler finished, continuing with the execution cycle. Task = {}", task);
            return true;
        } else if (runResult.getRunResultStatus() == TaskRunResultStatus.IS_WAITING) {
            LOGGER.trace("Task switched to waiting state, exiting the execution cycle. Task = {}", task);
            return true;
        } else if (runResult.getRunResultStatus() == TaskRunResultStatus.FINISHED_HANDLER) {
            LOGGER.trace("Task handler finished with FINISHED_HANDLER, calling task.finishHandler() and exiting the execution cycle. Task = {}", task);
            return true;
        } else {
            throw new IllegalStateException("Invalid value for Task's runResultStatus: " + runResult.getRunResultStatus() + " for task " + task);
        }
	}
	
	@NotNull
	@Override
	public StatisticsCollectionStrategy getStatisticsCollectionStrategy() {
		return new StatisticsCollectionStrategy()
				.fromZero()
				.maintainIterationStatistics()
				.maintainSynchronizationStatistics()
				.maintainActionsExecutedStatistics();
	}

	@Override
	public String getCategoryName(Task task) {
		// TODO Auto-generated method stub
		return null;
	};
	
	
//		private void processErrorCriticality(Task task, TaskPartitionDefinitionType partitionDefinition, Throwable ex, OperationResult result) throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException, ObjectAlreadyExistsException, PreconditionViolationException {
//			CriticalityType criticality = ExceptionUtil.getCriticality(partitionDefinition.getErrorCriticality(), ex, CriticalityType.PARTIAL);
//			RepoCommonUtils.processErrorCriticality(task.getTaskType(), criticality, ex, result);
//			
//		}
}
