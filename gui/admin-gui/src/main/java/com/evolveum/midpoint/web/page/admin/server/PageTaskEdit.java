/*
 * Copyright (c) 2010-2013 Evolveum
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

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskBinding;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.button.ButtonType;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.LinkPanel;
import com.evolveum.midpoint.web.component.model.operationStatus.ModelOperationStatusDto;
import com.evolveum.midpoint.web.component.model.operationStatus.ModelOperationStatusPanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.server.dto.ScheduleValidator;
import com.evolveum.midpoint.web.page.admin.server.dto.StartEndDateValidator;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoExecutionStatus;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoProviderOptions;
import com.evolveum.midpoint.web.page.admin.server.subtasks.SubtasksPanel;
import com.evolveum.midpoint.web.page.admin.server.workflowInformation.WorkflowInformationPanel;
import com.evolveum.midpoint.web.resource.img.ImgResources;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.MisfireActionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ScheduleType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ThreadStopActionType;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.datetime.markup.html.form.DateTextField;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.extensions.yui.calendar.DateTimeField;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.string.StringValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @author lazyman
 * @author mserbak
 */
public class PageTaskEdit extends PageAdminTasks {
	private static final long serialVersionUID = -5933030498922903813L;

	private static final Trace LOGGER = TraceManager.getTrace(PageTaskEdit.class);
	private static final String DOT_CLASS = PageTaskAdd.class.getName() + ".";
	public static final String PARAM_TASK_EDIT_ID = "taskEditOid";
	private static final String OPERATION_LOAD_TASK = DOT_CLASS + "loadTask";
	private static final String OPERATION_SAVE_TASK = DOT_CLASS + "saveTask";

    private static final String ID_IDENTIFIER = "identifier";
    private static final String ID_HANDLER_URI_LIST = "handlerUriList";
    private static final String ID_HANDLER_URI = "handlerUri";
    private static final String ID_MODEL_OPERATION_STATUS_LABEL = "modelOperationStatusLabel";
    private static final String ID_MODEL_OPERATION_STATUS_PANEL = "modelOperationStatusPanel";
    private static final String ID_SUBTASKS_LABEL = "subtasksLabel";
    private static final String ID_SUBTASKS_PANEL = "subtasksPanel";
    private static final String ID_WORKFLOW_INFORMATION_LABEL = "workflowInformationLabel";
    private static final String ID_WORKFLOW_INFORMATION_PANEL = "workflowInformationPanel";
    private static final String ID_NAME = "name";
    private static final String ID_NAME_LABEL = "nameLabel";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_DESCRIPTION_LABEL = "descriptionLabel";
    private static final String ID_PARENT = "parent";
    private static final String ID_OPERATION_RESULT_PANEL = "operationResultPanel";

    private IModel<TaskDto> model;
	private static boolean edit = false;

    private PageParameters parameters;

    public PageTaskEdit() {
        this(new PageParameters(), null);
    }

    public PageTaskEdit(PageParameters parameters, PageBase previousPage) {

        this.parameters = parameters;
        setPreviousPage(previousPage);

		model = new LoadableModel<TaskDto>(false) {

			@Override
			protected TaskDto load() {
				return loadTask();
			}
		};

        edit = false;
        initLayout();
	}

    private boolean isRunnableOrRunning() {
        TaskDtoExecutionStatus exec = model.getObject().getExecution();
        return TaskDtoExecutionStatus.RUNNABLE.equals(exec) || TaskDtoExecutionStatus.RUNNING.equals(exec);
    }

    private boolean isRunning() {
        TaskDtoExecutionStatus exec = model.getObject().getExecution();
        return TaskDtoExecutionStatus.RUNNING.equals(exec);
    }

