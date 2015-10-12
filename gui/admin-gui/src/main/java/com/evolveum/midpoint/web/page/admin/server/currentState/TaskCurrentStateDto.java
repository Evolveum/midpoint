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
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IterativeTaskInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * @author Pavol Mederly
 */
public class TaskCurrentStateDto {

    private TaskDto taskDto;
    private SynchronizationInformationType synchronizationInformationType;
    private IterativeTaskInformationType iterativeTaskInformationType;
    private Long currentProgress;

    public TaskCurrentStateDto(TaskDto taskDto) {
        this.taskDto = taskDto;
        if (taskDto == null) {
            return;
        }
        this.currentProgress = taskDto.getProgress();
        TaskType taskType = taskDto.getTaskType();
        if (taskType == null) {
            return;
        }
        PrismContainer<?> extension = taskType.asPrismObject().getExtension();
        if (extension == null) {
            return;
        }
        synchronizationInformationType = extension.getPropertyRealValue(SchemaConstants.MODEL_EXTENSION_SYNCHRONIZATION_INFORMATION_PROPERTY_NAME, SynchronizationInformationType.class);
        iterativeTaskInformationType = extension.getPropertyRealValue(SchemaConstants.MODEL_EXTENSION_ITERATIVE_TASK_INFORMATION_PROPERTY_NAME, IterativeTaskInformationType.class);
    }

    public TaskDto getTaskDto() {
        return taskDto;
    }

    public SynchronizationInformationType getSynchronizationInformationType() {
        return synchronizationInformationType;
    }

    public IterativeTaskInformationType getIterativeTaskInformationType() {
        return iterativeTaskInformationType;
    }

    public Long getCurrentProgress() {
        return currentProgress;
    }
}
