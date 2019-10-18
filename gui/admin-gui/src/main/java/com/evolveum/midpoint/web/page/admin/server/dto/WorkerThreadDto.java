/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.server.dto;

import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoExecutionStatus;

import java.io.Serializable;

/**
 * @author Pavol Mederly
 */
public class WorkerThreadDto implements Serializable {

    public static final String F_NAME = "name";
    public static final String F_EXECUTION_STATUS = "executionStatus";
    public static final String F_PROGRESS = "progress";

    private final TaskDto subtaskDto;

    public WorkerThreadDto(TaskDto subtaskDto) {
        this.subtaskDto = subtaskDto;
    }

    public String getName() {
        return subtaskDto != null ? subtaskDto.getName() : null;
    }

    public Long getProgress() {
        return subtaskDto != null ? subtaskDto.getProgress() : null;
    }

    public TaskDtoExecutionStatus getExecutionStatus() {
        return subtaskDto != null ? subtaskDto.getExecution() : null;
    }
}
