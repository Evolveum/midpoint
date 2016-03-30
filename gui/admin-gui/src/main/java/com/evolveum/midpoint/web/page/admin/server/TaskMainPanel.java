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
import com.evolveum.midpoint.schema.statistics.StatisticsUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.objectdetails.*;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationStatsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author semancik
 *
 */
public class TaskMainPanel extends Panel {

	private static final String ID_MAIN_FORM = "mainForm";
	private static final String ID_TAB_PANEL = "tabPanel";
	private static final String ID_EXECUTE_OPTIONS = "executeOptions";
	private static final String ID_BACK = "back";
	private static final String ID_SAVE = "save";
	private static final String ID_PREVIEW_CHANGES = "previewChanges";

	private static final Trace LOGGER = TraceManager.getTrace(TaskMainPanel.class);

	private LoadableModel<ObjectWrapper<TaskType>> objectModel;
	private LoadableModel<TaskDto> taskDtoModel;
	private Form mainForm;

	public TaskMainPanel(String id, LoadableModel<ObjectWrapper<TaskType>> objectModel, LoadableModel<TaskDto> taskDtoModel, PageTask2 parentPage) {
		super(id, objectModel);
		this.objectModel = objectModel;
		this.taskDtoModel = taskDtoModel;
		initLayout(parentPage);
	}

	private void initLayout(PageTask2 parentPage) {
		mainForm = new Form<>(ID_MAIN_FORM, true);
		add(mainForm);
		initTabPanel(parentPage);
	}

	protected void initTabPanel(final PageTask2 parentPage) {
		List<ITab> tabs = createTabs(parentPage);
		TabbedPanel<ITab> tabPanel = AbstractObjectMainPanel.createTabPanel(parentPage, tabs);
		mainForm.add(tabPanel);
	}

	protected List<ITab> createTabs(final PageTask2 parentPage) {
		List<ITab> tabs = new ArrayList<>();
		tabs.add(
				new AbstractTab(parentPage.createStringResource("pageTaskEdit.basic")){
					@Override
					public WebMarkupContainer getPanel(String panelId) {
						return new TaskBasicTabPanel(panelId, mainForm, objectModel, taskDtoModel, parentPage);
					}
				});
		tabs.add(
				new AbstractTab(parentPage.createStringResource("pageTaskEdit.scheduleTitle")){
					@Override
					public WebMarkupContainer getPanel(String panelId) {
						return new TaskSchedulingTabPanel(panelId, mainForm, objectModel, parentPage);
					}
				});
		tabs.add(
				new AbstractTab(parentPage.createStringResource("pageTaskEdit.subtasksAndThreads")){
					@Override
					public WebMarkupContainer getPanel(String panelId) {
						return new TaskSubtasksAndThreadsTabPanel(panelId, mainForm, objectModel, taskDtoModel, parentPage);
					}
					@Override
					public boolean isVisible() {
						return true;
					}
				});
		tabs.add(
				new AbstractTab(parentPage.createStringResource("pageTaskEdit.progress")){
					@Override
					public WebMarkupContainer getPanel(String panelId) {
						return new TaskProgressTabPanel(panelId, mainForm, objectModel, taskDtoModel, parentPage);
					}

					@Override
					public boolean isVisible() {
						final OperationStatsType operationStats = taskDtoModel.getObject().getTaskType().getOperationStats();
						return operationStats != null && operationStats.getIterativeTaskInformation() != null;
					}
				});
		tabs.add(
				new AbstractTab(parentPage.createStringResource("pageTaskEdit.statesAndActions")){
					@Override
					public WebMarkupContainer getPanel(String panelId) {
						return new TaskStatesAndActionsTabPanel(panelId, mainForm, objectModel, taskDtoModel, parentPage);
					}
					@Override
					public boolean isVisible() {
						final OperationStatsType operationStats = taskDtoModel.getObject().getTaskType().getOperationStats();
						return operationStats != null && (operationStats.getSynchronizationInformation() != null || operationStats.getActionsExecutedInformation() != null);
					}
				});
		tabs.add(
				new AbstractTab(parentPage.createStringResource("pageTaskEdit.performance")){
					@Override
					public WebMarkupContainer getPanel(String panelId) {
						return new TaskPerformanceTabPanel(panelId, mainForm, objectModel, taskDtoModel, parentPage);
					}

					@Override
					public boolean isVisible() {
						final OperationStatsType operationStats = taskDtoModel.getObject().getTaskType().getOperationStats();
						return operationStats != null && !StatisticsUtil.isEmpty(operationStats.getEnvironmentalPerformanceInformation());
					}
				});
		tabs.add(
				new AbstractTab(parentPage.createStringResource("pageTaskEdit.approvals")){
					@Override
					public WebMarkupContainer getPanel(String panelId) {
						return new TaskApprovalsTabPanel(panelId, mainForm, objectModel, taskDtoModel, parentPage);
					}
					@Override
					public boolean isVisible() {
						return taskDtoModel.getObject().getTaskType().getWorkflowContext() != null
								&& taskDtoModel.getObject().getWorkflowDeltaIn() != null;
					}
				});
		tabs.add(
				new AbstractTab(parentPage.createStringResource("pageTaskEdit.operation")){
					@Override
					public WebMarkupContainer getPanel(String panelId) {
						return new TaskOperationTabPanel(panelId, mainForm, objectModel, taskDtoModel, parentPage);
					}
					@Override
					public boolean isVisible() {
						return taskDtoModel.getObject().getTaskType().getModelOperationContext() != null;
					}
				});
		tabs.add(
				new AbstractTab(parentPage.createStringResource("pageTaskEdit.result")){
					@Override
					public WebMarkupContainer getPanel(String panelId) {
						return new TaskResultTabPanel(panelId, mainForm, objectModel, taskDtoModel, parentPage);
					}
				});
		return tabs;
	}

	protected PageTask2 getDetailsPage() {
		return (PageTask2) getPage();
	}

}
