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
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author semancik
 *
 */
public class TaskMainPanel extends Panel {

	private static final String ID_MAIN_FORM = "mainForm";
	private static final String ID_TAB_PANEL = "tabPanel";
	private static final String ID_BUTTON_PANEL = "buttonPanel";
	private static final String ID_EDIT = "edit";
	private static final String ID_BACK = "back";
	private static final String ID_CANCEL_EDITING = "cancelEditing";
	private static final String ID_SAVE = "save";
	private static final String ID_SUSPEND = "suspend";
	private static final String ID_RESUME = "resume";
	private static final String ID_RUN_NOW = "runNow";
	private static final String ID_STOP_APPROVAL = "stopApproval";

	private static final Trace LOGGER = TraceManager.getTrace(TaskMainPanel.class);

	private final LoadableModel<ObjectWrapper<TaskType>> objectModel;
	private final IModel<TaskDto> taskDtoModel;
	private final IModel<Boolean> showAdvancedFeaturesModel;
	private final PageTaskEdit parentPage;

	public TaskMainPanel(String id, LoadableModel<ObjectWrapper<TaskType>> objectModel, IModel<TaskDto> taskDtoModel,
			IModel<Boolean> showAdvancedFeaturesModel, PageTaskEdit parentPage) {
		super(id, objectModel);
		this.objectModel = objectModel;
		this.taskDtoModel = taskDtoModel;
		this.showAdvancedFeaturesModel = showAdvancedFeaturesModel;
		this.parentPage = parentPage;
		initLayout();
	}

	private void initLayout() {
		Form mainForm = new Form<>(ID_MAIN_FORM, true);
		add(mainForm);
		initTabPanel(mainForm);
		initButtons(mainForm);
	}

	protected void initTabPanel(Form mainForm) {
		List<ITab> tabs = createTabs();
		TabbedPanel<ITab> tabPanel = WebComponentUtil.createTabPanel(ID_TAB_PANEL, parentPage, tabs, new TabbedPanel.RightSideItemProvider() {
			@Override
			public Component createRightSideItem(String id) {
				VisibleEnableBehaviour boxEnabled = new VisibleEnableBehaviour() {
					@Override
					public boolean isEnabled() {
						return !parentPage.isEdit();
					}
				};
				TaskShowAdvancedFeaturesPanel advancedFeaturesPanel = new TaskShowAdvancedFeaturesPanel(id, showAdvancedFeaturesModel, boxEnabled) {
					@Override
					protected void onAdvancedFeaturesUpdate(AjaxRequestTarget target) {
						target.add(getTabPanel());
						target.add(getButtonPanel());
						// we DO NOT call parentPage.refresh here because in edit mode this would erase any model changes
						// (well, because - for some strange reasons - even this code erases name and description input fields
						// occassionally, we hide 'advanced features' checkbox in editing mode)
					}
				};
				advancedFeaturesPanel.add(new VisibleEnableBehaviour() {
					@Override
					public boolean isVisible() {
						return parentPage.getTaskDto().isWorkflow();			// we don't distinguish between basic/advanced features for other task types yet
					}
				});
				return advancedFeaturesPanel;
			}
		});
		mainForm.add(tabPanel);
	}

	protected List<ITab> createTabs() {
		List<ITab> tabs = new ArrayList<>();
		final TaskTabsVisibility visibility = new TaskTabsVisibility();
		tabs.add(
				new AbstractTab(parentPage.createStringResource("pageTaskEdit.basic")) {
					@Override
					public WebMarkupContainer getPanel(String panelId) {
						return new TaskBasicTabPanel(panelId, getMainForm(), objectModel, taskDtoModel, parentPage);
					}
					@Override
					public boolean isVisible() {
						return visibility.computeBasicVisible(parentPage);
					}
				});
		tabs.add(
				new AbstractTab(parentPage.createStringResource("pageTaskEdit.scheduleTitle")) {
					@Override
					public WebMarkupContainer getPanel(String panelId) {
						return new TaskSchedulingTabPanel(panelId, getMainForm(), objectModel, taskDtoModel, parentPage);
					}
					@Override
					public boolean isVisible() {
						return visibility.computeSchedulingVisible(parentPage);
					}
				});
		tabs.add(
				new AbstractTab(parentPage.createStringResource("pageTaskEdit.subtasksAndThreads")) {
					@Override
					public WebMarkupContainer getPanel(String panelId) {
						return new TaskSubtasksAndThreadsTabPanel(panelId, getMainForm(), objectModel, taskDtoModel, parentPage);
					}
					@Override
					public boolean isVisible() {
						return visibility.computeSubtasksAndThreadsVisible(parentPage);
					}
				});
		tabs.add(
				new AbstractTab(parentPage.createStringResource("pageTaskEdit.progress")) {
					@Override
					public WebMarkupContainer getPanel(String panelId) {
						return new TaskProgressTabPanel(panelId, getMainForm(), objectModel, taskDtoModel, parentPage);
					}
					@Override
					public boolean isVisible() {
						return visibility.computeProgressVisible(parentPage);
					}
				});
		tabs.add(
				new AbstractTab(parentPage.createStringResource("pageTaskEdit.performance")) {
					@Override
					public WebMarkupContainer getPanel(String panelId) {
						return new TaskPerformanceTabPanel(panelId, getMainForm(), objectModel, taskDtoModel, parentPage);
					}
					@Override
					public boolean isVisible() {
						return visibility.computeEnvironmentalPerformanceVisible(parentPage);
					}
				});
		tabs.add(
				new AbstractTab(parentPage.createStringResource("pageTaskEdit.approvals")) {
					@Override
					public WebMarkupContainer getPanel(String panelId) {
						return new TaskApprovalsTabPanel(panelId, getMainForm(), objectModel, taskDtoModel, parentPage);
					}
					@Override
					public boolean isVisible() {
						return visibility.computeApprovalsVisible(parentPage);
					}
				});
		tabs.add(
				new AbstractTab(parentPage.createStringResource("pageTaskEdit.operation")) {
					@Override
					public WebMarkupContainer getPanel(String panelId) {
						return new TaskOperationTabPanel(panelId, getMainForm(), objectModel, taskDtoModel, parentPage);
					}
					@Override
					public boolean isVisible() {
						return visibility.computeOperationVisible(parentPage);
					}
				});
		tabs.add(
				new AbstractTab(parentPage.createStringResource("pageTaskEdit.result")) {
					@Override
					public WebMarkupContainer getPanel(String panelId) {
						return new TaskResultTabPanel(panelId, getMainForm(), objectModel, taskDtoModel, parentPage);
					}
					@Override
					public boolean isVisible() {
						return visibility.computeResultVisible(parentPage);
					}
				});
		tabs.add(
				new AbstractTab(parentPage.createStringResource("pageTaskEdit.errors")) {
					@Override
					public WebMarkupContainer getPanel(String panelId) {
						return new TaskErrorsTabPanel(panelId, getMainForm(), objectModel, taskDtoModel, parentPage);
					}
					@Override
					public boolean isVisible() {
						return visibility.computeErrorsVisible(parentPage);
					}
				});
		return tabs;
	}

