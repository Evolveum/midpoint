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
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskBinding;
import com.evolveum.midpoint.task.api.TaskManager;
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

	private PageTask2 parentPage;

	public PageTaskController(PageTask2 parentPage) {
		this.parentPage = parentPage;
	}

	void savePerformed(AjaxRequestTarget target) {
		LOGGER.debug("Saving new task.");
		OperationResult result = new OperationResult(PageTask2.OPERATION_SAVE_TASK);

		TaskDto dto = parentPage.getTaskDto();
		Task operationTask = parentPage.createSimpleTask(PageTask2.OPERATION_SAVE_TASK);
		TaskManager manager = parentPage.getTaskManager();

		try {
			PrismObject<TaskType> originalTaskType = parentPage.getModelService().getObject(TaskType.class, dto.getOid(), null, operationTask, result);
			Task originalTask = manager.createTaskInstance(originalTaskType, result);
			Task updatedTask = updateTask(dto, originalTask);

			LOGGER.trace("Saving task modifications.");
			parentPage.getModelService().executeChanges(prepareChanges(updatedTask), null, operationTask, result);

			parentPage.setEdit(false);
			parentPage.setResponsePage(new PageTasks(false));
			result.recomputeStatus();
		} catch (Exception ex) {
			result.recomputeStatus();
			result.recordFatalError("Couldn't save task.", ex);
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't save task modifications", ex);
		}
		parentPage.showResult(result);
		target.add(parentPage.getFeedbackPanel());
	}

	private List<ObjectDelta<? extends ObjectType>> prepareChanges(Task updatedTask) {
		Collection<? extends ItemDelta<?,?>> modifications = updatedTask.getPendingModifications();
		List<ObjectDelta<? extends ObjectType>> retval = new ArrayList<>();
		retval.add(ObjectDelta.createModifyDelta(updatedTask.getOid(), modifications, TaskType.class, parentPage.getPrismContext()));
		return retval;
	}

	private Task updateTask(TaskDto dto, Task existingTask) throws SchemaException {

		if (!existingTask.getName().equals(dto.getName())) {
			existingTask.setName(WebComponentUtil.createPolyFromOrigString(dto.getName()));
		}   // if they are equal, modifyObject complains ... it's probably a bug in repo; we'll fix it later?

		if ((existingTask.getDescription() == null && dto.getDescription() != null) ||
				(existingTask.getDescription() != null && !existingTask.getDescription().equals(dto.getDescription()))) {
			existingTask.setDescription(dto.getDescription());
		}

		TaskAddResourcesDto resourceRefDto;
		if(dto.getResource() != null){
			resourceRefDto = dto.getResource();
			ObjectReferenceType resourceRef = new ObjectReferenceType();
			resourceRef.setOid(resourceRefDto.getOid());
			resourceRef.setType(ResourceType.COMPLEX_TYPE);
			existingTask.setObjectRef(resourceRef);
		}

		if (!dto.getRecurring()) {
			existingTask.makeSingle();
		}
		existingTask.setBinding(dto.getBound() == true ? TaskBinding.TIGHT : TaskBinding.LOOSE);

		ScheduleType schedule = new ScheduleType();

		schedule.setEarliestStartTime(MiscUtil.asXMLGregorianCalendar(dto.getNotStartBefore()));
		schedule.setLatestStartTime(MiscUtil.asXMLGregorianCalendar(dto.getNotStartAfter()));
		schedule.setMisfireAction(dto.getMisfire());
		if (existingTask.getSchedule() != null) {
			schedule.setLatestFinishTime(existingTask.getSchedule().getLatestFinishTime());
		}

		if (dto.getRecurring() == true) {

			if (dto.getBound() == false && dto.getCronSpecification() != null) {
				schedule.setCronLikePattern(dto.getCronSpecification());
			} else {
				schedule.setInterval(dto.getInterval());
			}
			existingTask.makeRecurring(schedule);
		} else {
			existingTask.makeSingle(schedule);
		}

		ThreadStopActionType tsa = dto.getThreadStop();
		//        if (tsa == null) {
		//            tsa = dto.getRunUntilNodeDown() ? ThreadStopActionType.CLOSE : ThreadStopActionType.RESTART;
		//        }
		existingTask.setThreadStopAction(tsa);

		SchemaRegistry registry = parentPage.getPrismContext().getSchemaRegistry();
		if (dto.isDryRun()) {
			PrismPropertyDefinition def = registry.findPropertyDefinitionByElementName(SchemaConstants.MODEL_EXTENSION_DRY_RUN);
			PrismProperty dryRun = new PrismProperty(SchemaConstants.MODEL_EXTENSION_DRY_RUN);
			dryRun.setDefinition(def);
			dryRun.setRealValue(true);

			existingTask.addExtensionProperty(dryRun);
		} else {
			PrismProperty dryRun = existingTask.getExtensionProperty(SchemaConstants.MODEL_EXTENSION_DRY_RUN);
			if (dryRun != null) {
				existingTask.deleteExtensionProperty(dryRun);
			}
		}

		if (dto.getKind() != null) {
			PrismPropertyDefinition def = registry.findPropertyDefinitionByElementName(SchemaConstants.MODEL_EXTENSION_KIND);
			PrismProperty kind = new PrismProperty(SchemaConstants.MODEL_EXTENSION_KIND);
			kind.setDefinition(def);
			kind.setRealValue(dto.getKind());

			existingTask.addExtensionProperty(kind);
		} else {
			PrismProperty kind = existingTask.getExtensionProperty(SchemaConstants.MODEL_EXTENSION_KIND);

			if(kind != null){
				existingTask.deleteExtensionProperty(kind);
			}
		}

		if (dto.getIntent() != null && StringUtils.isNotEmpty(dto.getIntent())) {
			PrismPropertyDefinition def = registry.findPropertyDefinitionByElementName(SchemaConstants.MODEL_EXTENSION_INTENT);
			PrismProperty intent = new PrismProperty(SchemaConstants.MODEL_EXTENSION_INTENT);
			intent.setDefinition(def);
			intent.setRealValue(dto.getIntent());

			existingTask.addExtensionProperty(intent);
		} else {
			PrismProperty intent = existingTask.getExtensionProperty(SchemaConstants.MODEL_EXTENSION_INTENT);

			if (intent != null) {
				existingTask.deleteExtensionProperty(intent);
			}
		}

		if(dto.getObjectClass() != null && StringUtils.isNotEmpty(dto.getObjectClass())){
			PrismPropertyDefinition def = registry.findPropertyDefinitionByElementName(SchemaConstants.OBJECTCLASS_PROPERTY_NAME);
			PrismProperty objectClassProperty = new PrismProperty(SchemaConstants.OBJECTCLASS_PROPERTY_NAME);
			objectClassProperty.setRealValue(def);

			QName objectClass = null;
			for (QName q: parentPage.getTaskDto().getObjectClassList()) {
				if (q.getLocalPart().equals(dto.getObjectClass())) {
					objectClass = q;
				}
			}

			objectClassProperty.setRealValue(objectClass);
			existingTask.addExtensionProperty(objectClassProperty);

		} else {
			PrismProperty objectClass = existingTask.getExtensionProperty(SchemaConstants.OBJECTCLASS_PROPERTY_NAME);

			if(objectClass != null){
				existingTask.deleteExtensionProperty(objectClass);
			}
		}

		if(dto.getWorkerThreads() != null) {
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
		parentPage.showResult(result);
		parentPage.setResponsePage(new PageTasks(false));
	}

	void resumePerformed(AjaxRequestTarget target) {
		String oid = parentPage.getTaskDto().getOid();
		OperationResult result = TaskOperationUtils.resumePerformed(parentPage.getTaskService(), Arrays.asList(oid));
		parentPage.showResult(result);
		parentPage.setResponsePage(new PageTasks(false));
	}

	void runNowPerformed(AjaxRequestTarget target) {
		String oid = parentPage.getTaskDto().getOid();
		OperationResult result = TaskOperationUtils.runNowPerformed(parentPage.getTaskService(), Arrays.asList(oid));
		parentPage.showResult(result);
		parentPage.setResponsePage(new PageTasks(false));
	}
}
