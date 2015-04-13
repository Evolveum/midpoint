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
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.assignment.*;
import com.evolveum.midpoint.web.component.form.*;
import com.evolveum.midpoint.web.component.form.multivalue.GenericMultiValueLabelEditPanel;
import com.evolveum.midpoint.web.component.form.multivalue.MultiValueChoosePanel;
import com.evolveum.midpoint.web.component.prism.*;
import com.evolveum.midpoint.web.component.progress.ProgressReporter;
import com.evolveum.midpoint.web.component.progress.ProgressReportingAwarePage;
import com.evolveum.midpoint.web.component.util.*;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.configuration.component.ChooseTypeDialog;
import com.evolveum.midpoint.web.page.admin.roles.component.MultiplicityPolicyDialog;
import com.evolveum.midpoint.web.page.admin.roles.component.UserOrgReferenceChoosePanel;
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
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.string.StringValue;

import java.util.ArrayList;
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
    private static final String OPERATION_SAVE_ROLE = DOT_CLASS + "saveRole";
    private static final String OPERATION_LOAD_OWNER = DOT_CLASS + "getOwner";
    private static final String OPERATION_LOAD_APPROVERS = DOT_CLASS + "loadApprovers";

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
    private static final String ID_OWNER_WRAPPER = "ownerRefWrapper";
    private static final String ID_OWNER_EDIT = "ownerRefEdit";
    private static final String ID_OWNER_LABEL = "ownerRefLabel";
    private static final String ID_APPROVER_REF = "approverRef";
    private static final String ID_RISK_LEVEL = "riskLevel";
    private static final String ID_MIN_ASSIGNMENTS = "minAssignmentsConfig";
    private static final String ID_MAX_ASSIGNMENTS = "maxAssignmentsConfig";

    private static final String ID_INDUCEMENTS = "inducementsPanel";
    private static final String ID_ASSIGNMENTS = "assignmentsPanel";

    private static final String ID_MODAL_OWNER_CHOOSER = "ownerChooser";

    private static final String ID_LABEL_SIZE = "col-md-4";
    private static final String ID_INPUT_SIZE = "col-md-6";

    private IModel<PrismObject<RoleType>> model;
    private IModel<List<ObjectType>> approversModel;
    private IModel<ContainerWrapper> extensionModel;
    private IModel<List<MultiplicityPolicyConstraintType>> minAssignmentModel;
    private IModel<List<MultiplicityPolicyConstraintType>> maxAssignmentsModel;
    private ObjectWrapper roleWrapper;

    private ProgressReporter progressReporter;

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

        approversModel = new LoadableModel<List<ObjectType>>(false) {

            @Override
            protected List<ObjectType> load() {
                return loadApprovers();
            }
        };

        minAssignmentModel = new LoadableModel<List<MultiplicityPolicyConstraintType>>(false) {

            @Override
            protected List<MultiplicityPolicyConstraintType> load() {
                RoleType role = model.getObject().asObjectable();

                if(role.getPolicyConstraints() == null){
                    role.setPolicyConstraints(new PolicyConstraintsType());
                }

                return role.getPolicyConstraints().getMinAssignees();
            }
        };

        maxAssignmentsModel = new LoadableModel<List<MultiplicityPolicyConstraintType>>(false) {

            @Override
            protected List<MultiplicityPolicyConstraintType> load() {
                RoleType role = model.getObject().asObjectable();

                if(role.getPolicyConstraints() == null){
                    role.setPolicyConstraints(new PolicyConstraintsType());
                }

                return model.getObject().asObjectable().getPolicyConstraints().getMaxAssignees();
            }
        };

        initLayout();
    }

    @Override
    protected IModel<String> createPageTitleModel(){
        return new LoadableModel<String>() {

            @Override
            protected String load() {
                if(!isEditingRole()){
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
                if(!isEditingRole()){
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
            if (!isEditingRole()) {
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
        ContainerStatus status = isEditingRole() ? ContainerStatus.MODIFYING : ContainerStatus.ADDING;
        ObjectWrapper wrapper;
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

    private List<ObjectType> loadApprovers(){
        List<ObjectType> approverList = new ArrayList<>();
        List<ObjectReferenceType> refList = new ArrayList<>();
        ObjectType helper;
        OperationResult result = new OperationResult(OPERATION_LOAD_APPROVERS);

        RoleType actRole = model.getObject().asObjectable();

        if(actRole != null){
            refList.addAll(actRole.getApproverRef());
        }

        if(!refList.isEmpty()){
            for(ObjectReferenceType ref: refList){
                String oid = ref.getOid();
                helper = WebModelUtils.loadObject(ObjectType.class, oid, null, result, this).asObjectable();
                approverList.add(helper);
            }
        }
        result.computeStatus();

        if(approverList.isEmpty()){
            approverList.add(new UserType());
        }

        return approverList;
    }

    private boolean isEditingRole(){
        StringValue oid = getPageParameters().get(OnePageParameterEncoder.PARAMETER);
        return oid != null && StringUtils.isNotEmpty(oid.toString());
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

        TextFormGroup riskLevel = new TextFormGroup(ID_RISK_LEVEL, new PrismPropertyModel(model, RoleType.F_RISK_LEVEL),
                createStringResource("PageRoleEditor.label.riskLevel"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        form.add(riskLevel);

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

        WebMarkupContainer ownerRefContainer = new WebMarkupContainer(ID_OWNER_WRAPPER);
        ownerRefContainer.setOutputMarkupId(true);
        form.add(ownerRefContainer);

        TextField ownerRefLabel = new TextField<>(ID_OWNER_LABEL, createOwnerRefLabelModel());
        ownerRefLabel.setEnabled(false);
        ownerRefContainer.add(ownerRefLabel);

        AjaxLink editOwnerRef = new AjaxLink(ID_OWNER_EDIT) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                editOwnerRefPerformed(target);
            }
        };
        ownerRefContainer.add(editOwnerRef);

        MultiValueChoosePanel approverRef = new MultiValueChoosePanel<ObjectType>(ID_APPROVER_REF, approversModel,
                createStringResource("PageRoleEditor.label.approverRef"), ID_LABEL_SIZE, ID_INPUT_SIZE, false, ObjectType.class) {

            @Override
            protected IModel<String> createTextModel(final IModel<ObjectType> model) {
                return prepareApproverRefLabel(model);
            }

            @Override
            protected ObjectType createNewEmptyItem() {
                return new UserType();
            }

            @Override
            protected void replaceIfEmpty(Object object) {
                boolean added = false;

                List<ObjectType> approverList = approversModel.getObject();
                for (ObjectType approver : approverList) {
                    if (WebMiscUtil.getName(approver) == null || WebMiscUtil.getName(approver).isEmpty()) {
                        approverList.remove(approver);
                        approverList.add((ObjectType) object);
                        added = true;
                        break;
                    }
                }

                if (!added) {
                    approverList.add((ObjectType)object);
                }
            }

            @Override
            protected void initDialog(Class<ObjectType> type) {
                ModalWindow dialog = new ChooseTypeDialog(MODAL_ID_CHOOSE_PANEL, type){

                    @Override
                    protected void chooseOperationPerformed(AjaxRequestTarget target, ObjectType object){
                        choosePerformed(target, object);
                    }

                    @Override
                    protected WebMarkupContainer createExtraContentContainer(String extraContentId) {
                        return new UserOrgReferenceChoosePanel(extraContentId, Boolean.FALSE){

                            @Override
                            protected void onReferenceTypeChangePerformed(AjaxRequestTarget target, Boolean newValue) {
                                updateTableByTypePerformed(target, Boolean.FALSE.equals(newValue) ? UserType.class : OrgType.class);
                            }
                        };
                    }
                };
                add(dialog);
            }
        };
        approverRef.setOutputMarkupId(true);
        form.add(approverRef);

        GenericMultiValueLabelEditPanel minAssignments = new GenericMultiValueLabelEditPanel<MultiplicityPolicyConstraintType>(ID_MIN_ASSIGNMENTS,
                minAssignmentModel, createStringResource("PageRoleEditor.label.minAssignments"), ID_LABEL_SIZE, ID_INPUT_SIZE){

            @Override
            protected void initDialog() {
                ModalWindow dialog = new MultiplicityPolicyDialog(ID_MODAL_EDITOR, null){

                    @Override
                    protected void savePerformed(AjaxRequestTarget target) {
                        closeModalWindow(target);
                        target.add(getMinAssignmentsContainer());
                    }
                };
                add(dialog);
            }

            @Override
            protected IModel<String> createTextModel(IModel<MultiplicityPolicyConstraintType> model) {
                return createMultiplicityPolicyLabel(model);
            }

            @Override
            protected void editValuePerformed(AjaxRequestTarget target, IModel<MultiplicityPolicyConstraintType> rowModel) {
                MultiplicityPolicyDialog window = (MultiplicityPolicyDialog) get(ID_MODAL_EDITOR);
                window.updateModel(target, rowModel.getObject());
                window.show(target);
            }

            @Override
            protected MultiplicityPolicyConstraintType createNewEmptyItem() {
                return new MultiplicityPolicyConstraintType();
            }
        };
        minAssignments.setOutputMarkupId(true);
        form.add(minAssignments);

        GenericMultiValueLabelEditPanel maxAssignments = new GenericMultiValueLabelEditPanel<MultiplicityPolicyConstraintType>(ID_MAX_ASSIGNMENTS,
                maxAssignmentsModel, createStringResource("PageRoleEditor.label.maxAssignments"), ID_LABEL_SIZE, ID_INPUT_SIZE){

            @Override
            protected void initDialog() {
                ModalWindow dialog = new MultiplicityPolicyDialog(ID_MODAL_EDITOR, null){

                    @Override
                    protected void savePerformed(AjaxRequestTarget target) {
                        closeModalWindow(target);
                        target.add(getMaxAssignmentsContainer());
                    }
                };
                add(dialog);
            }

            @Override
            protected IModel<String> createTextModel(IModel<MultiplicityPolicyConstraintType> model) {
                return createMultiplicityPolicyLabel(model);
            }

            @Override
            protected void editValuePerformed(AjaxRequestTarget target, IModel<MultiplicityPolicyConstraintType> rowModel) {
                MultiplicityPolicyDialog window = (MultiplicityPolicyDialog) get(ID_MODAL_EDITOR);
                window.updateModel(target, rowModel.getObject());
                window.show(target);
            }

            @Override
            protected MultiplicityPolicyConstraintType createNewEmptyItem() {
                return new MultiplicityPolicyConstraintType();
            }
        };
        maxAssignments.setOutputMarkupId(true);
        form.add(maxAssignments);

        initModalWindows();
        initExtension(form);
        initAssignmentsAndInducements(form);
        initButtons(form);
    }

    private IModel<String> createMultiplicityPolicyLabel(final IModel<MultiplicityPolicyConstraintType> model){
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                StringBuilder sb = new StringBuilder();

                if(model == null || model.getObject() == null || model.getObject().getMultiplicity() == null
                        || model.getObject().getMultiplicity().isEmpty()){
                    return getString("PageRoleEditor.label.assignmentConstraint.placeholder");
                }

                MultiplicityPolicyConstraintType policy = model.getObject();

                sb.append(policy.getMultiplicity());

                if(policy.getEnforcement() != null){
                    sb.append(" (");
                    sb.append(policy.getEnforcement());
                    sb.append(")");
                }

                return sb.toString();
           }
        };
    }

    private void initModalWindows(){
        ModalWindow ownerChooser = new ChooseTypeDialog(ID_MODAL_OWNER_CHOOSER, UserType.class){

            @Override
            protected void chooseOperationPerformed(AjaxRequestTarget target, ObjectType object) {
                ownerChoosePerformed(target, object);
            }

            @Override
            protected WebMarkupContainer createExtraContentContainer(String extraContentId) {
                return new UserOrgReferenceChoosePanel(extraContentId, Boolean.FALSE){

                    @Override
                    protected void onReferenceTypeChangePerformed(AjaxRequestTarget target, Boolean newValue) {
                        updateTableByTypePerformed(target, Boolean.FALSE.equals(newValue) ? UserType.class : OrgType.class);
                    }
                };
            }
        };
        add(ownerChooser);
    }

    private void initExtension(final Form form){
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
                PrismPropertyPanel propertyPanel = new PrismPropertyPanel(ID_EXTENSION_PROPERTY, item.getModel(), form, PageRole.this);
                propertyPanel.get("labelContainer:label").add(new AttributeAppender("style", "font-weight:bold;"));
                propertyPanel.get("labelContainer").add(new AttributeModifier("class", ID_LABEL_SIZE + " control-label"));
                item.add(propertyPanel);
                item.add(AttributeModifier.append("class", createStyleClassModel(item.getModel())));
            }
        };
        extensionProperties.setReuseItems(true);
        form.add(extensionProperties);
    }

    private void initAssignmentsAndInducements(Form form){
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

    private IModel<String> createOwnerRefLabelModel(){
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                if(model == null || model.getObject() == null || model.getObject().asObjectable() == null){
                    return getString("PageRoleEditor.label.ownerRef.placeholder");
                }

                StringBuilder sb = new StringBuilder();
                RoleType role = model.getObject().asObjectable();

                if(role.getOwnerRef() == null){
                    return getString("PageRoleEditor.label.ownerRef.placeholder");
                }

                ObjectReferenceType ownerRef = role.getOwnerRef();
                FocusType owner;
                OperationResult result = new OperationResult(OPERATION_LOAD_OWNER);

                if(UserType.COMPLEX_TYPE.equals(ownerRef.getType())){
                    owner = WebModelUtils.loadObject(UserType.class, ownerRef.getOid(), result, PageRole.this).asObjectable();
                    sb.append(WebMiscUtil.getName(owner));
                    sb.append("- (");
                    sb.append(UserType.class.getSimpleName());
                    sb.append(")");
                } else if(OrgType.COMPLEX_TYPE.equals(ownerRef.getType())){
                    owner = WebModelUtils.loadObject(OrgType.class, ownerRef.getOid(), result, PageRole.this).asObjectable();
                    sb.append(WebMiscUtil.getName(owner));
                    sb.append("- (");
                    sb.append(OrgType.class.getSimpleName());
                    sb.append(")");
                }

                return sb.toString();
            }
        };
    }

    private IModel<String> prepareApproverRefLabel(final IModel<ObjectType> refModel){
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                if(refModel == null || refModel.getObject() == null || refModel.getObject().getOid() == null){
                    return getString("PageRoleEditor.label.approverRef.placeholder");
                }

                ObjectType object = refModel.getObject();

                StringBuilder sb = new StringBuilder();

                if(object instanceof UserType){
                    sb.append(WebMiscUtil.getName(object));
                    sb.append("- (");
                    sb.append(UserType.class.getSimpleName());
                    sb.append(")");
                } else if(object instanceof OrgType){
                    sb.append(WebMiscUtil.getName(object));
                    sb.append("- (");
                    sb.append(OrgType.class.getSimpleName());
                    sb.append(")");
                }

                return sb.toString();
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
                backPerformed();
            }
        };
        back.setDefaultFormProcessing(false);
        form.add(back);

        form.add(new ExecuteChangeOptionsPanel(ID_EXECUTE_OPTIONS, executeOptionsModel, true, true));
    }

    private void editOwnerRefPerformed(AjaxRequestTarget target){
        ModalWindow window = (ModalWindow)get(ID_MODAL_OWNER_CHOOSER);
        window.show(target);
    }

    private WebMarkupContainer getOwnerRefContainer(){
        return (WebMarkupContainer) get(StringUtils.join(new String[]{ID_MAIN_FORM, ID_OWNER_WRAPPER}, ":"));
    }

    private WebMarkupContainer getMinAssignmentsContainer(){
        return (WebMarkupContainer) get(StringUtils.join(new String[]{ID_MAIN_FORM, ID_MIN_ASSIGNMENTS}, ":"));
    }

    private WebMarkupContainer getMaxAssignmentsContainer(){
        return (WebMarkupContainer) get(StringUtils.join(new String[]{ID_MAIN_FORM, ID_MAX_ASSIGNMENTS}, ":"));
    }

    private void ownerChoosePerformed(AjaxRequestTarget target, ObjectType newOwner){
        if(model == null || model.getObject() == null || model.getObject().asObjectable() == null){
            warn(getString("PageRoleEditor.message.cantAddOwner"));
            target.add(getFeedbackPanel());
            return;
        }

        ModalWindow window = (ModalWindow) get(ID_MODAL_OWNER_CHOOSER);
        window.close(target);

        ObjectReferenceType ownerReference = new ObjectReferenceType();
        ownerReference.setOid(newOwner.getOid());

        if(newOwner instanceof UserType){
            ownerReference.setType(UserType.COMPLEX_TYPE);
        } else if(newOwner instanceof OrgType){
            ownerReference.setType(OrgType.COMPLEX_TYPE);
        }

        model.getObject().asObjectable().setOwnerRef(ownerReference);
        success(getString("PageRoleEditor.message.addOwnerOk", WebMiscUtil.getName(newOwner)));
        target.add(getFeedbackPanel(), getOwnerRefContainer());
    }

    private void handleApproverReferences(PrismObject<RoleType> rolePrism){
        RoleType role = rolePrism.asObjectable();

        if(isEditingRole()){
            if(approversModel != null && approversModel.getObject() != null){
                for(ObjectType approver: approversModel.getObject()){
                    if(approver != null && WebMiscUtil.getName(approver) != null && !WebMiscUtil.getName(approver).isEmpty()){
                        if(!isApprover(approver, role.getApproverRef())){
                            ObjectReferenceType ref = new ObjectReferenceType();
                            ref.setOid(approver.getOid());
                            ref.setType(approver instanceof UserType ? UserType.COMPLEX_TYPE : OrgType.COMPLEX_TYPE);
                            role.getApproverRef().add(ref);
                        }
                    }
                }
            }
        } else {
            if(approversModel != null && approversModel.getObject() != null){
                for(ObjectType approver: approversModel.getObject()){
                    if(approver != null && WebMiscUtil.getName(approver) != null && !WebMiscUtil.getName(approver).isEmpty()){
                        ObjectReferenceType ref = new ObjectReferenceType();
                        ref.setOid(approver.getOid());
                        ref.setType(approver instanceof UserType ? UserType.COMPLEX_TYPE : OrgType.COMPLEX_TYPE);
                        role.getApproverRef().add(ref);
                    }
                }
            }
        }

        //Delete approver references deleted during role edition
        if(isEditingRole()){
            if(approversModel != null && approversModel.getObject() != null){
                for(ObjectReferenceType ref: role.getApproverRef()){
                    if(!isRefInApproverModel(ref)){
                        role.getApproverRef().remove(ref);
                    }
                }
            }
        }
    }

    private boolean isRefInApproverModel(ObjectReferenceType reference){
        for(ObjectType approver: approversModel.getObject()){
            if(reference.getOid().equals(approver.getOid())){
                return true;
            }
        }
        return false;
    }

    private boolean isApprover(ObjectType approver, List<ObjectReferenceType> approverList){
        for(ObjectReferenceType ref: approverList){
            if(ref.getOid().equals(approver.getOid())){
                return true;
            }
        }

        return false;
    }

    private void savePerformed(AjaxRequestTarget target){
        OperationResult result = new OperationResult(OPERATION_SAVE_ROLE);
        try {
            WebMiscUtil.revive(model, getPrismContext());
            WebMiscUtil.revive(approversModel, getPrismContext());
            PrismObject<RoleType> newRole = model.getObject();
            handleApproverReferences(newRole);

            ObjectDelta delta = null;
            if (!isEditingRole()) {

                //handle assignments
                PrismObjectDefinition orgDef = newRole.getDefinition();
                PrismContainerDefinition assignmentDef = orgDef.findContainerDefinition(RoleType.F_ASSIGNMENT);
                AssignmentTablePanel assignmentPanel = (AssignmentTablePanel)get(createComponentPath(ID_MAIN_FORM, ID_ASSIGNMENTS));
                assignmentPanel.handleAssignmentsWhenAdd(newRole, assignmentDef, newRole.asObjectable().getAssignment());

                //handle inducements
                PrismContainerDefinition inducementDef = orgDef.findContainerDefinition(RoleType.F_INDUCEMENT);
                AssignmentTablePanel inducementPanel = (AssignmentTablePanel)get(createComponentPath(ID_MAIN_FORM, ID_INDUCEMENTS));
                inducementPanel.handleAssignmentsWhenAdd(newRole, inducementDef, newRole.asObjectable().getInducement());

                delta = ObjectDelta.createAddDelta(newRole);

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
            ObjectDelta extDelta = null;

            if(extensionDelta != null){
                if(isEditingRole()){
                    extDelta = extensionDelta;
                } else {
                    extDelta = delta.getObjectToAdd().diff(extensionDelta.getObjectToAdd());
                }
            }

            if (delta != null) {
                if(extDelta != null){
                    delta = ObjectDelta.summarize(delta, extDelta);
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

    private void backPerformed(){
        setResponsePage(new PageRoles(false));
    }
}
