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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.datetime.markup.html.form.DateTextField;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteSettings;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.extensions.yui.calendar.DateTimeField;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.string.Strings;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.Definition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
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
import com.evolveum.midpoint.web.component.input.ChoiceableChoiceRenderer;
import com.evolveum.midpoint.web.component.input.QNameChoiceRenderer;
import com.evolveum.midpoint.web.component.input.StringChoiceRenderer;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.server.dto.ScheduleValidator;
import com.evolveum.midpoint.web.page.admin.server.dto.StartEndDateValidator;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskAddDto;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskAddResourcesDto;
import com.evolveum.midpoint.web.page.admin.server.dto.TsaValidator;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MisfireActionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScheduleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskBindingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskRecurrenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ThreadStopActionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author lazyman
 * @author mserbak
 */
@PageDescriptor(url = "/admin/tasks/addTask", action = {
        @AuthorizationAction(actionUri = PageAdminTasks.AUTHORIZATION_TASKS_ALL,
                label = PageAdminTasks.AUTH_TASKS_ALL_LABEL,
                description = PageAdminTasks.AUTH_TASKS_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_TASK_ADD_URL,
                label = "PageTaskAdd.auth.taskAdd.label",
                description = "PageTaskAdd.auth.taskAdd.description")})
public class PageTaskAdd extends PageAdminTasks {

    private static final long serialVersionUID = 2317887071933841581L;

    private static final String ID_DRY_RUN = "dryRun";    
    private static final String ID_FOCUS_TYPE = "focusType";
    private static final String ID_KIND = "kind";
    private static final String ID_INTENT = "intent";
    private static final String ID_OBJECT_CLASS = "objectClass";
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
    private static final String OPERATION_LOAD_RESOURCE = DOT_CLASS + "loadResource";
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
    
    public PageTaskAdd(final TaskType taskType) {
        model = new LoadableModel<TaskAddDto>(false) {

            @Override
            protected TaskAddDto load() {
                return loadTask(taskType);
            }
        };
        initLayout();
    }

    private TaskAddDto loadTask() {
        return new TaskAddDto();
    }
    
    private TaskAddDto loadTask(TaskType taskType) {
    	TaskAddDto taskAdd = new TaskAddDto();
    	taskAdd.setCategory(taskType.getCategory());
    	PrismProperty<ShadowKindType> pKind;
		try {
			pKind = taskType.asPrismObject().findOrCreateProperty(new ItemPath(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_KIND));
			taskAdd.setKind(pKind.getRealValue());
		} catch (SchemaException e) {
			warn("Could not set kind for new task : " + e.getMessage());
		}
    	
    	PrismProperty<String> pIntent;
		try {
			pIntent = taskType.asPrismObject().findOrCreateProperty(new ItemPath(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_INTENT));
			taskAdd.setIntent(pIntent.getRealValue());
		} catch (SchemaException e) {
			warn("Could not set intent for new task : " + e.getMessage());
		}
    	
    	PrismProperty<QName> pObjectClass;
		try {
			pObjectClass = taskType.asPrismObject().findOrCreateProperty(new ItemPath(TaskType.F_EXTENSION, SchemaConstants.OBJECTCLASS_PROPERTY_NAME));
			QName objectClass = pObjectClass.getRealValue();
			if (objectClass != null){
	    		taskAdd.setObjectClass(objectClass.getLocalPart());
	    	}
		} catch (SchemaException e) {
			warn("Could not set obejctClass for new task : " + e.getMessage());
		}
    	
    	
    	ObjectReferenceType ref = taskType.getObjectRef();
    	if (ref != null) {
    	TaskAddResourcesDto resource= new TaskAddResourcesDto(ref.getOid(), WebComponentUtil.getName(ref));
    	taskAdd.setResource(resource);
    	}
        return taskAdd;
    }