    private TaskDto loadTask() {
		OperationResult result = new OperationResult(OPERATION_LOAD_TASK);
        Task operationTask = getTaskManager().createTaskInstance(OPERATION_LOAD_TASK);

        StringValue taskOid = parameters.get(PARAM_TASK_EDIT_ID);

        TaskDto taskDto = null;
		try {
            Collection<SelectorOptions<GetOperationOptions>> options = GetOperationOptions.createRetrieveAttributesOptions(TaskType.F_SUBTASK);
            TaskType loadedTask = getModelService().getObject(TaskType.class, taskOid.toString(), options, operationTask, result).asObjectable();
            taskDto = prepareTaskDto(loadedTask, result);
			result.computeStatus();
		} catch (Exception ex) {
			result.recordFatalError("Couldn't get task.", ex);
		}

		if (!result.isSuccess()) {
			showResult(result);
		}

		if (taskDto == null) {
			getSession().error(getString("pageTaskEdit.message.cantTaskDetails"));
			if (!result.isSuccess()) {
				showResultInSession(result);
			}
            throw getRestartResponseException(PageTasks.class);
		}
		return taskDto;
	}

    private TaskDto prepareTaskDto(TaskType task, OperationResult result) throws SchemaException, ObjectNotFoundException {
        TaskDto taskDto = new TaskDto(task, getModelService(), getTaskService(), getModelInteractionService(), getTaskManager(), TaskDtoProviderOptions.fullOptions(), result);
        for (TaskType child : task.getSubtask()) {
            taskDto.addChildTaskDto(prepareTaskDto(child, result));
        }
        return taskDto;
    }

    private void initLayout() {
		Form mainForm = new Form("mainForm");
		add(mainForm);

		initMainInfo(mainForm);
		initSchedule(mainForm);

        VisibleEnableBehaviour hiddenWhenEditing = new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return !edit;
            }
        };

        VisibleEnableBehaviour hiddenWhenEditingOrNoSubtasks = new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return !edit && !model.getObject().getSubtasks().isEmpty();
            }
        };

        Label subtasksLabel = new Label(ID_SUBTASKS_LABEL, new ResourceModel("pageTaskEdit.subtasksLabel"));
        subtasksLabel.add(hiddenWhenEditingOrNoSubtasks);
        mainForm.add(subtasksLabel);
        SubtasksPanel subtasksPanel = new SubtasksPanel(ID_SUBTASKS_PANEL, new PropertyModel<List<TaskDto>>(model, TaskDto.F_SUBTASKS), getWorkflowManager().isEnabled());
        subtasksPanel.add(hiddenWhenEditingOrNoSubtasks);
        mainForm.add(subtasksPanel);

        VisibleEnableBehaviour hiddenWhenEditingOrNoWorkflowInformation = new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return !edit && model.getObject().isWorkflowShadowTask() && getWorkflowManager().isEnabled();
            }
        };

        Label workflowInformationLabel = new Label(ID_WORKFLOW_INFORMATION_LABEL, new ResourceModel("pageTaskEdit.workflowInformationLabel"));
        workflowInformationLabel.add(hiddenWhenEditingOrNoWorkflowInformation);
        mainForm.add(workflowInformationLabel);

        WorkflowInformationPanel workflowInformationPanel = new WorkflowInformationPanel(ID_WORKFLOW_INFORMATION_PANEL, model);
        workflowInformationPanel.add(hiddenWhenEditingOrNoWorkflowInformation);
        mainForm.add(workflowInformationPanel);

        VisibleEnableBehaviour modelOpBehaviour = new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return !edit && model.getObject().getModelOperationStatus() != null;
            }
        };
        Label modelOperationStatusLabel = new Label(ID_MODEL_OPERATION_STATUS_LABEL, new ResourceModel("pageTaskEdit.modelOperationStatusLabel"));
        modelOperationStatusLabel.add(modelOpBehaviour);
        mainForm.add(modelOperationStatusLabel);
        ModelOperationStatusPanel panel = new ModelOperationStatusPanel(ID_MODEL_OPERATION_STATUS_PANEL, new PropertyModel<ModelOperationStatusDto>(model, TaskDto.F_MODEL_OPERATION_STATUS));
        panel.add(modelOpBehaviour);
        mainForm.add(panel);

		SortableDataProvider<OperationResult, String> provider = new ListDataProvider<OperationResult>(this,
				new PropertyModel<List<OperationResult>>(model, "opResult"));
		TablePanel result = new TablePanel<OperationResult>("operationResult", provider, initResultColumns());
		result.setStyle("padding-top: 0px;");
		result.setShowPaging(false);
		result.setOutputMarkupId(true);
        result.add(hiddenWhenEditing);
		mainForm.add(result);

		DropDownChoice threadStop = new DropDownChoice("threadStop", new Model<ThreadStopActionType>() {

			@Override
			public ThreadStopActionType getObject() {
				return model.getObject().getThreadStop();
			}

			@Override
			public void setObject(ThreadStopActionType object) {
				model.getObject().setThreadStop(object);
			}
		}, WebMiscUtil.createReadonlyModelFromEnum(ThreadStopActionType.class),
				new EnumChoiceRenderer<ThreadStopActionType>(PageTaskEdit.this));
		threadStop.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isEnabled() {
				return edit;
			}
		});
		mainForm.add(threadStop);

		//mainForm.add(new TsaValidator(runUntilNodeDown, threadStop));

		initButtons(mainForm);
	}

	private void initMainInfo(Form mainForm) {
		RequiredTextField<String> name = new RequiredTextField<String>(ID_NAME, new PropertyModel<String>(
				model, TaskDto.F_NAME));
		name.add(new VisibleEnableBehaviour() {

			@Override
			public boolean isVisible() {
				return edit;
			}
		});
		name.add(new AttributeModifier("style", "width: 100%"));
		name.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		mainForm.add(name);

        Label nameLabel = new Label(ID_NAME_LABEL, new PropertyModel(model, TaskDto.F_NAME));
        nameLabel.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return !edit;
            }
        });
        mainForm.add(nameLabel);

        TextArea<String> description = new TextArea<String>(ID_DESCRIPTION, new PropertyModel<String>(
                model, TaskDto.F_DESCRIPTION));
        description.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return edit;
            }
        });
