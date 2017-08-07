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
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.DateInput;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectTabPanel;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.web.page.admin.server.dto.ScheduleValidator;
import com.evolveum.midpoint.web.page.admin.server.dto.StartEndDateValidator;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MisfireActionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ThreadStopActionType;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.IFormValidator;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;

/**
 * @author semancik
 * @author lazyman
 * @author mserbak
 * @author mederly
 */
public class TaskSchedulingTabPanel extends AbstractObjectTabPanel<TaskType> implements TaskTabPanel {
	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(TaskSchedulingTabPanel.class);

	public static final String ID_LAST_STARTED_CONTAINER = "lastStartedContainer";
	public static final String ID_LAST_STARTED = "lastStarted";
	public static final String ID_LAST_STARTED_AGO = "lastStartedAgo";
	public static final String ID_LAST_FINISHED_CONTAINER = "lastFinishedContainer";
	public static final String ID_LAST_FINISHED = "lastFinished";
	public static final String ID_LAST_FINISHED_AGO = "lastFinishedAgo";
	public static final String ID_NEXT_RUN_CONTAINER = "nextRunContainer";
	public static final String ID_NEXT_RUN = "nextRun";
	public static final String ID_NEXT_RUN_IN = "nextRunIn";
	public static final String ID_NEXT_RETRY_CONTAINER = "nextRetryContainer";
	public static final String ID_NEXT_RETRY = "nextRetry";
	public static final String ID_NEXT_RETRY_IN = "nextRetryIn";

	public static final String ID_SCHEDULING_TABLE = "schedulingTable";
	public static final String ID_RECURRING_CONTAINER = "recurringContainer";
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
	public static final String ID_NOT_START_BEFORE_CONTAINER = "notStartBeforeContainer";
	public static final String ID_NOT_START_BEFORE_FIELD = "notStartBeforeField";
	public static final String ID_NOT_START_AFTER_CONTAINER = "notStartAfterContainer";
	public static final String ID_NOT_START_AFTER_FIELD = "notStartAfterField";
	public static final String ID_MISFIRE_ACTION_CONTAINER = "misfireActionContainer";
	public static final String ID_MISFIRE_ACTION = "misfireAction";
	public static final String ID_THREAD_STOP_CONTAINER = "threadStopContainer";
	public static final String ID_THREAD_STOP = "threadStop";
	public static final String ID_REQUIRED_CAPABILITY_CONTAINER = "requiredCapabilityContainer";
	public static final String ID_REQUIRED_CAPABILITY = "requiredCapability";

	private PageTaskEdit parentPage;
	private IModel<TaskDto> taskDtoModel;

	public TaskSchedulingTabPanel(String id, Form mainForm,
			LoadableModel<ObjectWrapper<TaskType>> taskWrapperModel,
			IModel<TaskDto> taskDtoModel, PageTaskEdit parentPage) {
		super(id, mainForm, taskWrapperModel, parentPage);
		this.taskDtoModel = taskDtoModel;
		this.parentPage = parentPage;
		initLayoutForInfoPanel();
		initLayoutForSchedulingTable();
		setOutputMarkupId(true);
	}