    private void initLayout() {
        Form mainForm = new Form(ID_FORM_MAIN);
        add(mainForm);

        final DropDownChoice resource = new DropDownChoice<>(ID_RESOURCE,
                new PropertyModel<TaskAddResourcesDto>(model, TaskAddDto.F_RESOURCE),
                new AbstractReadOnlyModel<List<TaskAddResourcesDto>>() {

                    @Override
                    public List<TaskAddResourcesDto> getObject() {
                        return createResourceList();
                    }
                }, new ChoiceableChoiceRenderer<TaskAddResourcesDto>());
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
        resource.add(new AjaxFormComponentUpdatingBehavior("change") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                PageTaskAdd.this.loadResource();
                target.add(get(ID_FORM_MAIN + ":" + ID_OBJECT_CLASS));
            }
        });
        resource.setOutputMarkupId(true);
        mainForm.add(resource);

        final DropDownChoice focusType = new DropDownChoice<>(ID_FOCUS_TYPE,
                new PropertyModel<QName>(model, TaskAddDto.F_FOCUS_TYPE),
                new AbstractReadOnlyModel<List<QName>>() {

                    @Override
                    public List<QName> getObject() {
                        return createFocusTypeList();
                    }
                }, new QNameChoiceRenderer());
        focusType.setOutputMarkupId(true);
        focusType.add(new VisibleEnableBehaviour(){

            @Override
            public boolean isEnabled() {
                TaskAddDto dto = model.getObject();
                return TaskCategory.RECOMPUTATION.equals(dto.getCategory());
            }
        });
        mainForm.add(focusType);

        
        final DropDownChoice kind = new DropDownChoice<>(ID_KIND,
                new PropertyModel<ShadowKindType>(model, TaskAddDto.F_KIND),
                WebComponentUtil.createReadonlyModelFromEnum(ShadowKindType.class), new EnumChoiceRenderer<ShadowKindType>());
        kind.setOutputMarkupId(true);
        kind.add(new VisibleEnableBehaviour(){

            @Override
            public boolean isEnabled() {
                TaskAddDto dto = model.getObject();
                boolean sync = TaskCategory.LIVE_SYNCHRONIZATION.equals(dto.getCategory());
                boolean recon = TaskCategory.RECONCILIATION.equals(dto.getCategory());
                boolean importAccounts = TaskCategory.IMPORTING_ACCOUNTS.equals(dto.getCategory());
                return sync || recon || importAccounts;
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
                boolean sync = TaskCategory.LIVE_SYNCHRONIZATION.equals(dto.getCategory());
                boolean recon = TaskCategory.RECONCILIATION.equals(dto.getCategory());
                boolean importAccounts = TaskCategory.IMPORTING_ACCOUNTS.equals(dto.getCategory());
                return sync || recon || importAccounts;
            }
        });

        if (model.getObject() != null
                && model.getObject().getResource() != null
                && model.getObject().getResource().getOid() != null){
            loadResource();
        }
        AutoCompleteSettings autoCompleteSettings = new AutoCompleteSettings();
        autoCompleteSettings.setShowListOnEmptyInput(true);
        autoCompleteSettings.setMaxHeightInPx(200);
        final AutoCompleteTextField<String> objectClass = new AutoCompleteTextField<String>(ID_OBJECT_CLASS,
                new PropertyModel<String>(model, TaskAddDto.F_OBJECT_CLASS), autoCompleteSettings) {

            @Override
            protected Iterator<String> getChoices(String input) {
                return prepareObjectClassChoiceList(input);
            }
        };
        objectClass.add(new VisibleEnableBehaviour(){

            @Override
            public boolean isEnabled() {
                TaskAddDto dto = model.getObject();
                boolean sync = TaskCategory.LIVE_SYNCHRONIZATION.equals(dto.getCategory());
                boolean recon = TaskCategory.RECONCILIATION.equals(dto.getCategory());
                boolean importAccounts = TaskCategory.IMPORTING_ACCOUNTS.equals(dto.getCategory());
                return sync || recon || importAccounts;
            }
        });
        mainForm.add(objectClass);

        DropDownChoice type = new DropDownChoice<>(ID_CATEGORY, new PropertyModel<String>(model, TaskAddDto.F_CATEGORY),
                new AbstractReadOnlyModel<List<String>>() {

                    @Override
                    public List<String> getObject() {
                        return WebComponentUtil.createTaskCategoryList();
                    }
                }, new StringChoiceRenderer("pageTask.category."));
        type.add(new AjaxFormComponentUpdatingBehavior("change") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(resource);
                target.add(intent);
                target.add(kind);
                target.add(objectClass);
                target.add(focusType);
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

    private Iterator<String> prepareObjectClassChoiceList(String input){
        List<String> choices = new ArrayList<>();

        if(model.getObject().getResource() == null){
            return choices.iterator();
        }

        if(Strings.isEmpty(input)){
            for(QName q: model.getObject().getObjectClassList()){
                choices.add(q.getLocalPart());
                Collections.sort(choices);
            }
        } else {
            for(QName q: model.getObject().getObjectClassList()){
                if(q.getLocalPart().startsWith(input)){
                    choices.add(q.getLocalPart());
                }
                Collections.sort(choices);
            }
        }

        return choices.iterator();
    }


    private void loadResource(){
        Task task = createSimpleTask(OPERATION_LOAD_RESOURCE);
        OperationResult result = task.getResult();
        List<QName> objectClassList = new ArrayList<>();

        TaskAddResourcesDto resourcesDto = model.getObject().getResource();

        if(resourcesDto != null){
            PrismObject<ResourceType> resource = WebModelServiceUtils.loadObject(ResourceType.class,
                    resourcesDto.getOid(), PageTaskAdd.this, task, result);

            try {
                ResourceSchema schema = RefinedResourceSchemaImpl.getResourceSchema(resource, getPrismContext());
                model.getObject().setObjectClassList(schema.getObjectClassList());
            } catch (Exception e){
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load object class list from resource.", e);
                error("Couldn't load object class list from resource.");
            }

        }
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

        DropDownChoice threadStop = new DropDownChoice<>(ID_THREAD_STOP, new Model<ThreadStopActionType>() {

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
        }, WebComponentUtil.createReadonlyModelFromEnum(ThreadStopActionType.class),
                new EnumChoiceRenderer<ThreadStopActionType>(PageTaskAdd.this));
        mainForm.add(threadStop);

        mainForm.add(new TsaValidator(runUntilNodeDown, threadStop));

        DropDownChoice misfire = new DropDownChoice<>(ID_MISFIRE_ACTION, new PropertyModel<MisfireActionType>(
                model, TaskAddDto.F_MISFIRE_ACTION), WebComponentUtil.createReadonlyModelFromEnum(MisfireActionType.class),
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
        mainForm.setDefaultButton(saveButton);
        mainForm.add(saveButton);

        AjaxButton backButton = new AjaxButton(ID_BUTTON_BACK,
                createStringResource("PageBase.button.back")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                redirectBack();
            }
        };
        mainForm.add(backButton);
    }

   
    private List<ShadowKindType> createShadowKindTypeList(){
        List<ShadowKindType> kindList = new ArrayList<>();

        kindList.add(ShadowKindType.ACCOUNT);
        kindList.add(ShadowKindType.ENTITLEMENT);
        kindList.add(ShadowKindType.GENERIC);

        return kindList;
    }
    
    private List<QName> createFocusTypeList(){
        List<QName> focusTypeList = new ArrayList<>();

        focusTypeList.add(UserType.COMPLEX_TYPE);
        focusTypeList.add(RoleType.COMPLEX_TYPE);
        focusTypeList.add(OrgType.COMPLEX_TYPE);
        focusTypeList.add(FocusType.COMPLEX_TYPE);

        return focusTypeList;
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
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get resource list", ex);
        }

        // todo show result somehow...
        // if (!result.isSuccess()) {
        // showResult(result);
        // }
        if (resources != null) {
            ResourceType item = null;
            for (PrismObject<ResourceType> resource : resources) {
                item = resource.asObjectable();
                resourceList.add(new TaskAddResourcesDto(item.getOid(), WebComponentUtil.getOrigStringFromPoly(item.getName())));
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
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't add new task", ex);
        }
        showResult(result);
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

        task.setName(WebComponentUtil.createPolyFromOrigString(dto.getName()));

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

        if (dto.getFocusType() != null){

        	PrismObject<TaskType> prismTask = task.asPrismObject();
        	
        	ItemPath path = new ItemPath(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_OBJECT_TYPE);
			PrismProperty focusType = prismTask.findOrCreateProperty(path);
			focusType.setRealValue(dto.getFocusType());
            
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

        if(dto.getObjectClass() != null && StringUtils.isNotEmpty(dto.getObjectClass())){
            PrismObject<TaskType> prismTask = task.asPrismObject();
            ItemPath path = new ItemPath(TaskType.F_EXTENSION, SchemaConstants.OBJECTCLASS_PROPERTY_NAME);
            PrismProperty objectClassProperty = prismTask.findOrCreateProperty(path);

            QName objectClass = null;
            for(QName q: model.getObject().getObjectClassList()){
                if(q.getLocalPart().equals(dto.getObjectClass())){
                    objectClass = q;
                }
            }

            SchemaRegistry registry = getPrismContext().getSchemaRegistry();
            PrismPropertyDefinition def = registry.findPropertyDefinitionByElementName(SchemaConstants.OBJECTCLASS_PROPERTY_NAME);
            objectClassProperty.setDefinition(def);
            objectClassProperty.setRealValue(objectClass);
        }

        return task;
    }

    private static class EmptyOnBlurAjaxFormUpdatingBehaviour extends AjaxFormComponentUpdatingBehavior {

        public EmptyOnBlurAjaxFormUpdatingBehaviour() {
            super("blur");
        }

        @Override
        protected void onUpdate(AjaxRequestTarget target) {
        }
    }
}