	public Form getMainForm() {
		return (Form) get(ID_MAIN_FORM);
	}

	public TabbedPanel<ITab> getTabPanel() {
		return (TabbedPanel<ITab>) getMainForm().get(ID_TAB_PANEL);
	}

	public WebMarkupContainer getButtonPanel() {
		return (WebMarkupContainer) getMainForm().get(ID_BUTTON_PANEL);
	}

	private void initButtons(Form mainForm) {
		WebMarkupContainer buttonPanel = new WebMarkupContainer(ID_BUTTON_PANEL);
		buttonPanel.setOutputMarkupId(true);
		mainForm.add(buttonPanel);

		final TaskButtonsVisibility visibility = new TaskButtonsVisibility();

		AjaxButton backButton = new AjaxButton(ID_BACK, parentPage.createStringResource("pageTaskEdit.button.back")) {
			@Override
			public void onClick(AjaxRequestTarget target) {
				parentPage.getController().backPerformed(target);
			}
		};
		backButton.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return visibility.computeBackVisible(parentPage);
			}
		});
		buttonPanel.add(backButton);

		AjaxButton cancelEditingButton = new AjaxButton(ID_CANCEL_EDITING, parentPage.createStringResource("pageTaskEdit.button.cancelEditing")) {
			@Override
			public void onClick(AjaxRequestTarget target) {
				parentPage.getController().cancelEditingPerformed(target);
			}
		};
		cancelEditingButton.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return visibility.computeCancelEditVisible(parentPage);
			}
		});
		buttonPanel.add(cancelEditingButton);

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
				return visibility.computeSaveVisible(parentPage);
			}
		});
		mainForm.setDefaultButton(saveButton);
		buttonPanel.add(saveButton);

		AjaxButton editButton = new AjaxButton(ID_EDIT, parentPage.createStringResource("pageTaskEdit.button.edit")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				parentPage.setEdit(true);
				parentPage.refresh(target);		// stops refreshing as well
				target.add(getMainForm());
			}
		};
		editButton.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return visibility.computeEditVisible(parentPage);
			}
		});
		buttonPanel.add(editButton);

		AjaxButton suspend = new AjaxButton(ID_SUSPEND, parentPage.createStringResource("pageTaskEdit.button.suspend")) {
			@Override
			public void onClick(AjaxRequestTarget target) {
				parentPage.getController().suspendPerformed(target);
			}
		};
		suspend.add(new VisibleEnableBehaviour() {

			@Override
			public boolean isVisible() {
				return visibility.computeSuspendVisible(parentPage);
			}
		});
		buttonPanel.add(suspend);

		AjaxButton resume = new AjaxButton(ID_RESUME, parentPage.createStringResource("pageTaskEdit.button.resume")) {
			@Override
			public void onClick(AjaxRequestTarget target) {
				parentPage.getController().resumePerformed(target);
			}
		};
		resume.add(new VisibleEnableBehaviour() {

			@Override
			public boolean isVisible() {
				return visibility.computeResumeVisible(parentPage);
			}
		});
		buttonPanel.add(resume);

		AjaxButton runNow = new AjaxButton(ID_RUN_NOW, parentPage.createStringResource("pageTaskEdit.button.runNow")) {
			@Override
			public void onClick(AjaxRequestTarget target) {
				parentPage.getController().runNowPerformed(target);
			}
		};
		runNow.add(new VisibleEnableBehaviour() {

			@Override
			public boolean isVisible() {
				return visibility.computeRunNowVisible(parentPage);
			}
		});
		buttonPanel.add(runNow);

		AjaxButton stopApproval = new AjaxButton(ID_STOP_APPROVAL, parentPage.createStringResource("pageTaskEdit.button.stopApprovalProcess")) {
			@Override
			public void onClick(AjaxRequestTarget target) {
				parentPage.getController().stopApprovalProcessPerformed(target);
			}
		};
		stopApproval.add(new VisibleEnableBehaviour() {

			@Override
			public boolean isVisible() {
				return visibility.computeStopVisible(parentPage);
			}
		});
		buttonPanel.add(stopApproval);
	}

}
