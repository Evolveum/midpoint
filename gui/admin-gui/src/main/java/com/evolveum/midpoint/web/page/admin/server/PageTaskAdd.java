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
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.server.dto.*;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.datetime.markup.html.form.DateTextField;
import org.apache.wicket.extensions.yui.calendar.DateTimeField;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author lazyman
 * @author mserbak
 */
@PageDescriptor(url = "/admin/tasks/addTask", action = {
        @AuthorizationAction(actionUri = PageAdminTasks.AUTHORIZATION_TASKS_ALL,
                label = PageAdminTasks.AUTH_TASKS_ALL_LABEL,
                description = PageAdminTasks.AUTH_TASKS_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.NS_AUTHORIZATION + "#taskAdd",
                label = "PageTaskAdd.auth.taskAdd.label",
                description = "PageTaskAdd.auth.taskAdd.description")})
public class PageTaskAdd extends PageAdminTasks {

    private static final long serialVersionUID = 2317887071933841581L;

    private static final String ID_DRY_RUN = "dryRun";
    private static final String ID_KIND = "kind";
    private static final String ID_INTENT = "intent";
    private static final String ID_FORM_MAIN = "mainForm";
    private static final String ID_NAME = "name";
    private static final String ID_CATEGORY = "category";
    private static final String ID_RESOURCE = "resource";
    private static final String ID_RUN_UNTIL_NODW_DOWN = "runUntilNodeDown";
    private static final String ID_CREATE_SUSPENDED = "createSuspended";
    private static final String ID_THREAD_STOP = "threadStop";
    private static final String ID_MISFIRE_ACTION = "misfireAction";
    private static final String ID_RECURRING = "recurring";
    private static final String ID_CONTAINER = "container";
    private static final String ID_BOUND_CONTAINER = "boundContainer";
    private static final String ID_BOUND_HELP = "boundHelp";
    private static final String ID_BOUND = "bound";
    private static final String ID_INTERVAL_CONTAINER = "intervalContainer";
    private static final String ID_INTERVAL = "interval";
    private static final String ID_CRON_CONTAINER = "cronContainer";
    private static final String ID_CRON = "cron";
    private static final String ID_CRON_HELP = "cronHelp";
    private static final String ID_NO_START_BEFORE_FIELD = "notStartBeforeField";
    private static final String ID_NO_START_AFTER_FIELD = "notStartAfterField";
    private static final String ID_BUTTON_BACK = "backButton";
    private static final String ID_BUTTON_SAVE = "saveButton";

    private static final Trace LOGGER = TraceManager.getTrace(PageTaskAdd.class);
    private static final String DOT_CLASS = PageTaskAdd.class.getName() + ".";
    private static final String OPERATION_LOAD_RESOURCES = DOT_CLASS + "createResourceList";
    private static final String OPERATION_SAVE_TASK = DOT_CLASS + "saveTask";
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
        Form mainForm = new Form(ID_FORM_MAIN);
        add(mainForm);

        final DropDownChoice resource = new DropDownChoice(ID_RESOURCE,
                new PropertyModel<TaskAddResourcesDto>(model, TaskAddDto.F_RESOURCE),
                new AbstractReadOnlyModel<List<TaskAddResourcesDto>>() {

                    @Override
                    public List<TaskAddResourcesDto> getObject() {
                        return createResourceList();
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
                boolean sync = TaskCategory.LIVE_SYNCHRONIZATION.equals(dto.getCategory());
                boolean recon = TaskCategory.RECONCILIATION.equals(dto.getCategory());
                boolean importAccounts = TaskCategory.IMPORTING_ACCOUNTS.equals(dto.getCategory());
                return sync || recon || importAccounts;
            }
        });
        resource.setOutputMarkupId(true);
        mainForm.add(resource);

        final DropDownChoice kind = new DropDownChoice(ID_KIND,
                new PropertyModel<ShadowKindType>(model, TaskAddDto.F_KIND),
                new AbstractReadOnlyModel<List<ShadowKindType>>() {

                    @Override
                    public List<ShadowKindType> getObject() {
                        return createShadowKindTypeList();
                    }
                }, new IChoiceRenderer<ShadowKindType>() {

            @Override
            public Object getDisplayValue(ShadowKindType object) {
                return object.value();
            }

            @Override
            public String getIdValue(ShadowKindType object, int index) {
                return Integer.toString(index);
            }
        });
        kind.setOutputMarkupId(true);
        kind.add(new VisibleEnableBehaviour(){

            @Override
            public boolean isEnabled() {
                TaskAddDto dto = model.getObject();
                return TaskCategory.RECONCILIATION.equals(dto.getCategory());
            }
        });
        mainForm.add(kind);

