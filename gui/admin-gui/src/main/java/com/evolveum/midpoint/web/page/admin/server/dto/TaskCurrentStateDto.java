/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.server.dto;

import com.evolveum.midpoint.xml.ns._public.common.common_3.IterativeTaskInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActionsExecutedInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationStatsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationInformationType;

/**
 * @author Pavol Mederly
 */
public class TaskCurrentStateDto {

	public static final String F_ACTIONS_EXECUTED_INFORMATION_DTO = "actionsExecutedInformationDto";
	public static final String F_SYNCHRONIZATION_INFORMATION_DTO = "synchronizationInformationDto";
	public static final String F_SYNCHRONIZATION_INFORMATION_AFTER_DTO = "synchronizationInformationAfterDto";

	private TaskDto taskDto;

    public TaskCurrentStateDto(TaskDto taskDto) {
        this.taskDto = taskDto;
    }

    public TaskDto getTaskDto() {
        return taskDto;
    }

    public OperationStatsType getOperationStatsType() {
        if (taskDto == null) {
            return null;
        }
        return taskDto.getAggregatedOperationStats();
    }

    public SynchronizationInformationType getSynchronizationInformationType() {
        OperationStatsType stats = getOperationStatsType();
        if (stats == null) {
            return null;
        }
        return stats.getSynchronizationInformation();
    }

	public SynchronizationInformationDto getSynchronizationInformationDto() {
		return getSynchronizationInformationType() != null ? new SynchronizationInformationDto(getSynchronizationInformationType(), false) : null;
	}

	public SynchronizationInformationDto getSynchronizationInformationAfterDto() {
		return getSynchronizationInformationType() != null ? new SynchronizationInformationDto(getSynchronizationInformationType(), true) : null;
	}

	public IterativeTaskInformationType getIterativeTaskInformationType() {
        OperationStatsType stats = getOperationStatsType();
        if (stats == null) {
            return null;
        }
        return stats.getIterativeTaskInformation();
    }

    public ActionsExecutedInformationType getActionsExecutedInformationType() {
        OperationStatsType stats = getOperationStatsType();
        if (stats == null) {
            return null;
        }
        return stats.getActionsExecutedInformation();
    }

	public ActionsExecutedInformationDto getActionsExecutedInformationDto() {
		if (getActionsExecutedInformationType() == null) {
			return null;
		} else {
			return new ActionsExecutedInformationDto(getActionsExecutedInformationType());
		}
	}

    public Long getCurrentProgress() {
        if (taskDto == null) {
            return null;
        }
        return taskDto.getProgress();
    }
}
