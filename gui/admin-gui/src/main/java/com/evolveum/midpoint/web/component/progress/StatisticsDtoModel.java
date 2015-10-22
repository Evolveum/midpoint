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

import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EnvironmentalPerformanceInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationStatsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
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
        if (taskModel != null && taskModel.getObject() != null && taskModel.getObject().getTaskType() != null) {
            return getStatisticsFromTaskType(taskModel.getObject().getTaskType());
        }
        return null;
    }

    protected StatisticsDto getStatisticsFromTask(Task task) {

        OperationStatsType operationStats = task.getAggregatedLiveOperationStats();
        if (operationStats == null) {
            LOGGER.warn("No operational information in task");
            return null;
        }
        EnvironmentalPerformanceInformationType envInfo = operationStats.getEnvironmentalPerformanceInformation();
        if (envInfo == null) {
            LOGGER.warn("No environmental performance information in task");
            return null;
        }
        StatisticsDto dto = new StatisticsDto(envInfo);
        return dto;
    }

    protected StatisticsDto getStatisticsFromTaskType(TaskType task) {
        OperationStatsType operationStats = task.getOperationStats();
        if (operationStats == null) {
            LOGGER.warn("No operational information in task");
            return null;
        }
        EnvironmentalPerformanceInformationType envInfo = operationStats.getEnvironmentalPerformanceInformation();
        if (envInfo == null) {
            LOGGER.warn("No environmental performance information in task");
            return null;
        }
        StatisticsDto dto = new StatisticsDto(envInfo);
        return dto;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public void invalidateCache() {
        cachedObject = null;
    }
}