	private void initLayoutForInfoPanel() {

		// last start
		WebMarkupContainer lastStartedContainer = new WebMarkupContainer(ID_LAST_STARTED_CONTAINER);
		Label lastStart = new Label(ID_LAST_STARTED, new AbstractReadOnlyModel<String>() {
			@Override
			public String getObject() {
				TaskDto dto = taskDtoModel.getObject();
				if (dto.getLastRunStartTimestampLong() == null) {
					return "-";
				} else {
					return WebComponentUtil.formatDate(new Date(dto.getLastRunStartTimestampLong()));
				}
			}
		});
		lastStartedContainer.add(lastStart);

		Label lastStartAgo = new Label(ID_LAST_STARTED_AGO, new AbstractReadOnlyModel<String>() {
			@Override
			public String getObject() {
				TaskDto dto = taskDtoModel.getObject();
				if (dto.getLastRunStartTimestampLong() == null) {
					return "";
				} else {
					final long ago = System.currentTimeMillis() - dto.getLastRunStartTimestampLong();
					return createStringResource("TaskStatePanel.message.ago", DurationFormatUtils.formatDurationWords(ago, true, true)).getString();
				}
			}
		});
		lastStartedContainer.add(lastStartAgo);
		lastStartedContainer.add(parentPage.createVisibleIfAccessible(TaskType.F_LAST_RUN_START_TIMESTAMP));
		add(lastStartedContainer);

		// last finish
		WebMarkupContainer lastFinishedContainer = new WebMarkupContainer(ID_LAST_FINISHED_CONTAINER);
		Label lastFinished = new Label(ID_LAST_FINISHED, new AbstractReadOnlyModel<String>() {
			@Override
			public String getObject() {
				TaskDto dto = taskDtoModel.getObject();
				if (dto.getLastRunFinishTimestampLong() == null) {
					return "-";
				} else {
					return WebComponentUtil.formatDate(new Date(dto.getLastRunFinishTimestampLong()));
				}
			}
		});
		lastFinishedContainer.add(lastFinished);

		Label lastFinishedAgo = new Label(ID_LAST_FINISHED_AGO, new AbstractReadOnlyModel<String>() {
			@Override
			public String getObject() {
				TaskDto dto = taskDtoModel.getObject();
				if (dto.getLastRunFinishTimestampLong() == null) {
					return "";
				} else {
					Long duration;
					if (dto.getLastRunStartTimestampLong() == null || dto.getLastRunFinishTimestampLong() < dto.getLastRunStartTimestampLong()) {
						duration = null;
					} else {
						duration = dto.getLastRunFinishTimestampLong() - dto.getLastRunStartTimestampLong();
					}
					long ago = System.currentTimeMillis() - dto.getLastRunFinishTimestampLong();
					if (duration != null) {
						return getString("TaskStatePanel.message.durationAndAgo",
								DurationFormatUtils.formatDurationWords(ago, true, true),
								duration);
					} else {
						return getString("TaskStatePanel.message.ago",
								DurationFormatUtils.formatDurationWords(ago, true, true));
					}
				}
			}
		});
		lastFinishedContainer.add(lastFinishedAgo);
		lastFinishedContainer.add(parentPage.createVisibleIfAccessible(TaskType.F_LAST_RUN_FINISH_TIMESTAMP));
		add(lastFinishedContainer);

		WebMarkupContainer nextRunContainer = new WebMarkupContainer(ID_NEXT_RUN_CONTAINER);
		Label nextRun = new Label(ID_NEXT_RUN, new AbstractReadOnlyModel<String>() {
			@Override
			public String getObject() {
				TaskDto dto = taskDtoModel.getObject();
				if (dto.isRecurring() && dto.isBound() && dto.isRunning()) {
					return getString("pageTasks.runsContinually");
				} else if (dto.getNextRunStartTimeLong() == null) {
					return "-";
				} else {
					return WebComponentUtil.formatDate(new Date(dto.getNextRunStartTimeLong()));
				}
			}
		});
		nextRunContainer.add(nextRun);

		Label nextRunIn = new Label(ID_NEXT_RUN_IN, new AbstractReadOnlyModel<String>() {
			@Override
			public String getObject() {
				TaskDto dto = taskDtoModel.getObject();
				if (dto.getNextRunStartTimeLong() == null || (dto.isRecurring() && dto.isBound() && dto.isRunning())) {
					return "";
				} else {
					long currentTime = System.currentTimeMillis();
					final long in = dto.getNextRunStartTimeLong() - currentTime;
					if (in >= 0) {
						return getString("TaskStatePanel.message.in", DurationFormatUtils.formatDurationWords(in, true, true));
					} else {
						return "";
					}
				}
			}
		});
		nextRunContainer.add(nextRunIn);
		nextRunContainer.add(parentPage.createVisibleIfAccessible(TaskType.F_NEXT_RUN_START_TIMESTAMP));
		add(nextRunContainer);

		WebMarkupContainer nextRetryContainer = new WebMarkupContainer(ID_NEXT_RETRY_CONTAINER);
		Label nextRetry = new Label(ID_NEXT_RETRY, new AbstractReadOnlyModel<String>() {
			@Override
			public String getObject() {
				TaskDto dto = taskDtoModel.getObject();
				if (dto.getNextRetryTimeLong() == null) {
					return "-";
				} else {
					return WebComponentUtil.formatDate(new Date(dto.getNextRetryTimeLong()));
				}
			}
		});
		nextRetryContainer.add(nextRetry);

		Label nextRetryIn = new Label(ID_NEXT_RETRY_IN, new AbstractReadOnlyModel<String>() {
			@Override
			public String getObject() {
				TaskDto dto = taskDtoModel.getObject();
				if (dto.getNextRetryTimeLong() == null /* || (dto.isRecurring() && dto.isBound() && dto.isRunning()) */ ) {
					return "";
				} else {
					long currentTime = System.currentTimeMillis();
					final long in = dto.getNextRetryTimeLong() - currentTime;
					if (in >= 0) {
						return getString("TaskStatePanel.message.in", DurationFormatUtils.formatDurationWords(in, true, true));
					} else {
						return "";
					}
				}
			}
		});
		nextRetryContainer.add(nextRetryIn);
		nextRetryContainer.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return taskDtoModel.getObject().getNextRetryTimeLong() != null;
			}
		});
		add(nextRetryContainer);
	}
	
	private void initLayoutForSchedulingTable() {

		// models
		final IModel<Boolean> recurringCheckModel = new PropertyModel<>(taskDtoModel, TaskDto.F_RECURRING);
		final IModel<Boolean> boundCheckModel = new PropertyModel<Boolean>(taskDtoModel, TaskDto.F_BOUND);

		// behaviors
		final VisibleEnableBehaviour visibleIfEditAndRunnableOrRunning = new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return parentPage.isEdit() && parentPage.getTaskDto().isRunnableOrRunning();
			}
		};
		final VisibleEnableBehaviour visibleIfRecurringAndScheduleIsAccessible = new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return recurringCheckModel.getObject() && parentPage.isReadable(new ItemPath(TaskType.F_SCHEDULE));
			}
		};
		final VisibleEnableBehaviour visibleIfRecurringAndLooselyBoundAndScheduleIsAccessible = new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return recurringCheckModel.getObject() && !boundCheckModel.getObject() && parentPage.isReadable(new ItemPath(TaskType.F_SCHEDULE));
			}
		};
		final VisibleEnableBehaviour enabledIfEditAndNotRunningRunnableOrLooselyBoundAndScheduleIsEditable = new VisibleEnableBehaviour() {
			@Override
			public boolean isEnabled() {
				return parentPage.isEdit() && (!parentPage.getTaskDto().isRunnableOrRunning() || !boundCheckModel.getObject())
						&& parentPage.isEditable(new ItemPath(TaskType.F_SCHEDULE));
			}
		};
		final VisibleEnableBehaviour enabledIfEditAndNotRunningAndScheduleIsEditable = new VisibleEnableBehaviour() {
			@Override
			public boolean isEnabled() {
				return parentPage.isEdit() && !parentPage.getTaskDto().isRunning() && parentPage.isEditable(new ItemPath(TaskType.F_SCHEDULE));
			}
		};
		final VisibleEnableBehaviour enabledIfEditAndScheduleIsEditable = new VisibleEnableBehaviour() {
			@Override
			public boolean isEnabled() {
				return parentPage.isEdit() && parentPage.isEditable(new ItemPath(TaskType.F_SCHEDULE));
			}
		};
		final VisibleEnableBehaviour enabledIfEditAndThreadStopIsEditable = new VisibleEnableBehaviour() {
			@Override
			public boolean isEnabled() {
				return parentPage.isEdit() && parentPage.isEditable(new ItemPath(TaskType.F_THREAD_STOP_ACTION));
			}
		};
		final VisibleEnableBehaviour enabledIfEditAndRequiredCapabilityIsEditable = new VisibleEnableBehaviour() {
			@Override
			public boolean isEnabled() {
				return parentPage.isEdit() && parentPage.isEditable(new ItemPath(TaskType.F_REQUIRED_CAPABILITY));
			}
		};

		// components
		final WebMarkupContainer schedulingTable = new WebMarkupContainer(ID_SCHEDULING_TABLE);
		schedulingTable.setOutputMarkupId(true);
		add(schedulingTable);

		WebMarkupContainer recurringContainer = new WebMarkupContainer(ID_RECURRING_CONTAINER);
		AjaxCheckBox recurringCheck = new AjaxCheckBox(ID_RECURRING_CHECK, recurringCheckModel) {
			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				target.add(schedulingTable);
			}
			
			@Override
			   public boolean isEnabled() {
				   return parentPage.isEdit() && !parentPage.getTaskDto().isRunnableOrRunning() && parentPage.isEditable(TaskType.F_RECURRENCE);
			   }
		};
		recurringCheck.setOutputMarkupId(true);
