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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.datetime.markup.html.form.DateTextField;
import org.apache.wicket.extensions.yui.calendar.DatePicker;
import org.apache.wicket.extensions.yui.calendar.DateTimeField;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.string.StringValue;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.button.AjaxSubmitLinkButton;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.resources.PageResource;
import com.evolveum.midpoint.web.page.admin.resources.dto.ResourceDto;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskAddDto;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskAddResourcesDto;
import com.evolveum.midpoint.web.page.admin.users.PageUser;
import com.evolveum.midpoint.web.page.admin.users.dto.UsersDto;
import com.evolveum.midpoint.web.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.MisfireActionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ThreadStopActionType;

/**
 * @author lazyman
 * @author mserbak
 */
public class PageTaskAdd extends PageAdminTasks {
	private static final long serialVersionUID = 2317887071933841581L;

	private static final Trace LOGGER = TraceManager.getTrace(PageTaskAdd.class);
	private static final String DOT_CLASS = PageTaskAdd.class.getName() + ".";
	private static final String OPERATION_LOAD_RESOURCES = DOT_CLASS + "createResouceList";
	private static final String OPERATION_SAVE_TASK = DOT_CLASS + "saveTask";
	public static final String PARAM_TASK_ID = "taskOid";
	private IModel<TaskAddDto> model;

	public PageTaskAdd() {
		model = new LoadableModel<TaskAddDto>(false) {

			@Override
			protected TaskAddDto load() {
				return loadTask();
			}
		};
		initLayout();
	}

	private TaskAddDto loadTask() {
		return new TaskAddDto();
	}

	private void initLayout() {
		Form mainForm = new Form("mainForm");
		add(mainForm);

		final DropDownChoice resource = new DropDownChoice("resource",
				new PropertyModel<TaskAddResourcesDto>(model, "resource"),
				new AbstractReadOnlyModel<List<TaskAddResourcesDto>>() {

					@Override
					public List<TaskAddResourcesDto> getObject() {
						return createResouceList();
					}
				}, new IChoiceRenderer<TaskAddResourcesDto>() {

					@Override
					public Object getDisplayValue(TaskAddResourcesDto dto) {
						return dto.getName();
					}

					@Override
					public String getIdValue(TaskAddResourcesDto dto, int index) {
						return Integer.toString(index);
					}
				});
		resource.add(new VisibleEnableBehaviour() {

			@Override
			public boolean isEnabled() {
				TaskAddDto dto = model.getObject();
				boolean sync = dto.getType() == TaskCategory.LIVE_SYNCHRONIZATION;
				boolean cron = dto.getType() == TaskCategory.RECONCILIATION;
				return sync || cron;
			}
		});
		resource.setOutputMarkupId(true);
		mainForm.add(resource);
		DropDownChoice type = new DropDownChoice("type", new PropertyModel<String>(model, "type"),
				new AbstractReadOnlyModel<List<String>>() {

					@Override
					public List<String> getObject() {
						return createCategoryList();
					}
				}, new IChoiceRenderer<String>() {

					@Override
					public Object getDisplayValue(String item) {
						return PageTaskAdd.this.getString("pageTask.category." + item);
					}

					@Override
					public String getIdValue(String item, int index) {
						return Integer.toString(index);
					}

				});
		type.add(new AjaxFormComponentUpdatingBehavior("onChange") {

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				target.add(resource);
			}
		});
		mainForm.add(type);

		TextField<String> name = new TextField<String>("name", new PropertyModel<String>(model, "name"));
		name.setRequired(true);
		mainForm.add(name);

		initScheduling(mainForm);

		CheckBox runUntilNodeDown = new CheckBox("runUntilNodeDown", new PropertyModel<Boolean>(model,
				"runUntilNodeDown"));
		mainForm.add(runUntilNodeDown);

		initAdvanced(mainForm);