//        description.add(new AttributeModifier("style", "width: 100%"));
//        description.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        mainForm.add(description);

        Label descriptionLabel = new Label(ID_DESCRIPTION_LABEL, new PropertyModel(model, TaskDto.F_DESCRIPTION));
        descriptionLabel.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return !edit;
            }
        });
        mainForm.add(descriptionLabel);

        Label oid = new Label("oid", new PropertyModel(model, "oid"));
		mainForm.add(oid);

        mainForm.add(new Label(ID_IDENTIFIER, new PropertyModel(model, TaskDto.F_IDENTIFIER)));

		Label category = new Label("category", new PropertyModel(model, "category"));
		mainForm.add(category);

        LinkPanel parent = new LinkPanel(ID_PARENT, new PropertyModel(model, TaskDto.F_PARENT_TASK_NAME)) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                String oid = model.getObject().getParentTaskOid();
                if (oid != null) {
                    PageParameters parameters = new PageParameters();
                    parameters.add(PageTaskEdit.PARAM_TASK_EDIT_ID, oid);
                    setResponsePage(new PageTaskEdit(parameters, PageTaskEdit.this));
                }
            }
        };
        mainForm.add(parent);

        ListView<String> handlerUriList = new ListView<String>(ID_HANDLER_URI_LIST, new PropertyModel(model, TaskDto.F_HANDLER_URI_LIST)) {
            @Override
            protected void populateItem(ListItem<String> item) {
                item.add(new Label(ID_HANDLER_URI, item.getModelObject()));
            }
        };
        mainForm.add(handlerUriList);
