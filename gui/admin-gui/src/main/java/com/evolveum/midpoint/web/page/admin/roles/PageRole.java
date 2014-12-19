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
package com.evolveum.midpoint.web.page.admin.roles;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.assignment.*;
import com.evolveum.midpoint.web.component.form.*;
import com.evolveum.midpoint.web.component.prism.*;
import com.evolveum.midpoint.web.component.progress.ProgressReporter;
import com.evolveum.midpoint.web.component.progress.ProgressReportingAwarePage;
import com.evolveum.midpoint.web.component.util.*;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsDto;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsPanel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.web.util.WebModelUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.string.StringValue;

import java.util.Collection;
import java.util.List;

/**
 *  @author shood
 * */
@PageDescriptor(url = "/admin/role", encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(actionUri = PageAdminRoles.AUTH_ROLE_ALL,
                label = PageAdminRoles.AUTH_ROLE_ALL_LABEL,
                description = PageAdminRoles.AUTH_ROLE_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.NS_AUTHORIZATION + "#role",
                label = "PageRole.auth.role.label",
                description = "PageRole.auth.role.description")})
public class PageRole extends PageAdminRoles implements ProgressReportingAwarePage {

    private static final Trace LOGGER = TraceManager.getTrace(PageRole.class);

    private static final String DOT_CLASS = PageRole.class.getName() + ".";
    private static final String OPERATION_LOAD_ROLE = DOT_CLASS + "loadRole";
    private static final String OPERATION_CREATE_ROLE_WRAPPER = DOT_CLASS + "createRoleWrapper";
    private static final String OPERATION_LOAD_ASSIGNMENT = DOT_CLASS + "loadAssignment";
    private static final String OPERATION_SAVE_ROLE = DOT_CLASS + "saveRole";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_BUTTON_BACK = "backButton";
    private static final String ID_BUTTON_SAVE = "saveButton";
    private static final String ID_BUTTON_ABORT = "abortButton";
    private static final String ID_NAME = "name";
    private static final String ID_DISPLAY_NAME = "displayName";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_ROLE_TYPE = "roleType";
    private static final String ID_REQUESTABLE = "requestable";
    private static final String ID_IDENTIFIER = "identifier";
    private static final String ID_DATE_FROM = "dateFrom";
    private static final String ID_DATE_TO = "dateTo";
    private static final String ID_ADMIN_STATUS = "adminStatus";
    private static final String ID_EXTENSION = "extension";
    private static final String ID_EXTENSION_LABEL = "extensionLabel";
    private static final String ID_EXTENSION_PROPERTY = "property";
    private static final String ID_EXECUTE_OPTIONS = "executeOptions";

    private static final String ID_INDUCEMENTS = "inducementsPanel";
    private static final String ID_ASSIGNMENTS = "assignmentsPanel";

    private static final String ID_LABEL_SIZE = "col-md-4";
    private static final String ID_INPUT_SIZE = "col-md-6";

    private IModel<PrismObject<RoleType>> model;
    private IModel<ContainerWrapper> extensionModel;
    private ObjectWrapper roleWrapper;

    private ProgressReporter progressReporter;
    private ObjectDelta delta;

    private LoadableModel<ExecuteChangeOptionsDto> executeOptionsModel
            = new LoadableModel<ExecuteChangeOptionsDto>(false) {

        @Override
        protected ExecuteChangeOptionsDto load() {
            return new ExecuteChangeOptionsDto();
        }
    };

    public PageRole(){

        model = new LoadableModel<PrismObject<RoleType>>(false) {
            @Override
            protected PrismObject<RoleType> load() {
                return loadRole();
            }
        };

        extensionModel = new LoadableModel<ContainerWrapper>(false) {

            @Override
            protected ContainerWrapper load() {
                return loadRoleWrapper();
            }
        };

        initLayout();
    }

    @Override
    protected IModel<String> createPageTitleModel(){
        return new LoadableModel<String>() {

            @Override
            protected String load() {
                if(!isEditing()){
                    return createStringResource("PageRoleEditor.title.newRole").getObject();
                }

                return createStringResource("PageRoleEditor.title.editingRole").getObject();
            }
        };
    }

    @Override
    protected IModel<String> createPageSubTitleModel(){
        return new LoadableModel<String>() {

            @Override
            protected String load() {
                if(!isEditing()){
                    return createStringResource("PageRoleEditor.subtitle.newRole").getObject();
                }

                String roleName = WebMiscUtil.getName(model.getObject());
                return createStringResource("PageRoleEditor.subtitle.editingRole", roleName).getString();
            }
        };
    }

