/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.task.component;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.page.admin.server.RefreshableTabPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.apache.wicket.Component;

import java.util.Collection;
import java.util.Collections;

/**
 * @author semancik
 */
//@PanelType(name = "performance")
@PanelInstance(identifier = "performance", applicableFor = TaskType.class, status = ItemStatus.NOT_CHANGED)
@PanelDisplay(label = "Performance", order = 50)
public class TaskPerformancePanel extends AbstractObjectMainPanel<TaskType, ObjectDetailsModels<TaskType>> implements RefreshableTabPanel {
    private static final long serialVersionUID = 1L;

    private static final String ID_STATISTICS_PANEL = "statisticsPanel";

    private static final Trace LOGGER = TraceManager.getTrace(TaskPerformancePanel.class);

    public TaskPerformancePanel(String id, ObjectDetailsModels<TaskType> taskWrapperModel, ContainerPanelConfigurationType config) {
        super(id, taskWrapperModel, config);
        setOutputMarkupId(true);
    }

   protected void initLayout() {
//        StatisticsDtoModel statisticsDtoModel = new StatisticsDtoModel(getModel());
//        StatisticsPanel statisticsPanel = new StatisticsPanel(ID_STATISTICS_PANEL, statisticsDtoModel);
//        add(statisticsPanel);
    }

    @Override
    public Collection<Component> getComponentsToUpdate() {
        return Collections.<Component>singleton(this);
    }

}
