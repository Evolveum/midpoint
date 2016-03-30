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
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectMainPanel;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoExecutionStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationStatsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import org.apache.wicket.ajax.AjaxRequestTarget;
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
	private static final String ID_EDIT = "edit";
	private static final String ID_BACK = "back";
	private static final String ID_SAVE = "save";
	private static final String ID_SUSPEND = "suspend";
	private static final String ID_RESUME = "resume";
	private static final String ID_RUN_NOW = "runNow";

	private static final Trace LOGGER = TraceManager.getTrace(TaskMainPanel.class);

	private LoadableModel<ObjectWrapper<TaskType>> objectModel;
	private LoadableModel<TaskDto> taskDtoModel;
	private Form mainForm;
	private PageTask2 parentPage;

	public TaskMainPanel(String id, LoadableModel<ObjectWrapper<TaskType>> objectModel, LoadableModel<TaskDto> taskDtoModel, PageTask2 parentPage) {
		super(id, objectModel);
		this.objectModel = objectModel;
		this.taskDtoModel = taskDtoModel;
		this.parentPage = parentPage;
		initLayout();
	}

	private void initLayout() {
		mainForm = new Form<>(ID_MAIN_FORM, true);
		add(mainForm);
		initTabPanel();
		initButtons();
	}

	protected void initTabPanel() {
		List<ITab> tabs = createTabs();
		TabbedPanel<ITab> tabPanel = AbstractObjectMainPanel.createTabPanel(parentPage, tabs);
		mainForm.add(tabPanel);
	}

	protected List<ITab> createTabs() {
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
						return !parentPage.isEdit() && (!taskDtoModel.getObject().getSubtasks().isEmpty() || !taskDtoModel.getObject().getTransientSubtasks().isEmpty());
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
						return !parentPage.isEdit() && operationStats != null && operationStats.getIterativeTaskInformation() != null;
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
						return !parentPage.isEdit()
								&& operationStats != null
								&& (operationStats.getSynchronizationInformation() != null || operationStats.getActionsExecutedInformation() != null);
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
						return !parentPage.isEdit() && operationStats != null && !StatisticsUtil.isEmpty(operationStats.getEnvironmentalPerformanceInformation());
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
						return !parentPage.isEdit()
								&& taskDtoModel.getObject().getTaskType().getWorkflowContext() != null
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
						return !parentPage.isEdit() && taskDtoModel.getObject().getTaskType().getModelOperationContext() != null;
					}
				});
		tabs.add(
				new AbstractTab(parentPage.createStringResource("pageTaskEdit.result")){
					@Override
					public WebMarkupContainer getPanel(String panelId) {
						return new TaskResultTabPanel(panelId, mainForm, objectModel, taskDtoModel, parentPage);
					}

					@Override
					public boolean isVisible() {
						return !parentPage.isEdit();
					}
				});
		return tabs;
	}

	private void initButtons() {
		AjaxButton backButton = new AjaxButton(ID_BACK, parentPage.createStringResource("pageTaskEdit.button.back")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				parentPage.setEdit(false);
				parentPage.goBack(PageTasks.class);
			}
		};
		mainForm.add(backButton);

		AjaxSubmitButton saveButton = new AjaxSubmitButton(ID_SAVE, parentPage.createStringResource("pageTaskEdit.button.save")) {

			@Override
			protected void onSubmit(AjaxRequestTarget target, org.apache.wicket.markup.html.form.Form<?> form) {
				parentPage.getController().savePerformed(target);
			}

			@Override
			protected void onError(AjaxRequestTarget target, org.apache.wicket.markup.html.form.Form<?> form) {
				target.add(parentPage.getFeedbackPanel());
			}

		};
		saveButton.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return parentPage.isEdit();
			}
		});
		mainForm.setDefaultButton(saveButton);
		mainForm.add(saveButton);

		AjaxButton editButton = new AjaxButton(ID_EDIT, parentPage.createStringResource("pageTaskEdit.button.edit")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				parentPage.setEdit(true);
				parentPage.stopRefresh();
				target.add(mainForm);
			}
		};
		editButton.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return !parentPage.isEdit();
			}
		});
		mainForm.add(editButton);

		AjaxButton suspend = new AjaxButton(ID_SUSPEND, parentPage.createStringResource("pageTaskEdit.button.suspend")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				parentPage.getController().suspendPerformed(target);
			}
		};
		suspend.add(new VisibleEnableBehaviour() {

			@Override
			public boolean isVisible() {
				return !parentPage.isEdit() && isRunnableOrRunning();
			}
		});
		mainForm.add(suspend);

		AjaxButton resume = new AjaxButton(ID_RESUME, parentPage.createStringResource("pageTaskEdit.button.resume")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				parentPage.getController().resumePerformed(target);
			}
		};
		resume.add(new VisibleEnableBehaviour() {

			@Override
			public boolean isVisible() {
				return !parentPage.isEdit() && (isSuspended() || (isClosed() && isRecurring()));
			}
		});
		mainForm.add(resume);

		AjaxButton runNow = new AjaxButton(ID_RUN_NOW, parentPage.createStringResource("pageTaskEdit.button.runNow")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				parentPage.getController().runNowPerformed(target);
			}
		};
		runNow.add(new VisibleEnableBehaviour() {

			@Override
			public boolean isVisible() {
				return !parentPage.isEdit() && (isRunnable() || (isClosed() && !isRecurring()));
			}
		});
		mainForm.add(runNow);
	}

	private boolean isRunnableOrRunning() {
		TaskDtoExecutionStatus exec = parentPage.getTaskDto().getExecution();
		return TaskDtoExecutionStatus.RUNNABLE.equals(exec) || TaskDtoExecutionStatus.RUNNING.equals(exec);
	}

	private boolean isRunnable() {
		TaskDtoExecutionStatus exec = parentPage.getTaskDto().getExecution();
		return TaskDtoExecutionStatus.RUNNABLE.equals(exec);
	}

	private boolean isRunning() {
		TaskDtoExecutionStatus exec = parentPage.getTaskDto().getExecution();
		return TaskDtoExecutionStatus.RUNNING.equals(exec);
	}

	private boolean isClosed() {
		TaskDtoExecutionStatus exec = parentPage.getTaskDto().getExecution();
		return TaskDtoExecutionStatus.CLOSED.equals(exec);
	}

	private boolean isRecurring() {
		return parentPage.getTaskDto().getRecurring();
	}

	private boolean isSuspended() {
		TaskDtoExecutionStatus exec = parentPage.getTaskDto().getExecution();
		return TaskDtoExecutionStatus.SUSPENDED.equals(exec);
	}

}