    private PrismObject<RoleType> loadRole(){
        OperationResult result = new OperationResult(OPERATION_LOAD_ROLE);

        PrismObject<RoleType> role = null;
        try {
            if (!isEditing()) {
                RoleType r = new RoleType();
                ActivationType defaultActivation = new ActivationType();
                defaultActivation.setAdministrativeStatus(ActivationStatusType.ENABLED);
                r.setActivation(defaultActivation);
                getMidpointApplication().getPrismContext().adopt(r);
                role = r.asPrismObject();
            } else {
                StringValue oid = getPageParameters().get(OnePageParameterEncoder.PARAMETER);
                role = getModelService().getObject(RoleType.class, oid.toString(), null,
                        createSimpleTask(OPERATION_LOAD_ROLE), result);
            }
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't load role", ex);
            result.recordFatalError("Couldn't load role.", ex);
        } finally {
            result.computeStatus();
        }

        if (WebMiscUtil.showResultInPage(result)) {
            showResult(result);
        }

        if (role == null) {
            throw new RestartResponseException(PageRole.class);
        }

        return role;
    }

    private ContainerWrapper loadRoleWrapper(){
        OperationResult result = new OperationResult(OPERATION_CREATE_ROLE_WRAPPER);
        ContainerStatus status = isEditing() ? ContainerStatus.MODIFYING : ContainerStatus.ADDING;
        ObjectWrapper wrapper = null;
        ContainerWrapper extensionContainer = null;
        PrismObject<RoleType> role = model.getObject();

        try{
            wrapper = ObjectWrapperUtil.createObjectWrapper("PageRoleEditor.extension", null, role, status, this);
        } catch (Exception e){
            result.recordFatalError("Couldn't create role wrapper.", e);
            LoggingUtils.logException(LOGGER, "Couldn't create role wrapper", e);
            wrapper = new ObjectWrapper("PageRoleEditor.extension", null, role, null, status, this);
        }

        if(wrapper.getResult() != null && !WebMiscUtil.isSuccessOrHandledError(wrapper.getResult())){
            showResultInSession(wrapper.getResult());
        }

        wrapper.setShowEmpty(true);
        roleWrapper = wrapper;

        List<ContainerWrapper> list = wrapper.getContainers();
        for(ContainerWrapper cont: list){
            if("extension".equals(cont.getItem().getDefinition().getName().getLocalPart())){
                extensionContainer = cont;
            }
        }

        return extensionContainer;
    }

    private boolean isEditing(){
        StringValue oid = getPageParameters().get(OnePageParameterEncoder.PARAMETER);
        return oid != null && StringUtils.isNotEmpty(oid.toString());
    }

    private PrismObject getReference(ObjectReferenceType ref, OperationResult result) {
        OperationResult subResult = result.createSubresult(OPERATION_LOAD_ASSIGNMENT);
        subResult.addParam("targetRef", ref.getOid());
        PrismObject target = null;
        try {
            Task task = createSimpleTask(OPERATION_LOAD_ASSIGNMENT);
            Class type = ObjectType.class;
            if (ref.getType() != null) {
                type = getPrismContext().getSchemaRegistry().determineCompileTimeClass(ref.getType());
            }
            target = getModelService().getObject(type, ref.getOid(), null, task, subResult);
            subResult.recordSuccess();
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't get assignment target ref", ex);
            subResult.recordFatalError("Couldn't get assignment target ref.", ex);
        }

        return target;
    }

