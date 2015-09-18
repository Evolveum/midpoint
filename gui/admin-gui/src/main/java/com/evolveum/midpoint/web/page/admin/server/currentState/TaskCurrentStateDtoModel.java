/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.admin.server.currentState;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoProviderOptions;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IterativeTaskInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import org.apache.wicket.Application;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

import java.util.Collection;

/**
 * @author Pavol Mederly
 */
public class TaskCurrentStateDtoModel extends AbstractReadOnlyModel<TaskCurrentStateDto> {

    private static final Trace LOGGER = TraceManager.getTrace(TaskCurrentStateDtoModel.class);

    private IModel<TaskDto> taskModel;

    public TaskCurrentStateDtoModel(IModel<TaskDto> taskModel) {
        this.taskModel = taskModel;
    }

    private transient TaskCurrentStateDto object;

    @Override
    public TaskCurrentStateDto getObject() {
        if (object == null) {
            object = getObjectInternal();
        }
        return object;
    }

    protected TaskCurrentStateDto getObjectInternal() {
        String taskId;
        if (taskModel != null && taskModel.getObject() != null) {
            taskId = taskModel.getObject().getIdentifier();
        } else {
            taskId = null;
        }

        if (taskId == null) {
            LOGGER.warn("taskIdentifier not available");
            return new TaskCurrentStateDto(taskModel.getObject());
        }
        MidPointApplication application = (MidPointApplication) Application.get();
        TaskManager taskManager = application.getTaskManager();
        LOGGER.trace("Trying to find task by identifier {}", taskId);
        Task task = taskManager.getLocallyRunningTaskByIdentifier(taskId);
        if (task == null) {
            LOGGER.trace("No task by taskIdentifier, trying analyzing the extension");
            if (taskModel == null || taskModel.getObject() == null) {
                LOGGER.trace("No taskModel or no object in it");
                return new TaskCurrentStateDto(taskModel.getObject());
            }
            TaskType taskType = taskModel.getObject().getTaskType();
            if (taskType == null) {
                LOGGER.trace("No TaskType found");
                return new TaskCurrentStateDto(taskModel.getObject());
            }

            PrismContainer<?> extension = taskType.asPrismObject().getExtension();
            if (extension == null) {
                LOGGER.trace("No extension in TaskType found");
                return new TaskCurrentStateDto(taskModel.getObject());
            }
            SynchronizationInformationType infoPropertyValue = extension.getPropertyRealValue(SchemaConstants.MODEL_EXTENSION_SYNCHRONIZATION_INFORMATION_PROPERTY_NAME, SynchronizationInformationType.class);
            if (infoPropertyValue != null) {
                infoPropertyValue.setFromMemory(false);
            } else {
                LOGGER.trace("No SynchronizationInformationType in task extension.");
            }
            IterativeTaskInformationType ititPropertyValue = extension.getPropertyRealValue(SchemaConstants.MODEL_EXTENSION_ITERATIVE_TASK_INFORMATION_PROPERTY_NAME, IterativeTaskInformationType.class);
            if (ititPropertyValue != null) {
                ititPropertyValue.setFromMemory(false);
            } else {
                LOGGER.trace("No IterativeTaskInformationType in task extension.");
            }
            return new TaskCurrentStateDto(taskModel.getObject(), infoPropertyValue, ititPropertyValue, null);
        }
        SynchronizationInformationType sit = task.getAggregateSynchronizationInformation();
        if (sit != null) {
            sit.setFromMemory(true);
        } else {
            LOGGER.warn("No synchronization information in task");
        }
        IterativeTaskInformationType itit = task.getAggregateIterativeTaskInformation();;
        if (itit != null) {
            itit.setFromMemory(true);
        } else {
            LOGGER.warn("No synchronization information in task");
        }
        return new TaskCurrentStateDto(taskModel.getObject(), sit, itit, task.getProgress());
    }

    public void refresh(PageBase page) {
        object = null;

        if (taskModel == null || taskModel.getObject() == null) {
            LOGGER.warn("Null or empty taskModel");
            return;
        }
        TaskManager taskManager = page.getTaskManager();
        OperationResult result = new OperationResult("refresh");
        Task operationTask = taskManager.createTaskInstance("refresh");

        String oid = taskModel.getObject().getOid();
        try {
            LOGGER.debug("Refreshing task {}", taskModel.getObject());
            Collection<SelectorOptions<GetOperationOptions>> options = GetOperationOptions.createRetrieveAttributesOptions(TaskType.F_SUBTASK, TaskType.F_NODE_AS_OBSERVED);
            PrismObject<TaskType> task = page.getModelService().getObject(TaskType.class, oid, options, operationTask, result);
            TaskDto taskDto = new TaskDto(task.asObjectable(), page.getModelService(), page.getTaskService(),
                    page.getModelInteractionService(), taskManager, TaskDtoProviderOptions.minimalOptions(), result, page);
            taskModel.setObject(taskDto);
        } catch (CommunicationException|ObjectNotFoundException|SchemaException|SecurityViolationException|ConfigurationException|RuntimeException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't refresh task {}", e, taskModel.getObject());
        }
    }
}