		initButtons(mainForm);
	}

	private void initScheduling(final Form mainForm) {
		final WebMarkupContainer container = new WebMarkupContainer("container");
		container.setOutputMarkupId(true);
		mainForm.add(container);

		final IModel<Boolean> recurringCheck = new PropertyModel<Boolean>(model, "reccuring");
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
		mainForm.add(recurring);

		AjaxCheckBox bound = new AjaxCheckBox("bound", boundCheck) {

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				target.add(container);
			}
		};
		boundContainer.add(bound);

		TextField<Integer> interval = new TextField<Integer>("interval", new PropertyModel<Integer>(model,
				"interval"));
		interval.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		if(recurringCheck.getObject()){
			interval.setRequired(true);
		}
		intervalContainer.add(interval);

		TextField<String> cron = new TextField<String>("cron", new PropertyModel<String>(model, "cron"));
		cron.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		if(recurringCheck.getObject() && !boundCheck.getObject()){
			cron.setRequired(true);
		}
		cronContainer.add(cron);

		final DateTimeField notStartBefore = new DateTimeField("notStartBeforeField",
				new PropertyModel<Date>(model, "notStopBefore")) {
			@Override
			protected DateTextField newDateTextField(String id, PropertyModel dateFieldModel) {
				return DateTextField.forDatePattern(id, dateFieldModel, "dd/MMM/yyyy");
			}
		};
		notStartBefore.setOutputMarkupId(true);
		mainForm.add(notStartBefore);

		final DateTimeField notStartAfter = new DateTimeField("notStartAfterField", new PropertyModel<Date>(
				model, "notStartAfter")) {
			@Override
			protected DateTextField newDateTextField(String id, PropertyModel dateFieldModel) {
				return DateTextField.forDatePattern(id, dateFieldModel, "dd/MMM/yyyy");
			}
		};
		notStartAfter.setOutputMarkupId(true);
		mainForm.add(notStartAfter);
	}

	private void initAdvanced(Form mainForm) {
		final IModel<Boolean> createSuspendedCheck = new PropertyModel<Boolean>(model, "suspendedState");
		CheckBox createSuspended = new CheckBox("createSuspended", createSuspendedCheck);
		mainForm.add(createSuspended);

		DropDownChoice threadStop = new DropDownChoice("threadStop", new Model<ThreadStopActionType>() {

			@Override
			public ThreadStopActionType getObject() {
				TaskAddDto dto = model.getObject();
				if (dto.getThreadStop() == null) {
					if (!dto.getRunUntilNodeDown()) {
						dto.setThreadStop(ThreadStopActionType.RESTART);
					} else {
						dto.setThreadStop(ThreadStopActionType.CLOSE);
					}
				}
				return dto.getThreadStop();
			}

			@Override
			public void setObject(ThreadStopActionType object) {
				model.getObject().setThreadStop(object);
			}
		}, MiscUtil.createReadonlyModelFromEnum(ThreadStopActionType.class),
				new EnumChoiceRenderer<ThreadStopActionType>(PageTaskAdd.this));
		mainForm.add(threadStop);

		DropDownChoice misfire = new DropDownChoice("misfire", new PropertyModel<MisfireActionType>(model,
				"misfireAction"), MiscUtil.createReadonlyModelFromEnum(MisfireActionType.class),
				new EnumChoiceRenderer<MisfireActionType>(PageTaskAdd.this));
		mainForm.add(misfire);
	}

	private void initButtons(final Form mainForm) {
		AjaxSubmitLinkButton saveButton = new AjaxSubmitLinkButton("saveButton",
				createStringResource("pageTask.button.save")) {

			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				savePerformed(target);
			}

			@Override
			protected void onError(AjaxRequestTarget target, Form<?> form) {
				target.add(getFeedbackPanel());
			}
		};
		mainForm.add(saveButton);

		AjaxLinkButton backButton = new AjaxLinkButton("backButton",
				createStringResource("pageTask.button.back")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				setResponsePage(PageTasks.class);
			}
		};
		mainForm.add(backButton);
	}

	private List<String> createCategoryList() {
		List<String> categories = new ArrayList<String>();

		// todo change to something better and add i18n
		TaskManager manager = getTaskManager();
		List<String> list = manager.getAllTaskCategories();
		if (list != null) {
			Collections.sort(list);
			for (String item : list) {
				if (item != TaskCategory.IMPORT_FROM_FILE && item != TaskCategory.WORKFLOW) {
					categories.add(item);
				}
			}
		}
		return categories;
	}

	private List<TaskAddResourcesDto> createResouceList() {
		OperationResult result = new OperationResult(OPERATION_LOAD_RESOURCES);
		Task task = createSimpleTask(OPERATION_LOAD_RESOURCES);
		List<PrismObject<ResourceType>> resources = null;
		List<TaskAddResourcesDto> resourceList = new ArrayList<TaskAddResourcesDto>();

		try {
			resources = getModelService().searchObjects(ResourceType.class, null, null, task, result);
			result.recomputeStatus();
		} catch (Exception ex) {
			result.recordFatalError("Couldn't get resource list.", ex);
			LoggingUtils.logException(LOGGER, "Couldn't get resource list", ex);
		}

		// todo show result somehow...
		// if (!result.isSuccess()) {
		// showResult(result);
		// }
		if (resources != null) {
			ResourceType item = null;
			for (PrismObject<ResourceType> resource : resources) {
				item = resource.asObjectable();
				resourceList.add(new TaskAddResourcesDto(item.getOid(), item.getName()));
			}
		}
		return resourceList;
	}

	private void savePerformed(AjaxRequestTarget target) {
		LOGGER.debug("Saving task changes.");
		OperationResult result = new OperationResult(OPERATION_SAVE_TASK);
		TaskAddDto dto = model.getObject();
		
		if(isEditingTask()){
			
		} else {
			
		}
	}

	private void browsePerformed(AjaxRequestTarget target) {
		// todo implement
	}

	private boolean isEditingTask() {
		StringValue taskOid = getPageParameters().get(PageTaskAdd.PARAM_TASK_ID);
		return taskOid != null && StringUtils.isNotEmpty(taskOid.toString());
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