    private void initLayout(){
        final Form form = new Form(ID_MAIN_FORM);
        add(form);

        progressReporter = ProgressReporter.create(this, form, "progressPanel");

        TextFormGroup name = new TextFormGroup(ID_NAME, new PrismPropertyModel(model, RoleType.F_NAME),
                createStringResource("PageRoleEditor.label.name"), ID_LABEL_SIZE, ID_INPUT_SIZE, true);
        form.add(name);

        TextFormGroup displayName = new TextFormGroup(ID_DISPLAY_NAME, new PrismPropertyModel(model,
                RoleType.F_DISPLAY_NAME), createStringResource("PageRoleEditor.label.displayName"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        form.add(displayName);

        TextAreaFormGroup description = new TextAreaFormGroup(ID_DESCRIPTION, new PrismPropertyModel(model, RoleType.F_DESCRIPTION),
                createStringResource("PageRoleEditor.label.description"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        form.add(description);

        TextFormGroup roleType = new TextFormGroup(ID_ROLE_TYPE, new PrismPropertyModel(model, RoleType.F_ROLE_TYPE),
                createStringResource("PageRoleEditor.label.type"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        form.add(roleType);

        CheckFormGroup requestable = new CheckFormGroup(ID_REQUESTABLE, new PrismPropertyModel(model, RoleType.F_REQUESTABLE),
                createStringResource("PageRoleEditor.label.requestable"), ID_LABEL_SIZE, ID_INPUT_SIZE);
        form.add(requestable);

        TextFormGroup identifier = new TextFormGroup(ID_IDENTIFIER, new PrismPropertyModel(model, RoleType.F_IDENTIFIER),
                createStringResource("PageRoleEditor.label.identifier"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        form.add(identifier);

        IModel choices = WebMiscUtil.createReadonlyModelFromEnum(ActivationStatusType.class);
        IChoiceRenderer renderer = new EnumChoiceRenderer();
        DropDownFormGroup adminStatus = new DropDownFormGroup(ID_ADMIN_STATUS, new PrismPropertyModel(model,
                new ItemPath(RoleType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS)),
                choices, renderer, createStringResource("ActivationType.administrativeStatus"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        form.add(adminStatus);

        DateFormGroup validFrom = new DateFormGroup(ID_DATE_FROM, new PrismPropertyModel(model,
                new ItemPath(RoleType.F_ACTIVATION, ActivationType.F_VALID_FROM)),
                createStringResource("ActivationType.validFrom"),ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        form.add(validFrom);

        DateFormGroup validTo = new DateFormGroup(ID_DATE_TO, new PrismPropertyModel(model,
                new ItemPath(RoleType.F_ACTIVATION, ActivationType.F_VALID_TO)),
                createStringResource("ActivationType.validTo"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        form.add(validTo);

        AssignmentTablePanel assignments = new AssignmentTablePanel(ID_ASSIGNMENTS, new Model<AssignmentTableDto>(),
                createStringResource("PageRoleEditor.title.assignments")){

            @Override
            public List<AssignmentType> getAssignmentTypeList(){
                return model.getObject().asObjectable().getAssignment();
            }

            @Override
            public String getExcludeOid(){
                return model.getObject().asObjectable().getOid();
            }
        };
        form.add(assignments);

        AssignmentTablePanel inducements = new AssignmentTablePanel(ID_INDUCEMENTS, new Model<AssignmentTableDto>(),
                createStringResource("PageRoleEditor.title.inducements")){

            @Override
            public List<AssignmentType> getAssignmentTypeList(){
                return model.getObject().asObjectable().getInducement();
            }

            @Override
            public String getExcludeOid(){
                return model.getObject().asObjectable().getOid();
            }
        };
        form.add(inducements);

        Label extensionLabel = new Label(ID_EXTENSION_LABEL, createStringResource("PageRoleEditor.subtitle.extension"));
        extensionLabel.add(new VisibleEnableBehaviour(){

            @Override
            public boolean isVisible() {
                if(extensionModel == null || extensionModel.getObject() == null
                        || extensionModel.getObject().getProperties().isEmpty()){
                    return false;
                } else {
                    return true;
                }
            }
        });
        form.add(extensionLabel);

        ListView<PropertyWrapper> extensionProperties = new ListView<PropertyWrapper>(ID_EXTENSION,
                new PropertyModel(extensionModel, "properties")) {

            @Override
            protected void populateItem(ListItem<PropertyWrapper> item) {
                PrismPropertyPanel propertyPanel = new PrismPropertyPanel(ID_EXTENSION_PROPERTY, item.getModel(), form);
                propertyPanel.get("labelContainer:label").add(new AttributeAppender("style", "font-weight:bold;"));
                propertyPanel.get("labelContainer").add(new AttributeModifier("class", ID_LABEL_SIZE + " control-label"));
                item.add(propertyPanel);
                item.add(AttributeModifier.append("class", createStyleClassModel(item.getModel())));
            }
        };
        extensionProperties.setReuseItems(true);
        form.add(extensionProperties);

        initButtons(form);
    }

    private IModel<String> createStyleClassModel(final IModel<PropertyWrapper> wrapper) {
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                PropertyWrapper property = wrapper.getObject();
                return property.isVisible() ? "visible" : null;
            }
        };
    }

    private void initButtons(Form form){
        AjaxSubmitButton save = new AjaxSubmitButton(ID_BUTTON_SAVE, createStringResource("PageBase.button.save")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form){
                progressReporter.onSaveSubmit();
                savePerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form){
                target.add(form);
                target.add(getFeedbackPanel());
            }
        };
        progressReporter.registerSaveButton(save);
        form.add(save);

        AjaxSubmitButton abortButton = new AjaxSubmitButton(ID_BUTTON_ABORT,
                createStringResource("PageBase.button.abort")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                progressReporter.onAbortSubmit(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        progressReporter.registerAbortButton(abortButton);
        form.add(abortButton);

        AjaxSubmitButton back = new AjaxSubmitButton(ID_BUTTON_BACK, createStringResource("PageBase.button.back")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form){
                backPerformed(target);
            }
        };
        form.add(back);

        form.add(new ExecuteChangeOptionsPanel(ID_EXECUTE_OPTIONS, executeOptionsModel, true));
    }

    private void savePerformed(AjaxRequestTarget target){
        OperationResult result = new OperationResult(OPERATION_SAVE_ROLE);
        try {
            WebMiscUtil.revive(model, getPrismContext());

            ModelService modelService = getModelService();

            PrismObject<RoleType> newRole = model.getObject();

            delta = null;
            if (!isEditing()) {
                delta = saveExtension(result);

                //handle assignments
                PrismObjectDefinition orgDef = newRole.getDefinition();
                PrismContainerDefinition assignmentDef = orgDef.findContainerDefinition(RoleType.F_ASSIGNMENT);
                AssignmentTablePanel assignmentPanel = (AssignmentTablePanel)get(createComponentPath(ID_MAIN_FORM, ID_ASSIGNMENTS));
                assignmentPanel.handleAssignmentsWhenAdd(newRole, assignmentDef, newRole.asObjectable().getAssignment());

                //handle inducements
                PrismContainerDefinition inducementDef = orgDef.findContainerDefinition(RoleType.F_INDUCEMENT);
                AssignmentTablePanel inducementPanel = (AssignmentTablePanel)get(createComponentPath(ID_MAIN_FORM, ID_INDUCEMENTS));
                inducementPanel.handleAssignmentsWhenAdd(newRole, inducementDef, newRole.asObjectable().getInducement());

            } else {
                PrismObject<RoleType> oldRole = WebModelUtils.loadObject(RoleType.class, newRole.getOid(), result, this);
                if (oldRole != null) {
                    delta = oldRole.diff(newRole);

                    //handle assignments
                    SchemaRegistry registry = getPrismContext().getSchemaRegistry();
                    PrismObjectDefinition objectDefinition = registry.findObjectDefinitionByCompileTimeClass(RoleType.class);
                    PrismContainerDefinition assignmentDef = objectDefinition.findContainerDefinition(RoleType.F_ASSIGNMENT);
                    AssignmentTablePanel assignmentPanel = (AssignmentTablePanel)get(createComponentPath(ID_MAIN_FORM, ID_ASSIGNMENTS));
                    assignmentPanel.handleAssignmentDeltas(delta, assignmentDef, RoleType.F_ASSIGNMENT);

                    //handle inducements
                    PrismContainerDefinition inducementDef = objectDefinition.findContainerDefinition(RoleType.F_INDUCEMENT);
                    AssignmentTablePanel inducementPanel = (AssignmentTablePanel)get(createComponentPath(ID_MAIN_FORM, ID_INDUCEMENTS));
                    inducementPanel.handleAssignmentDeltas(delta, inducementDef, RoleType.F_INDUCEMENT);
                }
            }

            ObjectDelta extensionDelta = saveExtension(result);

            if (delta != null) {
                if(extensionDelta != null){
                    for(ItemDelta itemDelta: (List<ItemDelta>)extensionDelta.getModifications()){
                        delta.addModification(itemDelta);
                    }
                }

                ExecuteChangeOptionsDto executeOptions = executeOptionsModel.getObject();
                ModelExecuteOptions options = executeOptions.createOptions();

                Collection<ObjectDelta<? extends ObjectType>> deltas = WebMiscUtil.createDeltaCollection(delta);
                progressReporter.executeChanges(deltas, options, createSimpleTask(OPERATION_SAVE_ROLE), result, target);
            }
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't save role", ex);
            result.recordFatalError("Couldn't save role.", ex);
        } finally {
            result.computeStatusIfUnknown();
        }

        if (!result.isInProgress()) {
            finishProcessing(target, result);
        }
    }

    public void finishProcessing(AjaxRequestTarget target, OperationResult result) {
        if (!executeOptionsModel.getObject().isKeepDisplayingResults() &&
                progressReporter.isAllSuccess() &&
                WebMiscUtil.isSuccessOrHandledErrorOrInProgress(result)) {
            showResultInSession(result);
            setResponsePage(new PageRoles(false));
        } else {
            showResult(result);
            target.add(getFeedbackPanel());
        }
    }

    private ObjectDelta saveExtension(OperationResult result){
        ObjectDelta delta = null;

        try {
            WebMiscUtil.revive(extensionModel, getPrismContext());
            WebMiscUtil.revive(model, getPrismContext());

            delta = roleWrapper.getObjectDelta();
            if(roleWrapper.getOldDelta() != null){
                delta = ObjectDelta.summarize(roleWrapper.getOldDelta(), delta);
            }

            if(LOGGER.isTraceEnabled()){
                LOGGER.trace("Role delta computed from extension:\n{}", new Object[]{delta.debugDump(3)});
            }
        } catch (Exception e){
            result.recordFatalError(getString("PageRoleEditor.message.cantCreateExtensionDelta"), e);
            LoggingUtils.logException(LOGGER, "Can't create delta for role extension.", e);
            showResult(result);
        }

        return delta;
    }

    private void backPerformed(AjaxRequestTarget target){
        setResponsePage(new PageRoles(false));
    }
}
