/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.server;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.datetime.markup.html.form.DateTextField;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.extensions.yui.calendar.DateTimeField;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.string.StringValue;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.ClusterStatusInformation;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.resources.dto.ResourceObjectTypeDto;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoExecutionStatus;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoProvider;
import com.evolveum.midpoint.web.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.MisfireActionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ThreadStopActionType;

/**
 * @author lazyman
 * @author mserbak
 */
public class PageTaskEdit extends PageAdminTasks {
	private static final long serialVersionUID = -5933030498922903813L;

	private static final String DOT_CLASS = PageTaskAdd.class.getName() + ".";
	public static final String PARAM_TASK_EDIT_ID = "taskEditOid";
	private static final String OPERATION_LOAD_TASK = DOT_CLASS + "loadTask";
	private static final String OPERATION_SAVE_TASK = DOT_CLASS + "saveTask";
	private static final long ALLOWED_CLUSTER_INFO_AGE = 1200L;
	private IModel<TaskDto> model;
	private static boolean edit = false;

	public PageTaskEdit() {
		model = new LoadableModel<TaskDto>() {

			@Override
			protected TaskDto load() {
				return loadTask();
			}
		};
		initLayout();
	}

	private TaskDto loadTask() {
		OperationResult result = new OperationResult(OPERATION_LOAD_TASK);
		Task loadedTask = null;
		TaskManager manager = null;
		try {
			manager = getTaskManager();
			StringValue taskOid = getPageParameters().get(PARAM_TASK_EDIT_ID);
			loadedTask = manager.getTask(taskOid.toString(), result);
			result.recordSuccess();
		} catch (Exception ex) {
			result.recordFatalError("Couldn't get task.", ex);
		}

		if (!result.isSuccess()) {
			showResult(result);
		}

		if (loadedTask == null) {
			getSession().error(getString("pageTaskEdit.message.cantTaskDetails"));

			if (!result.isSuccess()) {
				showResultInSession(result);
			}
			throw new RestartResponseException(PageTasks.class);
		}
		ClusterStatusInformation info = manager.getRunningTasksClusterwide(ALLOWED_CLUSTER_INFO_AGE, result);
		return new TaskDto(loadedTask, info, manager);
	}

