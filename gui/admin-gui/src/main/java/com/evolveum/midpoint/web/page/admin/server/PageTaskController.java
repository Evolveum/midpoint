/*
 * Copyright (c) 2010-2016 Evolveum
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

package com.evolveum.midpoint.web.page.admin.server;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskBinding;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskAddResourcesDto;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.util.TaskOperationUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.*;

/**
 * @author mederly
 */
public class PageTaskController implements Serializable {

	private static final Trace LOGGER = TraceManager.getTrace(PageTaskController.class);

	private PageTaskEdit parentPage;

	public PageTaskController(PageTaskEdit parentPage) {
		this.parentPage = parentPage;
	}

	public void deleteSyncTokenPerformed(AjaxRequestTarget target) {
		LOGGER.debug("Deleting sync token.");
		OperationResult result = new OperationResult(PageTaskEdit.OPERATION_DELETE_SYNC_TOKEN);

		Task operationTask = parentPage.createSimpleTask(PageTaskEdit.OPERATION_DELETE_SYNC_TOKEN);
		try {
			final TaskDto taskDto = parentPage.getTaskDto();
			final PrismProperty property = taskDto.getExtensionProperty(SchemaConstants.SYNC_TOKEN);
			if (property == null) {
				result.recordWarning("Token is not present in this task.");		// should be treated by isVisible
			} else {
				final ObjectDelta<? extends ObjectType> delta = (ObjectDelta<? extends ObjectType>)
						DeltaBuilder.deltaFor(TaskType.class, parentPage.getPrismContext())
								.item(new ItemPath(TaskType.F_EXTENSION, SchemaConstants.SYNC_TOKEN), property.getDefinition()).replace()
								.asObjectDelta(parentPage.getTaskDto().getOid());
				//if (LOGGER.isTraceEnabled()) {
				LOGGER.info("Deleting sync token:\n{}", delta.debugDump());
				//}
				parentPage.getModelService()
						.executeChanges(Collections.<ObjectDelta<? extends ObjectType>>singleton(delta), null, operationTask, result);
				result.recomputeStatus();
			}
		} catch (Exception ex) {
			result.recomputeStatus();
			result.recordFatalError("Couldn't delete sync token from the task.", ex);
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't delete sync token from the task.", ex);
		}
		afterStateChangingOperation(target, result);
	}

	void savePerformed(AjaxRequestTarget target) {
		LOGGER.debug("Saving new task.");
		OperationResult result = new OperationResult(PageTaskEdit.OPERATION_SAVE_TASK);

		TaskDto dto = parentPage.getTaskDto();
		Task operationTask = parentPage.createSimpleTask(PageTaskEdit.OPERATION_SAVE_TASK);
		TaskManager manager = parentPage.getTaskManager();

		try {
			// TODO rework this
			TaskType taskType = parentPage.getTaskDto().getTaskType();
			Task task = manager.createTaskInstance(taskType.asPrismObject(), result);
			TaskType originalTaskType = taskType.clone();
			updateTask(dto, task);

			final Collection<ObjectDelta<? extends ObjectType>> deltas = prepareChanges(originalTaskType, task.getTaskPrismObject().asObjectable());
			//if (LOGGER.isTraceEnabled()) {
				LOGGER.info("Saving task modifications:\n{}", DebugUtil.debugDump(deltas));
			//}
			parentPage.getModelService().executeChanges(deltas, null, operationTask, result);

			result.recomputeStatus();
		} catch (Exception ex) {
			result.recomputeStatus();
			result.recordFatalError("Couldn't save task.", ex);
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't save task modifications", ex);
		}
		afterSave(target, result);
	}

	private List<ObjectDelta<? extends ObjectType>> prepareChanges(TaskType original, TaskType updated) {
		ObjectDelta<TaskType> delta = original.asPrismObject().diff(updated.asPrismObject());
		return Collections.<ObjectDelta<? extends ObjectType>>singletonList(delta);
	}

