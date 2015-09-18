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

import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.server.PageTaskEdit;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IterativeTaskInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationInformationType;

import java.util.Date;

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
    }

    public TaskCurrentStateDto(TaskDto taskDto, SynchronizationInformationType sit, IterativeTaskInformationType itit, Long currentProgress) {
        this.taskDto = taskDto;
        this.synchronizationInformationType = sit;
        this.iterativeTaskInformationType = itit;
        if (currentProgress != null) {
            this.currentProgress = currentProgress;
        } else {
            if (taskDto != null) {
                this.currentProgress = taskDto.getProgress();
            }
        }
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
