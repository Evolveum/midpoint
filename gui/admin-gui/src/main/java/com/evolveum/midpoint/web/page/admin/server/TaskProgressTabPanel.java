/*
 * Copyright (c) 2010-2016 Evolveum
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
import com.evolveum.midpoint.web.component.prism.PrismPropertyPanel;
import com.evolveum.midpoint.web.component.progress.StatisticsDtoModel;
import com.evolveum.midpoint.web.component.progress.StatisticsPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.model.PropertyWrapperFromObjectWrapperModel;
import com.evolveum.midpoint.web.page.admin.server.currentState.*;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import org.apache.wicket.Component;
import org.apache.wicket.model.PropertyModel;

import java.util.Collection;
import java.util.Collections;

/**
 * @author semancik
 */
public class TaskProgressTabPanel extends AbstractObjectTabPanel<TaskType> implements TaskTabPanel {
	private static final long serialVersionUID = 1L;

	private static final String ID_ITERATIVE_INFORMATION_PANEL = "iterativeInformationPanel";
	private static final String ID_SYNCHRONIZATION_INFORMATION_PANEL_BEFORE = "synchronizationInformationPanelBefore";
	private static final String ID_SYNCHRONIZATION_INFORMATION_PANEL_AFTER = "synchronizationInformationPanelAfter";
	private static final String ID_ACTIONS_EXECUTED_INFORMATION_PANEL = "actionsExecutedInformationPanel";

	private static final Trace LOGGER = TraceManager.getTrace(TaskProgressTabPanel.class);

	public TaskProgressTabPanel(String id, Form mainForm,
			LoadableModel<ObjectWrapper<TaskType>> taskWrapperModel,
			LoadableModel<TaskDto> taskDtoModel, PageBase pageBase) {
		super(id, mainForm, taskWrapperModel, pageBase);
		initLayout(taskDtoModel, pageBase);
		setOutputMarkupId(true);
	}

	private void initLayout(final LoadableModel<TaskDto> taskDtoModel, PageBase pageBase) {
		final TaskCurrentStateDtoModel model = new TaskCurrentStateDtoModel(taskDtoModel);
		add(new IterativeInformationPanel(ID_ITERATIVE_INFORMATION_PANEL, model, pageBase));

		SynchronizationInformationPanel synchronizationInformationPanelBefore = new SynchronizationInformationPanel(ID_SYNCHRONIZATION_INFORMATION_PANEL_BEFORE,
				new PropertyModel<SynchronizationInformationDto>(model, TaskCurrentStateDto.F_SYNCHRONIZATION_INFORMATION_DTO), false);
		synchronizationInformationPanelBefore.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return model.getObject().getSynchronizationInformationType() != null;
			}
		});
		add(synchronizationInformationPanelBefore);

		SynchronizationInformationPanel synchronizationInformationPanelAfter = new SynchronizationInformationPanel(ID_SYNCHRONIZATION_INFORMATION_PANEL_AFTER,
				new PropertyModel<SynchronizationInformationDto>(model, TaskCurrentStateDto.F_SYNCHRONIZATION_INFORMATION_AFTER_DTO), true);
		synchronizationInformationPanelAfter.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return model.getObject().getSynchronizationInformationType() != null && !taskDtoModel.getObject().isDryRun();
			}
		});
		add(synchronizationInformationPanelAfter);

		ActionsExecutedInformationPanel actionsExecutedInformationPanel = new ActionsExecutedInformationPanel(ID_ACTIONS_EXECUTED_INFORMATION_PANEL,
				new PropertyModel<ActionsExecutedInformationDto>(model, TaskCurrentStateDto.F_ACTIONS_EXECUTED_INFORMATION_DTO));
		actionsExecutedInformationPanel.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return model.getObject().getActionsExecutedInformationType() != null;
			}
		});
		add(actionsExecutedInformationPanel);

	}

	@Override
	public Collection<Component> getComponentsToUpdate() {
		return Collections.<Component>singleton(this);
	}

}