	private Task updateTask(TaskDto dto, Task existingTask) throws SchemaException {

		existingTask.setName(WebComponentUtil.createPolyFromOrigString(dto.getName()));
		existingTask.setDescription(dto.getDescription());

		dto.getHandlerDto().updateTask(existingTask, parentPage);

		ScheduleType schedule = new ScheduleType();
		schedule.setEarliestStartTime(MiscUtil.asXMLGregorianCalendar(dto.getNotStartBefore()));
		schedule.setLatestStartTime(MiscUtil.asXMLGregorianCalendar(dto.getNotStartAfter()));
		if (existingTask.getSchedule() != null) {
			schedule.setLatestFinishTime(existingTask.getSchedule().getLatestFinishTime());
		}
		schedule.setMisfireAction(dto.getMisfireActionType());
		if (!dto.isBound() && dto.getCronSpecification() != null) {
			schedule.setCronLikePattern(dto.getCronSpecification());
		} else {
			schedule.setInterval(dto.getInterval());
		}
		if (!dto.isRecurring()) {
			existingTask.makeSingle(schedule);
		} else {
			existingTask.makeRecurring(schedule);
		}
		existingTask.setBinding(dto.isBound() ? TaskBinding.TIGHT : TaskBinding.LOOSE);
		existingTask.setThreadStopAction(dto.getThreadStopActionType());

		SchemaRegistry registry = parentPage.getPrismContext().getSchemaRegistry();

		if (dto.getWorkerThreads() != null) {
			PrismPropertyDefinition def = registry.findPropertyDefinitionByElementName(SchemaConstants.MODEL_EXTENSION_WORKER_THREADS);
			PrismProperty workerThreads = new PrismProperty(SchemaConstants.MODEL_EXTENSION_WORKER_THREADS);
			workerThreads.setDefinition(def);
			workerThreads.setRealValue(dto.getWorkerThreads());

			existingTask.setExtensionProperty(workerThreads);
		} else {
			PrismProperty workerThreads = existingTask.getExtensionProperty(SchemaConstants.MODEL_EXTENSION_WORKER_THREADS);

			if(workerThreads != null){
				existingTask.deleteExtensionProperty(workerThreads);
			}
		}

		return existingTask;
	}

	void suspendPerformed(AjaxRequestTarget target) {
		String oid = parentPage.getTaskDto().getOid();
		OperationResult result = TaskOperationUtils.suspendPerformed(parentPage.getTaskService(), Collections.singleton(oid));
		afterStateChangingOperation(target, result);
	}

	void resumePerformed(AjaxRequestTarget target) {
		String oid = parentPage.getTaskDto().getOid();
		OperationResult result = TaskOperationUtils.resumePerformed(parentPage.getTaskService(), Arrays.asList(oid));
		afterStateChangingOperation(target, result);
	}

	void runNowPerformed(AjaxRequestTarget target) {
		String oid = parentPage.getTaskDto().getOid();
		OperationResult result = TaskOperationUtils.runNowPerformed(parentPage.getTaskService(), Arrays.asList(oid));
		afterStateChangingOperation(target, result);
	}

	private void afterStateChangingOperation(AjaxRequestTarget target, OperationResult result) {
		parentPage.showResult(result);
		parentPage.refresh(target);
		target.add(parentPage.getFeedbackPanel());
	}

	private void afterSave(AjaxRequestTarget target, OperationResult result) {
		parentPage.showResult(result);
		parentPage.setEdit(false);
		parentPage.refresh(target);
		target.add(parentPage.getFeedbackPanel());
		target.add(parentPage.get(PageTaskEdit.ID_SUMMARY_PANEL));
		target.add(parentPage.get(PageTaskEdit.ID_MAIN_PANEL));
		//parentPage.setResponsePage(new PageTasks(false));
	}

	public void backPerformed(AjaxRequestTarget target) {
		parentPage.redirectBack();
	}

	public void cancelEditingPerformed(AjaxRequestTarget target) {
		if (!parentPage.isEdit()) {
			backPerformed(target);			// just for sure
			return;
		}
		parentPage.setEdit(false);
		parentPage.refresh(target);
		target.add(parentPage.getFeedbackPanel());
		target.add(parentPage.get(PageTaskEdit.ID_SUMMARY_PANEL));
		target.add(parentPage.get(PageTaskEdit.ID_MAIN_PANEL));
	}

}