        final TextField<String> intent = new TextField<>(ID_INTENT, new PropertyModel<String>(model, TaskAddDto.F_INTENT));
        mainForm.add(intent);
        intent.setOutputMarkupId(true);
        intent.add(new VisibleEnableBehaviour(){

            @Override
            public boolean isEnabled() {
                TaskAddDto dto = model.getObject();
                return TaskCategory.RECONCILIATION.equals(dto.getCategory());
            }
        });

        DropDownChoice type = new DropDownChoice(ID_CATEGORY, new PropertyModel<String>(model, TaskAddDto.F_CATEGORY),
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
                target.add(intent);
                target.add(kind);
            }
        });
        type.setRequired(true);
        mainForm.add(type);

        TextField<String> name = new TextField<>(ID_NAME, new PropertyModel<String>(model, TaskAddDto.F_NAME));
        name.setRequired(true);
        mainForm.add(name);

        initScheduling(mainForm);
        initAdvanced(mainForm);

        CheckBox dryRun = new CheckBox(ID_DRY_RUN, new PropertyModel<Boolean>(model, TaskAddDto.F_DRY_RUN));
        mainForm.add(dryRun);

        initButtons(mainForm);
    }

    private void initScheduling(final Form mainForm) {
        final WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        mainForm.add(container);

        final IModel<Boolean> recurringCheck = new PropertyModel<>(model, TaskAddDto.F_RECURRING);
        final IModel<Boolean> boundCheck = new PropertyModel<>(model, TaskAddDto.F_BOUND);

        final WebMarkupContainer boundContainer = new WebMarkupContainer(ID_BOUND_CONTAINER);
        boundContainer.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return recurringCheck.getObject();
            }

        });
        boundContainer.setOutputMarkupId(true);
        container.add(boundContainer);

        final WebMarkupContainer intervalContainer = new WebMarkupContainer(ID_INTERVAL_CONTAINER);
        intervalContainer.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return recurringCheck.getObject();
            }

        });
        intervalContainer.setOutputMarkupId(true);
        container.add(intervalContainer);

        final WebMarkupContainer cronContainer = new WebMarkupContainer(ID_CRON_CONTAINER);
        cronContainer.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return recurringCheck.getObject() && !boundCheck.getObject();
            }

        });
        cronContainer.setOutputMarkupId(true);
        container.add(cronContainer);

        AjaxCheckBox recurring = new AjaxCheckBox(ID_RECURRING, recurringCheck) {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(container);
            }
        };
        mainForm.add(recurring);

        AjaxCheckBox bound = new AjaxCheckBox(ID_BOUND, boundCheck) {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(container);
            }
        };
        boundContainer.add(bound);

        Label boundHelp = new Label(ID_BOUND_HELP);
        boundHelp.add(new InfoTooltipBehavior());
        boundContainer.add(boundHelp);

        TextField<Integer> interval = new TextField<>(ID_INTERVAL,
                new PropertyModel<Integer>(model, TaskAddDto.F_INTERVAL));
        interval.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        intervalContainer.add(interval);

        TextField<String> cron = new TextField<>(ID_CRON, new PropertyModel<String>(
                model, TaskAddDto.F_CRON));
        cron.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
