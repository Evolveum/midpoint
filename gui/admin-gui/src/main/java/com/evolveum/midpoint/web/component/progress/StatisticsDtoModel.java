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

package com.evolveum.midpoint.web.component.progress;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationalInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import org.apache.wicket.Application;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

/**
 * @author Pavol Mederly
 */
public class StatisticsDtoModel extends AbstractReadOnlyModel<StatisticsDto> {

    private static final Trace LOGGER = TraceManager.getTrace(StatisticsDtoModel.class);

    // at most one of these may be null
    private transient Task task;
    private IModel<TaskDto> taskModel;

    private transient StatisticsDto cachedObject;

    public StatisticsDtoModel() {
    }

    public StatisticsDtoModel(IModel<TaskDto> taskModel) {
        this.taskModel = taskModel;
    }

    @Override
    public StatisticsDto getObject() {
        if (cachedObject != null) {
            return cachedObject;
        }

        StatisticsDto info = getObjectInternal();
        if (info != null) {
            cachedObject = info;
            return cachedObject;
        }

        return new StatisticsDto();
    }

    public StatisticsDto getObjectInternal() {

        if (task != null) {
            return getStatisticsFromTask(task);
        }

        String taskId = null;
        if (taskModel != null && taskModel.getObject() != null) {
            taskId = taskModel.getObject().getIdentifier();
        }

        if (taskId == null) {
            LOGGER.warn("taskIdentifier not available");
            return null;
        }
        MidPointApplication application = (MidPointApplication) Application.get();
        TaskManager taskManager = application.getTaskManager();
        Task task = taskManager.getLocallyRunningTaskByIdentifier(taskId);
        if (task == null) {
            LOGGER.trace("No task by taskIdentifier, trying analyzing the extension");
            if (taskModel == null || taskModel.getObject() == null) {
                LOGGER.trace("No taskModel or no object in it");
                return null;
            }
            TaskType taskType = taskModel.getObject().getTaskType();
            if (taskType == null) {
                LOGGER.trace("No TaskType found");
                return null;
            }

            PrismContainer<?> extension = taskType.asPrismObject().getExtension();
            if (extension == null) {
                LOGGER.trace("No extension in TaskType found");
                return null;
            }
            OperationalInformationType infoPropertyValue = extension.getPropertyRealValue(SchemaConstants.MODEL_EXTENSION_OPERATIONAL_INFORMATION_PROPERTY_NAME, OperationalInformationType.class);
            if (infoPropertyValue == null) {
                LOGGER.trace("No info in task extension either.");
                return null;
            }
            infoPropertyValue.setFromMemory(false);
            return new StatisticsDto(infoPropertyValue);
        }
        return getStatisticsFromTask(task);
    }

    protected StatisticsDto getStatisticsFromTask(Task task) {
        OperationalInformationType operationalInformation = task.getAggregateOperationalInformation();
        if (operationalInformation == null) {
            LOGGER.warn("No operational information in task");
            return null;
        }
        operationalInformation.setFromMemory(true);
        StatisticsDto dto = new StatisticsDto(operationalInformation);
        return dto;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public void invalidateCache() {
        cachedObject = null;
    }
}
