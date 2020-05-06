/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.progress;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EnvironmentalPerformanceInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationStatsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import org.apache.wicket.model.IModel;

/**
 * @author Pavol Mederly
 */
public class StatisticsDtoModel implements IModel<StatisticsDto> {

    private static final Trace LOGGER = TraceManager.getTrace(StatisticsDtoModel.class);

    // at most one of these may be null
    private transient Task task;
    private IModel<PrismObjectWrapper<TaskType>> taskModel;

    private transient StatisticsDto cachedObject;

    public StatisticsDtoModel() {
    }

    public StatisticsDtoModel(IModel<PrismObjectWrapper<TaskType>> taskModel) {
        this.taskModel = taskModel;
    }

    @Override
    public StatisticsDto getObject() {
        //we can't use cache here probably because data on the panel
        // should be updated all the time
//        if (cachedObject != null) {
//            return cachedObject;
//        }

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
        if (taskModel != null && taskModel.getObject() != null && taskModel.getObject().getObject() != null) {
            return getStatisticsFromTaskType(taskModel.getObject().getObject().asObjectable());
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