	private void initLayout() {
		Form mainForm = new Form("mainForm");
		add(mainForm);

		initMainInfo(mainForm);
		initSchedule(mainForm);

		SortableDataProvider<OperationResult> provider = new ListDataProvider<OperationResult>(this,
				new PropertyModel<List<OperationResult>>(model, "opResult"));
		TablePanel result = new TablePanel<OperationResult>("operationResult", provider, initResultColumns());
		result.setShowPaging(false);
		result.setOutputMarkupId(true);
		mainForm.add(result);

		CheckBox runUntilNodeDown = new CheckBox("runUntilNodeDown", new PropertyModel<Boolean>(model,
				"runUntilNodeDown"));
		runUntilNodeDown.add(new VisibleEnableBehaviour(){
			@Override
			public boolean isEnabled() {
				return edit;
			}
		});
		mainForm.add(runUntilNodeDown);

		DropDownChoice threadStop = new DropDownChoice("threadStop", new Model<ThreadStopActionType>() {

			@Override
			public ThreadStopActionType getObject() {
				return model.getObject().getThreadStop();
			}

			@Override
			public void setObject(ThreadStopActionType object) {
				model.getObject().setThreadStop(object);
			}
		}, MiscUtil.createReadonlyModelFromEnum(ThreadStopActionType.class),
				new EnumChoiceRenderer<ThreadStopActionType>(PageTaskEdit.this));
		threadStop.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isEnabled() {
				return edit;
			}
		});
		mainForm.add(threadStop);

		initButtons(mainForm);
	}

	private void initMainInfo(Form mainForm) {
		RequiredTextField<String> name = new RequiredTextField<String>("name", new PropertyModel<String>(
				model, "name"));
		name.add(new VisibleEnableBehaviour() {

			@Override
			public boolean isEnabled() {
				return edit;
			}
		});
		name.add(new SimpleAttributeModifier("style", "width: 100%"));
		name.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		mainForm.add(name);

		Label oid = new Label("oid", new PropertyModel(model, "oid"));
		mainForm.add(oid);

		Label category = new Label("category", new PropertyModel(model, "category"));
		mainForm.add(category);

		Label uri = new Label("uri", new PropertyModel(model, "uri"));
		mainForm.add(uri);

		Label execution = new Label("execution", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				return model.getObject().getExecution().name();
			}
		});
		mainForm.add(execution);

		Label node = new Label("node", new AbstractReadOnlyModel<String>() {
			@Override
			public String getObject() {
				TaskDto dto = model.getObject();
				if (dto.getExecution() == TaskDtoExecutionStatus.RUNNING) {
					return null;
				}
				return PageTaskEdit.this.getString("pageTaskEdit.message.node", dto.getExecutingAt());
			}
		});
		mainForm.add(node);
	}

	private void initSchedule(Form mainForm) {
		final WebMarkupContainer container = new WebMarkupContainer("container");
		container.setOutputMarkupId(true);
		mainForm.add(container);

		final IModel<Boolean> recurringCheck = new PropertyModel<Boolean>(model, "recurring");
		final IModel<Boolean> boundCheck = new PropertyModel<Boolean>(model, "bound");

		final WebMarkupContainer boundContainer = new WebMarkupContainer("boundContainer");
		boundContainer.add(new VisibleEnableBehaviour() {

			@Override
			public boolean isVisible() {
				return recurringCheck.getObject();
			}

		});
		boundContainer.setOutputMarkupId(true);
		container.add(boundContainer);

		final WebMarkupContainer intervalContainer = new WebMarkupContainer("intervalContainer");
		intervalContainer.add(new VisibleEnableBehaviour() {

			@Override
			public boolean isVisible() {
				return recurringCheck.getObject();
			}

		});
		intervalContainer.setOutputMarkupId(true);
		container.add(intervalContainer);

		final WebMarkupContainer cronContainer = new WebMarkupContainer("cronContainer");
		cronContainer.add(new VisibleEnableBehaviour() {

			@Override
			public boolean isVisible() {
				return recurringCheck.getObject() && !boundCheck.getObject();
			}

		});
		cronContainer.setOutputMarkupId(true);
		container.add(cronContainer);
		AjaxCheckBox recurring = new AjaxCheckBox("recurring", recurringCheck) {

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				target.add(container);
			}
		};
		recurring.add(new VisibleEnableBehaviour() {

			@Override
			public boolean isEnabled() {
				if (!edit) {
					return false;
				}
				TaskDto dto = model.getObject();
				return dto.getExecution() != TaskDtoExecutionStatus.RUNNABLE
						&& dto.getExecution() != TaskDtoExecutionStatus.RUNNING;
			}
		});
		mainForm.add(recurring);

		AjaxCheckBox bound = new AjaxCheckBox("bound", boundCheck) {

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				target.add(container);
			}
		};
		bound.add(new VisibleEnableBehaviour() {

			@Override
			public boolean isEnabled() {
				if (!edit) {
					return false;
				}
				TaskDto dto = model.getObject();
				return dto.getExecution() != TaskDtoExecutionStatus.RUNNABLE
						&& dto.getExecution() != TaskDtoExecutionStatus.RUNNING;
			}
		});
		boundContainer.add(bound);

		RequiredTextField<Integer> interval = new RequiredTextField<Integer>("interval",
				new PropertyModel<Integer>(model, "interval"));
		interval.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		interval.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isEnabled() {
				return edit;
			}
		});
		intervalContainer.add(interval);

		RequiredTextField<String> cron = new RequiredTextField<String>("cron", new PropertyModel<String>(
				model, "cronSpecification"));
		cron.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		cron.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isEnabled() {
				return edit;
			}
		});
		cronContainer.add(cron);

		final DateTimeField notStartBefore = new DateTimeField("notStartBeforeField",
				new PropertyModel<Date>(model, "notStartBefore")) {
			@Override
			protected DateTextField newDateTextField(String id, PropertyModel dateFieldModel) {
				return DateTextField.forDatePattern(id, dateFieldModel, "dd/MMM/yyyy");
			}
		};
		notStartBefore.setOutputMarkupId(true);
		notStartBefore.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isEnabled() {
				return edit;
			}
		});
		mainForm.add(notStartBefore);

		final DateTimeField notStartAfter = new DateTimeField("notStartAfterField", new PropertyModel<Date>(
				model, "notStartAfter")) {
			@Override
			protected DateTextField newDateTextField(String id, PropertyModel dateFieldModel) {
				return DateTextField.forDatePattern(id, dateFieldModel, "dd/MMM/yyyy");
			}
		};
		notStartAfter.setOutputMarkupId(true);
		notStartAfter.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isEnabled() {
				return edit;
			}
		});
		mainForm.add(notStartAfter);

		DropDownChoice misfire = new DropDownChoice("misfireAction", new PropertyModel<MisfireActionType>(
				model, "misfireAction"), MiscUtil.createReadonlyModelFromEnum(MisfireActionType.class),
				new EnumChoiceRenderer<MisfireActionType>(PageTaskEdit.this));
		misfire.add(new VisibleEnableBehaviour() {

			@Override
			public boolean isEnabled() {
				return edit;
			}
		});
		mainForm.add(misfire);

		Label lastStart = new Label("lastStarted", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				TaskDto dto = model.getObject();
				if (dto.getLastRunStartTimestampLong() == null) {
					return "-";
				}
				Date date = new Date(dto.getLastRunStartTimestampLong());
				SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, d. MMM yyyy HH:mm:ss");
				return dateFormat.format(date);
			}

		});
		mainForm.add(lastStart);

		Label lastFinished = new Label("lastFinished", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				TaskDto dto = model.getObject();
				if (dto.getLastRunFinishTimestampLong() == null) {
					return "-";
				}
				Date date = new Date(dto.getLastRunFinishTimestampLong());
				SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, d. MMM yyyy HH:mm:ss");
				return dateFormat.format(date);
			}
		});
		mainForm.add(lastFinished);

		Label nextRun = new Label("nextRun", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				TaskDto dto = model.getObject();
				if (dto.getNextRunStartTimeLong() == null) {
					return "-";
				}
				Date date = new Date(dto.getNextRunStartTimeLong());
				SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, d. MMM yyyy HH:mm:ss");
				return dateFormat.format(date);
			}
		});
		mainForm.add(nextRun);
	}

	private void initButtons(final Form mainForm) {
		AjaxLinkButton backButton = new AjaxLinkButton("backButton",
				createStringResource("pageTaskEdit.button.back")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				edit = false;
				setResponsePage(PageTasks.class);
			}
		};
		mainForm.add(backButton);

		AjaxLinkButton saveButton = new AjaxLinkButton("saveButton",
				createStringResource("pageTaskEdit.button.save")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				savePerformed(target);
			}
		};
		saveButton.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return edit;
			}
		});
		mainForm.add(saveButton);

		AjaxLinkButton editButton = new AjaxLinkButton("editButton",
				createStringResource("pageTaskEdit.button.edit")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				edit = true;
				target.add(mainForm);
			}
		};
		editButton.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return !edit;
			}
		});
		mainForm.add(editButton);
	}

	private List<IColumn<OperationResult>> initResultColumns() {
		List<IColumn<OperationResult>> columns = new ArrayList<IColumn<OperationResult>>();

		columns.add(new PropertyColumn(createStringResource("pageTaskEdit.opResult.token"), "token"));
		columns.add(new PropertyColumn(createStringResource("pageTaskEdit.opResult.operation"), "operation"));
		columns.add(new PropertyColumn(createStringResource("pageTaskEdit.opResult.status"), "status"));
		columns.add(new PropertyColumn(createStringResource("pageTaskEdit.opResult.message"), "message"));
		return columns;
	}

	private void savePerformed(AjaxRequestTarget target) {
		// TODO implement
	}

	private static class EmptyOnBlurAjaxFormUpdatingBehaviour extends AjaxFormComponentUpdatingBehavior {

		public EmptyOnBlurAjaxFormUpdatingBehaviour() {
			super("onBlur");
		}

		@Override
		protected void onUpdate(AjaxRequestTarget target) {
		}
	}
}
