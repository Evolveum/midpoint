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
import com.evolveum.midpoint.web.component.DateInput;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectTabPanel;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MisfireActionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ThreadStopActionType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import java.util.Date;

/**
 * @author semancik
 * @author lazyman
 * @author mserbak
 * @author mederly
 */
public class TaskSchedulingTabPanel extends AbstractObjectTabPanel<TaskType> {
	private static final long serialVersionUID = 1L;


	private static final Trace LOGGER = TraceManager.getTrace(TaskSchedulingTabPanel.class);

	public static final String ID_SCHEDULING_TABLE = "schedulingTable";
	public static final String ID_RECURRING_CHECK = "recurringCheck";
	public static final String ID_SUSPEND_REQ_RECURRING = "suspendReqRecurring";
	//public static final String ID_RECURRENT_TASKS_CONTAINER = "recurrentTasksContainer";
	public static final String ID_BOUND_CONTAINER = "boundContainer";
	public static final String ID_BOUND_HELP = "boundHelp";
	public static final String ID_BOUND_CHECK = "boundCheck";
	public static final String ID_SUSPEND_REQ_BOUND = "suspendReqBound";
	public static final String ID_INTERVAL_CONTAINER = "intervalContainer";
	public static final String ID_CRON_CONTAINER = "cronContainer";
	public static final String ID_INTERVAL = "interval";
	public static final String ID_CRON = "cron";
	public static final String ID_CRON_HELP = "cronHelp";
	public static final String ID_NOT_START_BEFORE_FIELD = "notStartBeforeField";
	public static final String ID_NOT_START_AFTER_FIELD = "notStartAfterField";
	public static final String ID_MISFIRE_ACTION = "misfireAction";
	public static final String ID_LAST_STARTED = "lastStarted";
	public static final String ID_LAST_FINISHED = "lastFinished";
	public static final String ID_NEXT_RUN = "nextRun";

	private PageTaskEdit parentPage;
	private IModel<TaskDto> taskDtoModel;

	public TaskSchedulingTabPanel(String id, Form mainForm,
			LoadableModel<ObjectWrapper<TaskType>> taskWrapperModel,
			LoadableModel<TaskDto> taskDtoModel, PageTaskEdit parentPage) {
		super(id, mainForm, taskWrapperModel, parentPage);
		this.taskDtoModel = taskDtoModel;
		this.parentPage = parentPage;
		initLayoutForInfoPanel();
		initLayoutForSchedulingTable();
	}