//		recurringCheck.add(new VisibleEnableBehaviour() {
//							   @Override
//							   public boolean isEnabled() {
//								   return parentPage.isEdit() && !parentPage.getTaskDto().isRunnableOrRunning() && parentPage.isEditable(TaskType.F_RECURRENCE);
//							   }
//						   });
		recurringContainer.add(recurringCheck);

		WebMarkupContainer suspendReqRecurring = new WebMarkupContainer(ID_SUSPEND_REQ_RECURRING);
		suspendReqRecurring.add(visibleIfEditAndRunnableOrRunning);
		recurringContainer.add(suspendReqRecurring);
		recurringContainer.add(parentPage.createVisibleIfAccessible(TaskType.F_RECURRENCE));
		schedulingTable.add(recurringContainer);

		final WebMarkupContainer boundContainer = new WebMarkupContainer(ID_BOUND_CONTAINER);
		boundContainer.setOutputMarkupId(true);

		final AjaxCheckBox bound = new AjaxCheckBox(ID_BOUND_CHECK, boundCheckModel) {
			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				target.add(schedulingTable);
			}
			
			@Override
			public boolean isEnabled() {
				return parentPage.isEdit() && !parentPage.getTaskDto().isRunnableOrRunning() && parentPage.isEditable(TaskType.F_BINDING);
			}
		};
