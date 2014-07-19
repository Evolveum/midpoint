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

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.*;
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
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.PrismPropertyModel;
import com.evolveum.midpoint.web.page.admin.resources.PageAdminResources;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.web.util.WebModelUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
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
public class PageRole extends PageAdminRoles{

    private static final Trace LOGGER = TraceManager.getTrace(PageRole.class);

    private static final String DOT_CLASS = PageRole.class.getName() + ".";
    private static final String OPERATION_LOAD_ROLE = DOT_CLASS + "loadRole";
    private static final String OPERATION_LOAD_ASSIGNMENT = DOT_CLASS + "loadAssignment";
    private static final String OPERATION_SAVE_ROLE = DOT_CLASS + "saveRole";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_BUTTON_BACK = "backButton";
    private static final String ID_BUTTON_SAVE = "saveButton";
    private static final String ID_NAME = "name";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_ROLE_TYPE = "roleType";
    private static final String ID_REQUESTABLE = "requestable";
    private static final String ID_DATE_FROM = "dateFrom";
    private static final String ID_DATE_TO = "dateTo";
    private static final String ID_ADMIN_STATUS = "adminStatus";

    private static final String ID_INDUCEMENTS = "inducementsPanel";
    private static final String ID_ASSIGNMENTS = "assignmentsPanel";


    private static final String ID_LABEL_SIZE = "col-md-4";
    private static final String ID_INPUT_SIZE = "col-md-8";

    private IModel<PrismObject<RoleType>> model;

    public PageRole(){

        model = new LoadableModel<PrismObject<RoleType>>(false) {
            @Override
            protected PrismObject<RoleType> load() {
                return loadRole();
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

                String roleName = model.getObject().asObjectable().getName().getOrig();
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
        Form form = new Form(ID_MAIN_FORM);
        add(form);

        TextFormGroup name = new TextFormGroup(ID_NAME, new PrismPropertyModel(model, RoleType.F_NAME),
                createStringResource("PageRoleEditor.label.name"), ID_LABEL_SIZE, ID_INPUT_SIZE, true);
        form.add(name);

        TextAreaFormGroup description = new TextAreaFormGroup(ID_DESCRIPTION, new PrismPropertyModel(model, RoleType.F_DESCRIPTION),
                createStringResource("PageRoleEditor.label.description"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        form.add(description);

        TextFormGroup roleType = new TextFormGroup(ID_ROLE_TYPE, new PrismPropertyModel(model, RoleType.F_ROLE_TYPE),
                createStringResource("PageRoleEditor.label.type"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        form.add(roleType);

        CheckFormGroup requestable = new CheckFormGroup(ID_REQUESTABLE, new PrismPropertyModel(model, RoleType.F_REQUESTABLE),
                createStringResource("PageRoleEditor.label.requestable"), ID_LABEL_SIZE, ID_INPUT_SIZE);
        form.add(requestable);

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

        initButtons(form);
    }

    private void initButtons(Form form){
        AjaxSubmitButton save = new AjaxSubmitButton(ID_BUTTON_SAVE, createStringResource("PageBase.button.save")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form){
                savePerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form){
                target.add(form);
                target.add(getFeedbackPanel());
            }
        };
        form.add(save);

        AjaxSubmitButton back = new AjaxSubmitButton(ID_BUTTON_BACK, createStringResource("PageBase.button.back")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form){
                backPerformed(target);
            }
        };
        form.add(back);
    }

    private void savePerformed(AjaxRequestTarget target){
        OperationResult result = new OperationResult(OPERATION_SAVE_ROLE);
        try {
            WebMiscUtil.revive(model, getPrismContext());

            ModelService modelService = getModelService();

            PrismObject<RoleType> newRole = model.getObject();

            ObjectDelta delta = null;
            if (!isEditing()) {
                delta = ObjectDelta.createAddDelta(newRole);

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

            if (delta != null) {
                Collection<ObjectDelta<? extends ObjectType>> deltas = WebMiscUtil.createDeltaCollection(delta);
                modelService.executeChanges(deltas, null, createSimpleTask(OPERATION_SAVE_ROLE), result);
            }
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't save role", ex);
            result.recordFatalError("Couldn't save role.", ex);
        } finally {
            result.computeStatusIfUnknown();
        }

        if (WebMiscUtil.isSuccessOrHandledError(result)) {
            showResultInSession(result);
            setResponsePage(PageRoles.class);
        } else {
            showResult(result);
            target.add(getFeedbackPanel());
        }
    }

    private void backPerformed(AjaxRequestTarget target){
        setResponsePage(PageRoles.class);
    }
}
