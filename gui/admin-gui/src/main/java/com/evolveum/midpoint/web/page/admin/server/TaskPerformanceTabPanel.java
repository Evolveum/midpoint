/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.web.page.admin.server;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectTabPanel;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.progress.StatisticsDtoModel;
import com.evolveum.midpoint.web.component.progress.StatisticsPanel;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

import java.util.Collection;
import java.util.Collections;

/**
 * @author semancik
 */
public class TaskPerformanceTabPanel extends AbstractObjectTabPanel<TaskType> implements TaskTabPanel {
	private static final long serialVersionUID = 1L;

	private static final String ID_STATISTICS_PANEL = "statisticsPanel";

	private IModel<TaskDto> taskDtoModel;

	private static final Trace LOGGER = TraceManager.getTrace(TaskPerformanceTabPanel.class);

	public TaskPerformanceTabPanel(String id, Form mainForm,
			LoadableModel<ObjectWrapper<TaskType>> taskWrapperModel,
			IModel<TaskDto> taskDtoModel, PageBase pageBase) {
		super(id, mainForm, taskWrapperModel, pageBase);
		this.taskDtoModel = taskDtoModel;
		initLayout(pageBase);
		setOutputMarkupId(true);
	}

	private void initLayout(PageBase pageBase) {
		StatisticsDtoModel statisticsDtoModel = new StatisticsDtoModel(taskDtoModel);
		StatisticsPanel statisticsPanel = new StatisticsPanel(ID_STATISTICS_PANEL, statisticsDtoModel);
		add(statisticsPanel);
	}

	@Override
	public Collection<Component> getComponentsToUpdate() {
		return Collections.<Component>singleton(this);
	}

}