	private void initLayoutForInfoPanel() {
		Label lastStart = new Label(ID_LAST_STARTED, new AbstractReadOnlyModel<String>() {
			@Override
			public String getObject() {
				TaskDto dto = taskDtoModel.getObject();
				if (dto.getLastRunStartTimestampLong() == null) {
					return "-";
				}
				Date date = new Date(dto.getLastRunStartTimestampLong());
				return WebComponentUtil.formatDate(date);
			}
		});
		add(lastStart);

		Label lastFinished = new Label(ID_LAST_FINISHED, new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				TaskDto dto = taskDtoModel.getObject();
				if (dto.getLastRunFinishTimestampLong() == null) {
					return "-";
				}
				Date date = new Date(dto.getLastRunFinishTimestampLong());
				return WebComponentUtil.formatDate(date);
			}
		});
		add(lastFinished);

		Label nextRun = new Label(ID_NEXT_RUN, new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				TaskDto dto = taskDtoModel.getObject();
				if (dto.getRecurring() && dto.getBound() && parentPage.isRunning()) {
					return getString("pageTasks.runsContinually");
				}
				if (dto.getNextRunStartTimeLong() == null) {
					return "-";
				}
				Date date = new Date(dto.getNextRunStartTimeLong());
				return WebComponentUtil.formatDate(date);
			}
		});
		add(nextRun);
	}
	
	private void initLayoutForSchedulingTable() {

		// models
		final IModel<Boolean> recurringCheckModel = new PropertyModel<Boolean>(taskDtoModel, TaskDto.RECURRING);
		final IModel<Boolean> boundCheckModel = new PropertyModel<Boolean>(taskDtoModel, TaskDto.BOUND);

		// behaviors
		final VisibleEnableBehaviour visibleIfEditAndRunnableOrRunning = new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return parentPage.isEdit() && parentPage.isRunnableOrRunning();
			}
		};
		final VisibleEnableBehaviour enabledIfEditAndNotRunnableOrRunning = new VisibleEnableBehaviour() {
			@Override
			public boolean isEnabled() {
				return parentPage.isEdit() && !parentPage.isRunnableOrRunning();
			}
		};
		final VisibleEnableBehaviour visibleIfRecurring = new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return recurringCheckModel.getObject();
			}
		};
		final VisibleEnableBehaviour visibleIfRecurringAndLooselyBound = new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return recurringCheckModel.getObject() && !boundCheckModel.getObject();
			}
		};
		final VisibleEnableBehaviour enabledIfEditAndNotRunningRunnableOrLooselyBound = new VisibleEnableBehaviour() {
			@Override
			public boolean isEnabled() {
				return parentPage.isEdit() && (!parentPage.isRunnableOrRunning() || !boundCheckModel.getObject());
			}
		};
		final VisibleEnableBehaviour enabledIfEditAndNotRunning = new VisibleEnableBehaviour() {
			@Override
			public boolean isEnabled() {
				return parentPage.isEdit() && !parentPage.isRunning();
			}
		};
		final VisibleEnableBehaviour enabledIfEdit = new VisibleEnableBehaviour() {
			@Override
			public boolean isEnabled() {
				return parentPage.isEdit();
			}
		};

		// components
		final WebMarkupContainer schedulingTable = new WebMarkupContainer(ID_SCHEDULING_TABLE);
		schedulingTable.setOutputMarkupId(true);
		add(schedulingTable);

		AjaxCheckBox recurringCheck = new AjaxCheckBox(ID_RECURRING_CHECK, recurringCheckModel) {
			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				target.add(schedulingTable);
			}
		};
		recurringCheck.setOutputMarkupId(true);
		recurringCheck.add(enabledIfEditAndNotRunnableOrRunning);
		schedulingTable.add(recurringCheck);

		WebMarkupContainer suspendReqRecurring = new WebMarkupContainer(ID_SUSPEND_REQ_RECURRING);
		suspendReqRecurring.add(visibleIfEditAndRunnableOrRunning);
		schedulingTable.add(suspendReqRecurring);

		final WebMarkupContainer boundContainer = new WebMarkupContainer(ID_BOUND_CONTAINER);
		boundContainer.add(visibleIfRecurring);
		boundContainer.setOutputMarkupId(true);
		schedulingTable.add(boundContainer);

		final AjaxCheckBox bound = new AjaxCheckBox(ID_BOUND_CHECK, boundCheckModel) {
			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				target.add(schedulingTable);
			}
		};
		bound.add(enabledIfEditAndNotRunnableOrRunning);
		boundContainer.add(bound);

		WebMarkupContainer suspendReqBound = new WebMarkupContainer(ID_SUSPEND_REQ_BOUND);
		suspendReqBound.add(visibleIfEditAndRunnableOrRunning);
		boundContainer.add(suspendReqBound);

		final WebMarkupContainer intervalContainer = new WebMarkupContainer(ID_INTERVAL_CONTAINER);
		intervalContainer.add(visibleIfRecurring);
		intervalContainer.setOutputMarkupId(true);
		schedulingTable.add(intervalContainer);

		final WebMarkupContainer cronContainer = new WebMarkupContainer(ID_CRON_CONTAINER);
		cronContainer.add(visibleIfRecurringAndLooselyBound);
		cronContainer.setOutputMarkupId(true);
		schedulingTable.add(cronContainer);

		Label boundHelp = new Label(ID_BOUND_HELP);
		boundHelp.add(new InfoTooltipBehavior());
		boundContainer.add(boundHelp);

		TextField<Integer> interval = new TextField<>(ID_INTERVAL, new PropertyModel<Integer>(taskDtoModel, TaskDto.F_INTERVAL));
		interval.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		interval.add(enabledIfEditAndNotRunningRunnableOrLooselyBound);
		intervalContainer.add(interval);

		TextField<String> cron = new TextField<>(ID_CRON, new PropertyModel<String>(taskDtoModel, TaskDto.CRON_SPECIFICATION));
		cron.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		cron.add(enabledIfEditAndNotRunningRunnableOrLooselyBound);
		cronContainer.add(cron);

		Label cronHelp = new Label(ID_CRON_HELP);
		cronHelp.add(new InfoTooltipBehavior());
		cronContainer.add(cronHelp);

		DateInput notStartBefore = new DateInput(ID_NOT_START_BEFORE_FIELD, new PropertyModel<Date>(taskDtoModel, TaskDto.F_NOT_START_BEFORE));
		notStartBefore.setOutputMarkupId(true);
		notStartBefore.add(enabledIfEditAndNotRunning);
		schedulingTable.add(notStartBefore);

		DateInput notStartAfter = new DateInput(ID_NOT_START_AFTER_FIELD, new PropertyModel<Date>(taskDtoModel, TaskDto.F_NOT_START_AFTER));
		notStartAfter.setOutputMarkupId(true);
		notStartAfter.add(enabledIfEdit);
		schedulingTable.add(notStartAfter);

		DropDownChoice misfire = new DropDownChoice(ID_MISFIRE_ACTION,
				new PropertyModel<MisfireActionType>(taskDtoModel, TaskDto.F_MISFIRE_ACTION),
				WebComponentUtil.createReadonlyModelFromEnum(MisfireActionType.class),
				new EnumChoiceRenderer<MisfireActionType>(parentPage));
		misfire.add(enabledIfEdit);
		schedulingTable.add(misfire);

		DropDownChoice threadStop = new DropDownChoice<>("threadStop", new Model<ThreadStopActionType>() {

			@Override
			public ThreadStopActionType getObject() {
				return taskDtoModel.getObject().getThreadStop();
			}

			@Override
			public void setObject(ThreadStopActionType object) {
				taskDtoModel.getObject().setThreadStop(object);
			}
		}, WebComponentUtil.createReadonlyModelFromEnum(ThreadStopActionType.class),
				new EnumChoiceRenderer<ThreadStopActionType>(parentPage));
		threadStop.add(enabledIfEdit);
		schedulingTable.add(threadStop);

		//add(new StartEndDateValidator(notStartBefore, notStartAfter));
		//add(new ScheduleValidator(parentPage.getTaskManager(), recurring, bound, interval, cron));

	}

}
