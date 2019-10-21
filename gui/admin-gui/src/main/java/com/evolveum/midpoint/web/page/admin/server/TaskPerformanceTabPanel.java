/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.server;

import java.util.Collection;
import java.util.Collections;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectTabPanel;
import com.evolveum.midpoint.web.component.progress.StatisticsDtoModel;
import com.evolveum.midpoint.web.component.progress.StatisticsPanel;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * @author semancik
 */
public class TaskPerformanceTabPanel extends AbstractObjectTabPanel<TaskType> implements TaskTabPanel {
    private static final long serialVersionUID = 1L;

    private static final String ID_STATISTICS_PANEL = "statisticsPanel";

    private IModel<TaskDto> taskDtoModel;

    private static final Trace LOGGER = TraceManager.getTrace(TaskPerformanceTabPanel.class);

    public TaskPerformanceTabPanel(String id, Form mainForm,
            LoadableModel<PrismObjectWrapper<TaskType>> taskWrapperModel,
            IModel<TaskDto> taskDtoModel, PageBase pageBase) {
        super(id, mainForm, taskWrapperModel);
        this.taskDtoModel = taskDtoModel;

        setOutputMarkupId(true);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        StatisticsDtoModel statisticsDtoModel = new StatisticsDtoModel(taskDtoModel);
        StatisticsPanel statisticsPanel = new StatisticsPanel(ID_STATISTICS_PANEL, statisticsDtoModel);
        add(statisticsPanel);
    }

    @Override
    public Collection<Component> getComponentsToUpdate() {
        return Collections.<Component>singleton(this);
    }

}
