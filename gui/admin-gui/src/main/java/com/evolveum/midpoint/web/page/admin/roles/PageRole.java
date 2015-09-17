/*
 * Copyright (c) 2010-2015 Evolveum
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

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.form.multivalue.GenericMultiValueLabelEditPanel;
import com.evolveum.midpoint.web.component.progress.ProgressReportingAwarePage;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.PageTemplate;
import com.evolveum.midpoint.web.page.admin.PageAdminAbstractRole;
import com.evolveum.midpoint.web.page.admin.home.PageDashboard;
import com.evolveum.midpoint.web.page.admin.roles.component.MultiplicityPolicyDialog;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MultiplicityPolicyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

/**
 *  @author shood
 * */
@PageDescriptor(url = "/admin/role", encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(actionUri = PageAdminRoles.AUTH_ROLE_ALL,
                label = PageAdminRoles.AUTH_ROLE_ALL_LABEL,
                description = PageAdminRoles.AUTH_ROLE_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ROLE_URL,
                label = "PageRole.auth.role.label",
                description = "PageRole.auth.role.description")})
public class PageRole extends PageAdminAbstractRole<RoleType> implements ProgressReportingAwarePage {
	
	public static final String AUTH_ROLE_ALL = AuthorizationConstants.AUTZ_UI_ROLES_ALL_URL;
    public static final String AUTH_ROLE_ALL_LABEL = "PageAdminRoles.auth.roleAll.label";
    public static final String AUTH_ROLE_ALL_DESCRIPTION = "PageAdminRoles.auth.roleAll.description";

    private static final Trace LOGGER = TraceManager.getTrace(PageRole.class);

//    private static final String DOT_CLASS = PageRole.class.getName() + ".";
//    private static final String OPERATION_LOAD_ROLE = DOT_CLASS + "loadRole";
//    private static final String OPERATION_CREATE_ROLE_WRAPPER = DOT_CLASS + "createRoleWrapper";
//    private static final String OPERATION_SAVE_ROLE = DOT_CLASS + "saveRole";
//    private static final String OPERATION_LOAD_OWNER = DOT_CLASS + "getOwner";
//    private static final String OPERATION_LOAD_APPROVERS = DOT_CLASS + "loadApprovers";
//
//    private static final String ID_MAIN_FORM = "mainForm";
//    private static final String ID_BUTTON_BACK = "backButton";
//    private static final String ID_BUTTON_SAVE = "saveButton";
//    private static final String ID_BUTTON_ABORT = "abortButton";
//    private static final String ID_NAME = "name";
//    private static final String ID_DISPLAY_NAME = "displayName";
//    private static final String ID_DESCRIPTION = "description";
//    private static final String ID_ROLE_TYPE = "roleType";
//    private static final String ID_REQUESTABLE = "requestable";
//    private static final String ID_IDENTIFIER = "identifier";
//    private static final String ID_DATE_FROM = "dateFrom";
//    private static final String ID_DATE_TO = "dateTo";
//    private static final String ID_ADMIN_STATUS = "adminStatus";
//    private static final String ID_EXTENSION = "extension";
//    private static final String ID_EXTENSION_LABEL = "extensionLabel";
//    private static final String ID_EXTENSION_PROPERTY = "property";
//    private static final String ID_EXECUTE_OPTIONS = "executeOptions";
//    private static final String ID_OWNER_WRAPPER = "ownerRefWrapper";
//    private static final String ID_OWNER_EDIT = "ownerRefEdit";
//    private static final String ID_OWNER_LABEL = "ownerRefLabel";
//    private static final String ID_APPROVER_REF = "approverRef";
//    private static final String ID_RISK_LEVEL = "riskLevel";
    private static final String ID_MIN_ASSIGNMENTS = "minAssignmentsConfig";
    private static final String ID_MAX_ASSIGNMENTS = "maxAssignmentsConfig";

//    private static final String ID_INDUCEMENTS = "inducementsPanel";
//    private static final String ID_ASSIGNMENTS = "assignmentsPanel";
//
//    private static final String ID_MODAL_OWNER_CHOOSER = "ownerChooser";
//
    private static final String ID_LABEL_SIZE = "col-md-4";
    private static final String ID_INPUT_SIZE = "col-md-6";
//
//    private IModel<PrismObject<RoleType>> model;
//    private IModel<List<ObjectType>> approversModel;
//    private IModel<ContainerWrapper> extensionModel;
    private IModel<List<MultiplicityPolicyConstraintType>> minAssignmentModel;
    private IModel<List<MultiplicityPolicyConstraintType>> maxAssignmentsModel;
//    private ObjectWrapper roleWrapper;
//
//    private ProgressReporter progressReporter;
//
//    private LoadableModel<ExecuteChangeOptionsDto> executeOptionsModel
//            = new LoadableModel<ExecuteChangeOptionsDto>(false) {
//
//        @Override
//        protected ExecuteChangeOptionsDto load() {
//            return new ExecuteChangeOptionsDto();
//        }
//    };