//		bound.add(new VisibleEnableBehaviour() {
//			@Override
//			public boolean isEnabled() {
//				return parentPage.isEdit() && !parentPage.getTaskDto().isRunnableOrRunning() && parentPage.isEditable(TaskType.F_BINDING);
//			}
//		});
		boundContainer.add(bound);

		WebMarkupContainer suspendReqBound = new WebMarkupContainer(ID_SUSPEND_REQ_BOUND);
		suspendReqBound.add(visibleIfEditAndRunnableOrRunning);
		boundContainer.add(suspendReqBound);
		boundContainer.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return recurringCheckModel.getObject() && parentPage.isReadable(new ItemPath(TaskType.F_BINDING));
			}
		});
		Label boundHelp = new Label(ID_BOUND_HELP);
		boundHelp.add(new InfoTooltipBehavior());
		boundContainer.add(boundHelp);
		schedulingTable.add(boundContainer);

		WebMarkupContainer intervalContainer = new WebMarkupContainer(ID_INTERVAL_CONTAINER);
		intervalContainer.add(visibleIfRecurringAndScheduleIsAccessible);
		intervalContainer.setOutputMarkupId(true);
		schedulingTable.add(intervalContainer);

		TextField<Integer> interval = new TextField<Integer>(ID_INTERVAL, new PropertyModel<Integer>(taskDtoModel, TaskDto.F_INTERVAL)) {
			@Override
			public boolean isEnabled() {
				return parentPage.isEdit() && (!parentPage.getTaskDto().isRunnableOrRunning() || !boundCheckModel.getObject())
						&& parentPage.isEditable(new ItemPath(TaskType.F_SCHEDULE));
			}
		};
		
		interval.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		interval.add(enabledIfEditAndNotRunningRunnableOrLooselyBoundAndScheduleIsEditable);
		intervalContainer.add(interval);

		WebMarkupContainer cronContainer = new WebMarkupContainer(ID_CRON_CONTAINER);
		cronContainer.add(visibleIfRecurringAndLooselyBoundAndScheduleIsAccessible);
		cronContainer.setOutputMarkupId(true);
		schedulingTable.add(cronContainer);

		TextField<String> cron = new TextField<>(ID_CRON, new PropertyModel<String>(taskDtoModel, TaskDto.F_CRON_SPECIFICATION));
		cron.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		cron.add(enabledIfEditAndNotRunningRunnableOrLooselyBoundAndScheduleIsEditable);
		cronContainer.add(cron);

		Label cronHelp = new Label(ID_CRON_HELP);
		cronHelp.add(new InfoTooltipBehavior());
		cronContainer.add(cronHelp);

		WebMarkupContainer notStartBeforeContainer = new WebMarkupContainer(ID_NOT_START_BEFORE_CONTAINER);
		DateInput notStartBefore = new DateInput(ID_NOT_START_BEFORE_FIELD, new PropertyModel<Date>(taskDtoModel, TaskDto.F_NOT_START_BEFORE));
		notStartBefore.setOutputMarkupId(true);
		notStartBefore.add(enabledIfEditAndNotRunningAndScheduleIsEditable);
		notStartBeforeContainer.add(notStartBefore);
		notStartBeforeContainer.add(parentPage.createVisibleIfAccessible(TaskType.F_SCHEDULE));
		schedulingTable.add(notStartBeforeContainer);

		WebMarkupContainer notStartAfterContainer = new WebMarkupContainer(ID_NOT_START_AFTER_CONTAINER);
		DateInput notStartAfter = new DateInput(ID_NOT_START_AFTER_FIELD, new PropertyModel<Date>(taskDtoModel, TaskDto.F_NOT_START_AFTER));
		notStartAfter.setOutputMarkupId(true);
		notStartAfter.add(enabledIfEditAndNotRunningAndScheduleIsEditable);
		notStartAfterContainer.add(notStartAfter);
		notStartAfterContainer.add(parentPage.createVisibleIfAccessible(TaskType.F_SCHEDULE));
		schedulingTable.add(notStartAfterContainer);

		WebMarkupContainer misfireActionContainer = new WebMarkupContainer(ID_MISFIRE_ACTION_CONTAINER);
		DropDownChoice misfire = new DropDownChoice(ID_MISFIRE_ACTION,
				new PropertyModel<MisfireActionType>(taskDtoModel, TaskDto.F_MISFIRE_ACTION),
				WebComponentUtil.createReadonlyModelFromEnum(MisfireActionType.class),
				new EnumChoiceRenderer<MisfireActionType>(parentPage));
		misfire.add(enabledIfEditAndScheduleIsEditable);
		misfireActionContainer.add(misfire);
		misfireActionContainer.add(parentPage.createVisibleIfAccessible(TaskType.F_SCHEDULE));
		schedulingTable.add(misfireActionContainer);

		WebMarkupContainer threadStopContainer = new WebMarkupContainer(ID_THREAD_STOP_CONTAINER);
		DropDownChoice threadStop = new DropDownChoice<>(ID_THREAD_STOP, new Model<ThreadStopActionType>() {

			@Override
			public ThreadStopActionType getObject() {
				return taskDtoModel.getObject().getThreadStopActionType();
			}

			@Override
			public void setObject(ThreadStopActionType object) {
				taskDtoModel.getObject().setThreadStopActionType(object);
			}
		}, WebComponentUtil.createReadonlyModelFromEnum(ThreadStopActionType.class),
				new EnumChoiceRenderer<ThreadStopActionType>(parentPage));
		threadStop.add(enabledIfEditAndThreadStopIsEditable);
		threadStopContainer.add(threadStop);
		threadStopContainer.add(parentPage.createVisibleIfAccessible(TaskType.F_THREAD_STOP_ACTION));
		schedulingTable.add(threadStopContainer);

		WebMarkupContainer requiredCapabilityContainer = new WebMarkupContainer(ID_REQUIRED_CAPABILITY_CONTAINER);
		TextField<String> requiredCapability = new TextField<>(ID_REQUIRED_CAPABILITY, new PropertyModel<String>(taskDtoModel, TaskDto.F_REQUIRED_CAPABILITY));
		requiredCapability.add(enabledIfEditAndRequiredCapabilityIsEditable);
		requiredCapabilityContainer.add(requiredCapability);
		requiredCapabilityContainer.add(parentPage.createVisibleIfAccessible(TaskType.F_REQUIRED_CAPABILITY));
		schedulingTable.add(requiredCapabilityContainer);

		org.apache.wicket.markup.html.form.Form<?> form = parentPage.getForm();
		// if not removed, the validators will accumulate on the form
		// TODO implement more intelligently when other tabs have validators as well
		for (IFormValidator validator : form.getFormValidators()) {
			form.remove(validator);
		}
		form.add(new StartEndDateValidator(notStartBefore, notStartAfter));
		form.add(new ScheduleValidator(parentPage.getTaskManager(), recurringCheck, bound, interval, cron));
	}

	@Override
	public Collection<Component> getComponentsToUpdate() {
		return Collections.<Component>singleton(this);
	}

}
