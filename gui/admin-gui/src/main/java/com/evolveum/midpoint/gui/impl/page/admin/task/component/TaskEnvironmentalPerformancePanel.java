/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.task.component;

import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.task.TaskDetailsModel;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.progress.StatisticsDtoModel;
import com.evolveum.midpoint.web.component.progress.StatisticsPanel;
import com.evolveum.midpoint.web.page.admin.server.RefreshableTabPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.apache.wicket.Component;

import java.util.Collection;
import java.util.Collections;

/**
 * @author semancik
 */
@PanelType(name = "environmentalPerformance")
@PanelInstance(identifier = "environmentalPerformance", applicableForType = TaskType.class, childOf = TaskPerformancePanel.class,
        display = @PanelDisplay(label = "pageTask.environmentalPerformance.title", order = 75))
public class TaskEnvironmentalPerformancePanel extends AbstractObjectMainPanel<TaskType, TaskDetailsModel> implements RefreshableTabPanel {
    private static final long serialVersionUID = 1L;

    private static final String ID_STATISTICS_PANEL = "statisticsPanel";

    private static final Trace LOGGER = TraceManager.getTrace(TaskEnvironmentalPerformancePanel.class);

    public TaskEnvironmentalPerformancePanel(String id, TaskDetailsModel taskWrapperModel, ContainerPanelConfigurationType config) {
        super(id, taskWrapperModel, config);
        setOutputMarkupId(true);
    }

   protected void initLayout() {
        StatisticsDtoModel statisticsDtoModel = new StatisticsDtoModel(getObjectWrapperModel());
        StatisticsPanel statisticsPanel = new StatisticsPanel(ID_STATISTICS_PANEL, statisticsDtoModel);
        add(statisticsPanel);
    }

    @Override
    public Collection<Component> getComponentsToUpdate() {
        return Collections.<Component>singleton(this);
    }

}