//		Label uri = new Label(ID_HANDLER_URI, new PropertyModel(model, "uri"));
//		mainForm.add(uri);

		Label execution = new Label("execution", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
                TaskDtoExecutionStatus executionStatus = model.getObject().getExecution();
                if (executionStatus != TaskDtoExecutionStatus.CLOSED) {
				    return getString(TaskDtoExecutionStatus.class.getSimpleName() + "." + executionStatus.name());
                } else {
                    return getString(TaskDtoExecutionStatus.class.getSimpleName() + "." + executionStatus.name() + ".withTimestamp",
                            new AbstractReadOnlyModel<String>() {
                                @Override
                                public String getObject() {
                                    if (model.getObject().getCompletionTimestamp() != null) {
                                        return new Date(model.getObject().getCompletionTimestamp()).toLocaleString();   // todo correct formatting
                                    } else {
                                        return "?";
                                    }
                                }
                            });
                }
			}
		});
		mainForm.add(execution);

		Label node = new Label("node", new AbstractReadOnlyModel<String>() {
			@Override
			public String getObject() {
				TaskDto dto = model.getObject();
				if (!TaskDtoExecutionStatus.RUNNING.equals(dto.getExecution())) {
					return null;
				}
				return PageTaskEdit.this.getString("pageTaskEdit.message.node", dto.getExecutingAt());
			}
		});
		mainForm.add(node);
	}

	private void initSchedule(Form mainForm) {
        //todo probably can be removed, visibility can be updated in children (already components) [lazyman]
		final WebMarkupContainer container = new WebMarkupContainer("container");
		container.setOutputMarkupId(true);
		mainForm.add(container);

		final IModel<Boolean> recurringCheck = new PropertyModel<Boolean>(model, "recurring");
		final IModel<Boolean> boundCheck = new PropertyModel<Boolean>(model, "bound");
		
		WebMarkupContainer suspendReqRecurring = new WebMarkupContainer("suspendReqRecurring");
		suspendReqRecurring.add(new VisibleEnableBehaviour(){
			@Override
			public boolean isVisible() {
				return edit && isRunnableOrRunning();
			}
		});
		mainForm.add(suspendReqRecurring);

		final WebMarkupContainer boundContainer = new WebMarkupContainer("boundContainer");
		boundContainer.add(new VisibleEnableBehaviour() {

			@Override
			public boolean isVisible() {
				return recurringCheck.getObject();
			}

		});
		boundContainer.setOutputMarkupId(true);
		container.add(boundContainer);
		
		WebMarkupContainer suspendReqBound = new WebMarkupContainer("suspendReqBound");
		suspendReqBound.add(new VisibleEnableBehaviour(){
			@Override
			public boolean isVisible() {
				return edit && isRunnableOrRunning();
			}
		});
		boundContainer.add(suspendReqBound);

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
				target.add(PageTaskEdit.this.get("mainForm:recurring"));
			}
		};
		recurring.setOutputMarkupId(true);
		recurring.add(new VisibleEnableBehaviour() {

			@Override
			public boolean isEnabled() {
                return edit && !isRunnableOrRunning();
			}
		});
		mainForm.add(recurring);

		final AjaxCheckBox bound = new AjaxCheckBox("bound", boundCheck) {

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				target.add(container);
				target.add(PageTaskEdit.this.get("mainForm:recurring"));
			}
		};
		bound.add(new VisibleEnableBehaviour() {

			@Override
			public boolean isEnabled() {
				return edit && !isRunnableOrRunning();
			}
		});
		boundContainer.add(bound);
		
		final Image boundHelp = new Image("boundHelp", new PackageResourceReference(ImgResources.class,
				ImgResources.TOOLTIP_INFO));
		boundHelp.setOutputMarkupId(true);
		boundHelp.add(new AttributeAppender("original-title", getString("pageTaskEdit.boundHelp")));
		boundHelp.add(new AbstractDefaultAjaxBehavior() {

			@Override
			public void renderHead(Component component, IHeaderResponse response) {
				String js = "$('#"+ boundHelp.getMarkupId() +"').tipsy()";
				response.render(OnDomReadyHeaderItem.forScript(js));
				super.renderHead(component, response);
			}

			@Override
			protected void respond(AjaxRequestTarget target) {
			}
		});
		boundContainer.add(boundHelp);
		
		

		TextField<Integer> interval = new TextField<Integer>("interval",
				new PropertyModel<Integer>(model, "interval"));
		interval.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		interval.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isEnabled() {
				return edit && (!isRunnableOrRunning() || !boundCheck.getObject());
			}
		});
		intervalContainer.add(interval);

		TextField<String> cron = new TextField<String>("cron", new PropertyModel<String>(
				model, "cronSpecification"));
		cron.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		cron.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isEnabled() {
                return edit && (!isRunnableOrRunning() || !boundCheck.getObject());
			}
		});
		cronContainer.add(cron);
		
		final Image cronHelp = new Image("cronHelp", new PackageResourceReference(ImgResources.class,
				ImgResources.TOOLTIP_INFO));
		cronHelp.setOutputMarkupId(true);
		cronHelp.add(new AttributeAppender("original-title", getString("pageTaskEdit.cronHelp")));
		cronHelp.add(new AbstractDefaultAjaxBehavior() {
			@Override
			public void renderHead(Component component, IHeaderResponse response) {
				String js = "$('#"+ cronHelp.getMarkupId() +"').tipsy()";
				response.render(OnDomReadyHeaderItem.forScript(js));
				super.renderHead(component, response);
			}

			@Override
			protected void respond(AjaxRequestTarget target) {
			}
		});
		cronContainer.add(cronHelp);

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
				return edit && !isRunning();
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
				model, "misfireAction"), WebMiscUtil.createReadonlyModelFromEnum(MisfireActionType.class),
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
				return WebMiscUtil.formatDate(date);
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
				return WebMiscUtil.formatDate(date);
			}
		});
		mainForm.add(lastFinished);

		Label nextRun = new Label("nextRun", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				TaskDto dto = model.getObject();
                if (dto.getRecurring() && dto.getBound() && isRunning()) {
                    return getString("pageTasks.runsContinually");
                }
				if (dto.getNextRunStartTimeLong() == null) {
					return "-";
				}
				Date date = new Date(dto.getNextRunStartTimeLong());
				return WebMiscUtil.formatDate(date);
			}
		});
		mainForm.add(nextRun);

        mainForm.add(new StartEndDateValidator(notStartBefore, notStartAfter));
        mainForm.add(new ScheduleValidator(getTaskManager(), recurring, bound, interval, cron));
	}

	private void initButtons(final Form mainForm) {
		AjaxButton backButton = new AjaxButton("backButton", createStringResource("pageTaskEdit.button.back")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				edit = false;
                goBack(PageTasks.class);
			}
		};
		mainForm.add(backButton);

		AjaxSubmitButton saveButton = new AjaxSubmitButton("saveButton",
                createStringResource("pageTaskEdit.button.save")) {

			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				savePerformed(target);
			}

			@Override
			protected void onError(AjaxRequestTarget target, Form<?> form) {
				target.add(getFeedbackPanel());
			}

		};
		saveButton.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return edit;
			}
		});
		mainForm.add(saveButton);

        AjaxButton editButton = new AjaxButton("editButton",
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

	private List<IColumn<OperationResult, String>> initResultColumns() {
		List<IColumn<OperationResult, String>> columns = new ArrayList<IColumn<OperationResult, String>>();

		columns.add(new PropertyColumn(createStringResource("pageTaskEdit.opResult.token"), "token"));
		columns.add(new PropertyColumn(createStringResource("pageTaskEdit.opResult.operation"), "operation"));
		columns.add(new PropertyColumn(createStringResource("pageTaskEdit.opResult.status"), "status"));
		columns.add(new PropertyColumn(createStringResource("pageTaskEdit.opResult.message"), "message"));
		return columns;
	}

	private void savePerformed(AjaxRequestTarget target) {
		LOGGER.debug("Saving new task.");
		OperationResult result = new OperationResult(OPERATION_SAVE_TASK);
		TaskDto dto = model.getObject();
        Task operationTask = createSimpleTask(OPERATION_SAVE_TASK);
        TaskManager manager = getTaskManager();

		try {
            PrismObject<TaskType> originalTaskType = getModelService().getObject(TaskType.class, dto.getOid(), null, operationTask, result);
			Task originalTask = manager.createTaskInstance(originalTaskType, result);
			Task updatedTask = updateTask(dto, originalTask);

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Saving task modifications.");
			}
            getModelService().executeChanges(prepareChanges(updatedTask), null, operationTask, result);

			edit = false;
			setResponsePage(PageTasks.class);
			result.recomputeStatus();
		} catch (Exception ex) {
			result.recomputeStatus();
			result.recordFatalError("Couldn't save task.", ex);
			LoggingUtils.logException(LOGGER, "Couldn't save task modifications", ex);
		}
		showResultInSession(result);
		target.add(getFeedbackPanel());
	}

    private List<ObjectDelta<? extends ObjectType>> prepareChanges(Task updatedTask) {
        Collection<ItemDelta<?>> modifications = updatedTask.getPendingModifications();
        List<ObjectDelta<? extends ObjectType>> retval = new ArrayList<ObjectDelta<? extends ObjectType>>();
        retval.add(ObjectDelta.createModifyDelta(updatedTask.getOid(), modifications, TaskType.class, getPrismContext()));
        return retval;
    }

    private Task updateTask(TaskDto dto, Task existingTask) {

        if (!existingTask.getName().equals(dto.getName())) {
		    existingTask.setName(WebMiscUtil.createPolyFromOrigString(dto.getName()));
        }   // if they are equal, modifyObject complains ... it's probably a bug in repo; we'll fix it later?

        if ((existingTask.getDescription() == null && dto.getDescription() != null) ||
                (existingTask.getDescription() != null && !existingTask.getDescription().equals(dto.getDescription()))) {
            existingTask.setDescription(dto.getDescription());
        }

        if (!dto.getRecurring()) {
			existingTask.makeSingle();
		}
		existingTask.setBinding(dto.getBound() == true ? TaskBinding.TIGHT : TaskBinding.LOOSE);

        ScheduleType schedule = new ScheduleType();

        schedule.setEarliestStartTime(MiscUtil.asXMLGregorianCalendar(dto.getNotStartBefore()));
        schedule.setLatestStartTime(MiscUtil.asXMLGregorianCalendar(dto.getNotStartAfter()));
        schedule.setMisfireAction(dto.getMisfire());
        if (existingTask.getSchedule() != null) {
            schedule.setLatestFinishTime(existingTask.getSchedule().getLatestFinishTime());
        }

        if (dto.getRecurring() == true) {

		    if (dto.getBound() == false && dto.getCronSpecification() != null) {
                schedule.setCronLikePattern(dto.getCronSpecification());
            } else {
                schedule.setInterval(dto.getInterval());
            }
            existingTask.makeRecurring(schedule);
        } else {
            existingTask.makeSingle(schedule);
        }

        ThreadStopActionType tsa = dto.getThreadStop();
//        if (tsa == null) {
//            tsa = dto.getRunUntilNodeDown() ? ThreadStopActionType.CLOSE : ThreadStopActionType.RESTART;
//        }
        existingTask.setThreadStopAction(tsa);
		return existingTask;
	}

	private static class EmptyOnBlurAjaxFormUpdatingBehaviour extends AjaxFormComponentUpdatingBehavior {

		public EmptyOnBlurAjaxFormUpdatingBehaviour() {
			super("onBlur");
		}

		@Override
		protected void onUpdate(AjaxRequestTarget target) {
		}
	}

    @Override
    public PageBase reinitialize() {
        return new PageTaskEdit(parameters, getPreviousPage());
    }
}