//		if (recurringCheck.getObject() && !boundCheck.getObject()) {
//			cron.setRequired(true);
//		}
        cronContainer.add(cron);

        Label cronHelp = new Label(ID_CRON_HELP);
        cronHelp.add(new InfoTooltipBehavior());
        cronContainer.add(cronHelp);

        final DateTimeField notStartBefore = new DateTimeField(ID_NO_START_BEFORE_FIELD,
                new PropertyModel<Date>(model, TaskAddDto.F_NOT_START_BEFORE)) {
            @Override
            protected DateTextField newDateTextField(String id, PropertyModel dateFieldModel) {
                return DateTextField.forDatePattern(id, dateFieldModel, "dd/MMM/yyyy"); // todo i18n
            }
        };
        notStartBefore.setOutputMarkupId(true);
        mainForm.add(notStartBefore);

        final DateTimeField notStartAfter = new DateTimeField(ID_NO_START_AFTER_FIELD, new PropertyModel<Date>(
                model, TaskAddDto.F_NOT_START_AFTER)) {
            @Override
            protected DateTextField newDateTextField(String id, PropertyModel dateFieldModel) {
                return DateTextField.forDatePattern(id, dateFieldModel, "dd/MMM/yyyy"); // todo i18n
            }
        };
        notStartAfter.setOutputMarkupId(true);
        mainForm.add(notStartAfter);

        mainForm.add(new StartEndDateValidator(notStartBefore, notStartAfter));
        mainForm.add(new ScheduleValidator(getTaskManager(), recurring, bound, interval, cron));

    }

    private void initAdvanced(Form mainForm) {
        CheckBox runUntilNodeDown = new CheckBox(ID_RUN_UNTIL_NODW_DOWN, new PropertyModel<Boolean>(model,
                TaskAddDto.F_RUN_UNTIL_NODW_DOWN));
        mainForm.add(runUntilNodeDown);

        final IModel<Boolean> createSuspendedCheck = new PropertyModel<>(model, TaskAddDto.F_SUSPENDED_STATE);
        CheckBox createSuspended = new CheckBox(ID_CREATE_SUSPENDED, createSuspendedCheck);
        mainForm.add(createSuspended);

        DropDownChoice threadStop = new DropDownChoice(ID_THREAD_STOP, new Model<ThreadStopActionType>() {

            @Override
            public ThreadStopActionType getObject() {
                TaskAddDto dto = model.getObject();
//				if (dto.getThreadStop() == null) {
//					if (!dto.getRunUntilNodeDown()) {
//						dto.setThreadStop(ThreadStopActionType.RESTART);
//					} else {
//						dto.setThreadStop(ThreadStopActionType.CLOSE);
//					}
//				}
                return dto.getThreadStop();
            }

            @Override
            public void setObject(ThreadStopActionType object) {
                model.getObject().setThreadStop(object);
            }
        }, WebMiscUtil.createReadonlyModelFromEnum(ThreadStopActionType.class),
                new EnumChoiceRenderer<ThreadStopActionType>(PageTaskAdd.this));
        mainForm.add(threadStop);

        mainForm.add(new TsaValidator(runUntilNodeDown, threadStop));

        DropDownChoice misfire = new DropDownChoice(ID_MISFIRE_ACTION, new PropertyModel<MisfireActionType>(
                model, TaskAddDto.F_MISFIRE_ACTION), WebMiscUtil.createReadonlyModelFromEnum(MisfireActionType.class),
                new EnumChoiceRenderer<MisfireActionType>(PageTaskAdd.this));
        mainForm.add(misfire);
    }

    private void initButtons(final Form mainForm) {
        AjaxSubmitButton saveButton = new AjaxSubmitButton(ID_BUTTON_SAVE,
                createStringResource("PageBase.button.save")) {

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

        AjaxButton backButton = new AjaxButton(ID_BUTTON_BACK,
                createStringResource("PageBase.button.back")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                setResponsePage(PageTasks.class);
            }
        };
        mainForm.add(backButton);
    }

    private List<String> createCategoryList() {
        List<String> categories = new ArrayList<>();

        // todo change to something better and add i18n
//		TaskManager manager = getTaskManager();
//		List<String> list = manager.getAllTaskCategories();
//		if (list != null) {
//			Collections.sort(list);
//			for (String item : list) {
//				if (item != TaskCategory.IMPORT_FROM_FILE && item != TaskCategory.WORKFLOW) {
//					categories.add(item);
//				}
//			}
//		}
        categories.add(TaskCategory.LIVE_SYNCHRONIZATION);
        categories.add(TaskCategory.RECONCILIATION);
        categories.add(TaskCategory.IMPORTING_ACCOUNTS);
        categories.add(TaskCategory.USER_RECOMPUTATION);
        categories.add(TaskCategory.DEMO);
        return categories;
    }

    private List<ShadowKindType> createShadowKindTypeList(){
        List<ShadowKindType> kindList = new ArrayList<>();

        kindList.add(ShadowKindType.ACCOUNT);
        kindList.add(ShadowKindType.ENTITLEMENT);
        kindList.add(ShadowKindType.GENERIC);

        return kindList;
    }

    private List<TaskAddResourcesDto> createResourceList() {
        OperationResult result = new OperationResult(OPERATION_LOAD_RESOURCES);
        Task task = createSimpleTask(OPERATION_LOAD_RESOURCES);
        List<PrismObject<ResourceType>> resources = null;
        List<TaskAddResourcesDto> resourceList = new ArrayList<TaskAddResourcesDto>();

        try {
            resources = getModelService().searchObjects(ResourceType.class, new ObjectQuery(), null, task, result);
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
                resourceList.add(new TaskAddResourcesDto(item.getOid(), WebMiscUtil.getOrigStringFromPoly(item.getName())));
            }
        }
        return resourceList;
    }

    private void savePerformed(AjaxRequestTarget target) {
        LOGGER.debug("Saving new task.");
        OperationResult result = new OperationResult(OPERATION_SAVE_TASK);
        TaskAddDto dto = model.getObject();

        Task operationTask = createSimpleTask(OPERATION_SAVE_TASK);
        try {
            TaskType task = createTask(dto);

            getPrismContext().adopt(task);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Adding new task.");
            }
            getModelService().executeChanges(prepareChangesToExecute(task), null, operationTask, result);
            result.recomputeStatus();
            setResponsePage(PageTasks.class);
        } catch (Exception ex) {
            result.recomputeStatus();
            result.recordFatalError("Unable to save task.", ex);
            LoggingUtils.logException(LOGGER, "Couldn't add new task", ex);
        }
        showResultInSession(result);
        target.add(getFeedbackPanel());
    }

    private List<ObjectDelta<? extends ObjectType>> prepareChangesToExecute(TaskType taskToBeAdded) {
        List<ObjectDelta<? extends ObjectType>> retval = new ArrayList<ObjectDelta<? extends ObjectType>>();
        retval.add(ObjectDelta.createAddDelta(taskToBeAdded.asPrismObject()));
        return retval;
    }

    private TaskType createTask(TaskAddDto dto) throws SchemaException {
        TaskType task = new TaskType();
        MidPointPrincipal owner = SecurityUtils.getPrincipalUser();

        ObjectReferenceType ownerRef = new ObjectReferenceType();
        ownerRef.setOid(owner.getOid());
        ownerRef.setType(owner.getUser().COMPLEX_TYPE);
        task.setOwnerRef(ownerRef);

        task.setCategory(dto.getCategory());
        String handlerUri = getTaskManager().getHandlerUriForCategory(dto.getCategory());
        if (handlerUri == null) {
            throw new SystemException("Cannot determine task handler URI for category " + dto.getCategory());
        }
        task.setHandlerUri(handlerUri);

        ObjectReferenceType objectRef;
        if (dto.getResource() != null) {
            objectRef = new ObjectReferenceType();
            objectRef.setOid(dto.getResource().getOid());
            objectRef.setType(ResourceType.COMPLEX_TYPE);
            task.setObjectRef(objectRef);
        }

        task.setName(WebMiscUtil.createPolyFromOrigString(dto.getName()));

        task.setRecurrence(dto.getReccuring() ? TaskRecurrenceType.RECURRING : TaskRecurrenceType.SINGLE);
        task.setBinding(dto.getBound() ? TaskBindingType.TIGHT : TaskBindingType.LOOSE);

        ScheduleType schedule = new ScheduleType();
        schedule.setInterval(dto.getInterval());
        schedule.setCronLikePattern(dto.getCron());
        schedule.setEarliestStartTime(MiscUtil.asXMLGregorianCalendar(dto.getNotStartBefore()));
        schedule.setLatestStartTime(MiscUtil.asXMLGregorianCalendar(dto.getNotStartAfter()));
        schedule.setMisfireAction(dto.getMisfireAction());
        task.setSchedule(schedule);

        if (dto.getSuspendedState()) {
            task.setExecutionStatus(TaskExecutionStatusType.SUSPENDED);
        } else {
            task.setExecutionStatus(TaskExecutionStatusType.RUNNABLE);
        }

        if (dto.getThreadStop() != null) {
            task.setThreadStopAction(dto.getThreadStop());
        } else {
            // fill-in default
            if (dto.getRunUntilNodeDown() == true) {
                task.setThreadStopAction(ThreadStopActionType.CLOSE);
            } else {
                task.setThreadStopAction(ThreadStopActionType.RESTART);
            }
        }

        if (dto.isDryRun()) {
            PrismObject<TaskType> prismTask = task.asPrismObject();
            ItemPath path = new ItemPath(TaskType.F_EXTENSION,SchemaConstants.MODEL_EXTENSION_DRY_RUN);
            PrismProperty dryRun = prismTask.findOrCreateProperty(path);

            SchemaRegistry registry = getPrismContext().getSchemaRegistry();
            PrismPropertyDefinition def = registry.findPropertyDefinitionByElementName(SchemaConstants.MODEL_EXTENSION_DRY_RUN);
            dryRun.setDefinition(def);
            dryRun.setRealValue(true);
        }

        if(dto.getKind() != null){
            PrismObject<TaskType> prismTask = task.asPrismObject();
            ItemPath path = new ItemPath(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_KIND);
            PrismProperty kind = prismTask.findOrCreateProperty(path);

            SchemaRegistry registry = getPrismContext().getSchemaRegistry();
            PrismPropertyDefinition def = registry.findPropertyDefinitionByElementName(SchemaConstants.MODEL_EXTENSION_KIND);
            kind.setDefinition(def);
            kind.setRealValue(dto.getKind());
        }

        if(dto.getIntent() != null && StringUtils.isNotEmpty(dto.getIntent())){
            PrismObject<TaskType> prismTask = task.asPrismObject();
            ItemPath path = new ItemPath(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_INTENT);
            PrismProperty intent = prismTask.findOrCreateProperty(path);

            SchemaRegistry registry = getPrismContext().getSchemaRegistry();
            PrismPropertyDefinition def = registry.findPropertyDefinitionByElementName(SchemaConstants.MODEL_EXTENSION_INTENT);
            intent.setDefinition(def);
            intent.setRealValue(dto.getIntent());
        }

        return task;
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
