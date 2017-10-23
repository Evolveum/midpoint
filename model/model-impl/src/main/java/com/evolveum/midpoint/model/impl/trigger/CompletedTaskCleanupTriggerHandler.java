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
package com.evolveum.midpoint.model.impl.trigger;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * @author Radovan Semancik
 *
 */
@Component
public class CompletedTaskCleanupTriggerHandler implements TriggerHandler {

	public static final String HANDLER_URI = SchemaConstants.COMPLETED_TASK_CLEANUP_TRIGGER_HANDLER_URI;

	private static final transient Trace LOGGER = TraceManager.getTrace(CompletedTaskCleanupTriggerHandler.class);

	@Autowired private TriggerHandlerRegistry triggerHandlerRegistry;
	@Autowired private RepositoryService repositoryService;
	@Autowired private TaskManager taskManager;

	@PostConstruct
	private void initialize() {
		triggerHandlerRegistry.register(HANDLER_URI, this);
	}

	@Override
	public <O extends ObjectType> void handle(PrismObject<O> object, TriggerType trigger, Task task, OperationResult result) {
		try {
			// reload the task to minimize potential for race conflicts
			// todo use repo preconditions to implement this
			if (!(object.asObjectable() instanceof TaskType)) {
				return;
			}
			TaskType completedTask = repositoryService.getObject(TaskType.class, object.getOid(), null, result).asObjectable();
			LOGGER.trace("Checking completed task to be deleted {}", completedTask);
			if (completedTask.getExecutionStatus() != TaskExecutionStatusType.CLOSED) {
				LOGGER.debug("Task {} is not closed, not deleting it.", completedTask);
				return;
			}
			XMLGregorianCalendar completion = completedTask.getCompletionTimestamp();
			if (completion == null) {
				LOGGER.debug("Task {} has no completion timestamp, not deleting it.", completedTask);
				return;
			}
			if (completedTask.getCleanupAfterCompletion() == null) {
				LOGGER.debug("Task {} has no 'cleanup after completion' set, not deleting it.", completedTask);
				return;
			}
			completion.add(completedTask.getCleanupAfterCompletion());
			if (!XmlTypeConverter.isBeforeNow(completion)) {
				LOGGER.debug("Task {} should be deleted no earlier than {}, not deleting it.", completedTask, completion);
				// We assume there is another trigger set to the correct time. This might not be the case if the administrator
				// set 'cleanupAfterCompletion' after the task was completed. Let's jut ignore this situation.
				return;
			}
			LOGGER.debug("Deleting completed task {}", completedTask);
			taskManager.deleteTask(object.getOid(), result);
		} catch (CommonException | RuntimeException | Error e) {
			LOGGER.error(e.getMessage(), e);
		}
	}
}