    public PageRole() {
        initialize(null);
    }

    public PageRole(PageParameters parameters, PageTemplate previousPage) {
        getPageParameters().overwriteWith(parameters);
        setPreviousPage(previousPage);
        initialize(null);
    }
    
  
    @Override
	protected void performCustomInitialization(){
        minAssignmentModel = new LoadableModel<List<MultiplicityPolicyConstraintType>>(false) {

            @Override
            protected List<MultiplicityPolicyConstraintType> load() {
                RoleType role = (RoleType) getFocusWrapper().getObject().asObjectable();

                if(role.getPolicyConstraints() == null){
                    role.setPolicyConstraints(new PolicyConstraintsType());
                }

                return role.getPolicyConstraints().getMinAssignees();
            }
        };

        maxAssignmentsModel = new LoadableModel<List<MultiplicityPolicyConstraintType>>(false) {

            @Override
            protected List<MultiplicityPolicyConstraintType> load() {
                RoleType role = (RoleType) getFocusWrapper().getObject().asObjectable();

                if(role.getPolicyConstraints() == null){
                    role.setPolicyConstraints(new PolicyConstraintsType());
                }

                return role.getPolicyConstraints().getMaxAssignees();
            }
        };

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

                String roleName = getFocusWrapper().getDisplayName();
                return createStringResource("PageRoleEditor.subtitle.editingRole", roleName).getString();
            }
        };
    }

  
    private boolean isEditingRole(){
        StringValue oid = getPageParameters().get(OnePageParameterEncoder.PARAMETER);
        return oid != null && StringUtils.isNotEmpty(oid.toString());
    }

    protected void initCustomLayout(Form mainForm) {
    	super.initCustomLayout(mainForm);
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
        mainForm.add(minAssignments);

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
        mainForm.add(maxAssignments);

     
    };
       
      
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

   
 

    private WebMarkupContainer getMinAssignmentsContainer(){
        return (WebMarkupContainer) get(StringUtils.join(new String[]{ID_MAIN_FORM, ID_MIN_ASSIGNMENTS}, ":"));
    }

    private WebMarkupContainer getMaxAssignmentsContainer(){
        return (WebMarkupContainer) get(StringUtils.join(new String[]{ID_MAIN_FORM, ID_MAX_ASSIGNMENTS}, ":"));
    }

  

    /**
     * Removes empty policy constraints from role.
     * It was created when loading model (not very good model implementation).
     * MID-2366
     *
     * TODO improve
     *
     * @param prism
     */
    private void removeEmptyPolicyConstraints(PrismObject<RoleType> prism) {
        RoleType role = prism.asObjectable();
        PolicyConstraintsType pc =role.getPolicyConstraints();
        if (pc == null) {
            return;
        }

        if (pc.getExclusion().isEmpty() && pc.getMinAssignees().isEmpty() && pc.getMaxAssignees().isEmpty()) {
            role.setPolicyConstraints(null);
        }
    }
    
    @Override
    protected void prepareFocusDeltaForModify(ObjectDelta<RoleType> focusDelta) throws SchemaException {
    	// TODO policy constraints
    	super.prepareFocusDeltaForModify(focusDelta);
    }
    
    @Override
    protected void prepareFocusForAdd(PrismObject<RoleType> focus) throws SchemaException {
    	// TODO policyConstraints
    	super.prepareFocusForAdd(focus);
    }


    @Override
    protected void setSpecificResponsePage() {
        if (getPreviousPage() != null) {
            goBack(PageDashboard.class);            // parameter is not used
        } else {
            setResponsePage(new PageRoles(false));
        }
    }

	@Override
	protected RoleType createNewFocus() {
		return new RoleType();
	}

	@Override
	protected void reviveCustomModels() throws SchemaException {
		//TODO revivie max min assignments?
	}

	@Override
	protected Class<RoleType> getCompileTimeClass() {
		return RoleType.class;
	}

	@Override
	protected Class getRestartResponsePage() {
		return PageRoles.class;
	}
}
