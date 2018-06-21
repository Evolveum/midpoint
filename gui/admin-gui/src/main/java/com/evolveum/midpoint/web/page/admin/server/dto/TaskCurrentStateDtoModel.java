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

package com.evolveum.midpoint.web.page.admin.server.dto;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
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
//        if (object == null) {
            object = getObjectInternal();
//        }
        return object;
    }

    protected TaskCurrentStateDto getObjectInternal() {
        return new TaskCurrentStateDto(taskModel.getObject());
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
            TaskDto taskDto = new TaskDto(task.asObjectable(), null, page.getModelService(), page.getTaskService(),
                    page.getModelInteractionService(), taskManager, page.getWorkflowManager(), TaskDtoProviderOptions.fullOptions(), false, operationTask, result, page);
            taskModel.setObject(taskDto);
        } catch (CommunicationException|ObjectNotFoundException|SchemaException|SecurityViolationException|ConfigurationException|ExpressionEvaluationException|RuntimeException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't refresh task {}", e, taskModel.getObject());
        }
    }
}
