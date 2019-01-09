/*
 * Copyright (c) 2016-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.web.page.self;

import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.assignment.*;
import com.evolveum.midpoint.web.component.input.RelationDropDownChoicePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.self.dto.AssignmentViewType;
import com.evolveum.midpoint.web.session.RoleCatalogStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_4.*;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;

import javax.xml.namespace.QName;
import java.util.*;

/**
 * Created by honchar.
 */
@PageDescriptor(url = {"/self/assignmentShoppingCart"}, action = {
        @AuthorizationAction(actionUri = PageSelf.AUTH_SELF_ALL_URI,
                label = PageSelf.AUTH_SELF_ALL_LABEL,
                description = PageSelf.AUTH_SELF_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SELF_ASSIGNMENT_SHOP_KART_URL,
                label = "PageAssignmentShoppingCart.auth.requestAssignment.label",
                description = "PageAssignmentShoppingCart.auth.requestAssignment.description")})
public class PageAssignmentShoppingCart<R extends AbstractRoleType> extends PageSelf {
    private static final long serialVersionUID = 1L;

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_TARGET_USER_PANEL = "targetUserPanel";
    private static final String ID_VIEWS_TAB_PANEL = "viewsTabPanel";
    private static final String ID_PARAMETERS_PANEL = "parametersPanel";
    private static final String DOT_CLASS = PageAssignmentShoppingCart.class.getName() + ".";
    private static final String ID_RELATION_CONTAINER = "relationContainer";
    private static final String ID_RELATION = "relation";

    private static final String OPERATION_GET_ASSIGNMENT_VIEW_LIST = DOT_CLASS + "getRoleCatalogViewsList";
    private static final String OPERATION_LOAD_RELATION_DEFINITIONS = DOT_CLASS + "loadRelationDefinitions";
    private static final String OPERATION_LOAD_ASSIGNMENTS_LIMIT = DOT_CLASS + "loadAssignmentsLimit";
    private static final String OPERATION_LOAD_DEFAULT_VIEW_TYPE = DOT_CLASS + "loadDefaultViewType";
    private static final Trace LOGGER = TraceManager.getTrace(PageAssignmentShoppingCart.class);

   private IModel<RoleManagementConfigurationType> roleManagementConfigModel;

    public PageAssignmentShoppingCart() {
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initModels();
        getRoleCatalogStorage().setAssignmentRequestLimit(
                AssignmentsUtil.loadAssignmentsLimit(new OperationResult(OPERATION_LOAD_ASSIGNMENTS_LIMIT), this));

        setOutputMarkupId(true);

        Form mainForm = new com.evolveum.midpoint.web.component.form.Form(ID_MAIN_FORM);
        add(mainForm);

        List<ITab> tabs = getTabsList();
        TabbedPanel tabbedPanel = new TabbedPanel<ITab>(ID_VIEWS_TAB_PANEL, tabs) {

            private static final long serialVersionUID = 1L;

            @Override
            public TabbedPanel<ITab> setSelectedTab(int index) {
                getRoleCatalogStorage().setDefaultTabIndex(index);
                return super.setSelectedTab(index);
            }
        };
        tabbedPanel.setOutputMarkupId(true);

        int defaultSelectedTabIndex  = getDefaultViewTypeIndex();
        if (getRoleCatalogStorage().getDefaultTabIndex() > 0 && getRoleCatalogStorage().getDefaultTabIndex() < tabs.size()){
            tabbedPanel.setSelectedTab(getRoleCatalogStorage().getDefaultTabIndex());
        } else if (defaultSelectedTabIndex < tabs.size()){
            tabbedPanel.setSelectedTab(defaultSelectedTabIndex);
        }
        mainForm.add(tabbedPanel);

        WebMarkupContainer parametersPanel = new WebMarkupContainer(ID_PARAMETERS_PANEL);
        parametersPanel.setOutputMarkupId(true);
        mainForm.add(parametersPanel);

        initTargetUserSelectionPanel(parametersPanel);
        initRelationPanel(parametersPanel);
    }

    private void initModels(){
        roleManagementConfigModel = new LoadableModel<RoleManagementConfigurationType>(false) {
            @Override
            protected RoleManagementConfigurationType load() {
                    OperationResult result = new OperationResult(OPERATION_GET_ASSIGNMENT_VIEW_LIST);
                    SystemConfigurationType config = null;
                    try {
                        config = getModelInteractionService().getSystemConfiguration(result);
                    } catch (ObjectNotFoundException | SchemaException e) {
                        LOGGER.error("Error getting system configuration: {}", e.getMessage(), e);
                        return null;
                    }
                    if (config != null) {
                        return config.getRoleManagement();
                    }
                    return null;
            }
        };

    }

    private int getDefaultViewTypeIndex(){
        RoleManagementConfigurationType roleConfig = roleManagementConfigModel.getObject();

        if (roleConfig == null || roleConfig.getDefaultCollection() == null || roleConfig.getDefaultCollection().getCollectionUri() == null){
            return 0;
        }
        List<AssignmentViewType> viewTypes = Arrays.asList(AssignmentViewType.values());
        for (AssignmentViewType viewType : viewTypes){
            if (viewType.getUri().equals(roleConfig.getDefaultCollection().getCollectionUri())){
                return viewTypes.indexOf(viewType);
            }
        }
        return 0;
    }

    private List<ITab> getTabsList(){
        List<ITab> tabs = new ArrayList<>();

        String roleCatalogOid = getRoleCatalogOid();
        if (StringUtils.isNotEmpty(roleCatalogOid)) {

            tabs.add(new PanelTab<AbstractRoleType>(createStringResource("AssignmentViewType.ROLE_CATALOG_VIEW"),
                    getTabVisibleBehaviour(SchemaConstants.OBJECT_COLLECTION_ROLE_CATALOG_URI)) {

                private static final long serialVersionUID = 1L;

                @Override
                public WebMarkupContainer createPanel(String panelId) {
                    return new RoleCatalogTabPanel(panelId, roleManagementConfigModel.getObject(), roleCatalogOid){
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected void assignmentAddedToShoppingCartPerformed(AjaxRequestTarget target){
                            reloadShoppingCartIcon(target);
                        }

                        @Override
                        protected QName getNewAssignmentRelation(){
                            return getRelationParameterValue();
                        }

                    };
                }
            });
        }
        tabs.add(new PanelTab<AbstractRoleType>(createStringResource("AssignmentViewType.ROLE_TYPE"),
                getTabVisibleBehaviour(SchemaConstants.OBJECT_COLLECTION_ALL_ROLES_URI)) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new AbstractShoppingCartTabPanel(panelId, roleManagementConfigModel.getObject()) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected QName getQueryType() {
                        return RoleType.COMPLEX_TYPE;
                    }

                    @Override
                    protected void assignmentAddedToShoppingCartPerformed(AjaxRequestTarget target){
                        reloadShoppingCartIcon(target);
                    }

                    @Override
                    protected QName getNewAssignmentRelation(){
                        return getRelationParameterValue();
                    }
                };
            }
        });
        tabs.add(new PanelTab<AbstractRoleType>(createStringResource("AssignmentViewType.ORG_TYPE"),
                getTabVisibleBehaviour(SchemaConstants.OBJECT_COLLECTION_ALL_ORGS_URI)) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new AbstractShoppingCartTabPanel(panelId, roleManagementConfigModel.getObject()) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected QName getQueryType() {
                        return OrgType.COMPLEX_TYPE;
                    }

                    @Override
                    protected void assignmentAddedToShoppingCartPerformed(AjaxRequestTarget target){
                        reloadShoppingCartIcon(target);
                    }

                    @Override
                    protected QName getNewAssignmentRelation(){
                        return getRelationParameterValue();
                    }
                };
            }
        });
        tabs.add(new PanelTab<AbstractRoleType>(createStringResource("AssignmentViewType.SERVICE_TYPE"),
                getTabVisibleBehaviour(SchemaConstants.OBJECT_COLLECTION_ALL_SERVICES_URI)) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new AbstractShoppingCartTabPanel(panelId, roleManagementConfigModel.getObject()) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected QName getQueryType() {
                        return ServiceType.COMPLEX_TYPE;
                    }

                    @Override
                    protected void assignmentAddedToShoppingCartPerformed(AjaxRequestTarget target){
                        reloadShoppingCartIcon(target);
                    }

                    @Override
                    protected QName getNewAssignmentRelation(){
                        return getRelationParameterValue();
                    }
                };
            }
        });
        tabs.add(new PanelTab<AbstractRoleType>(createStringResource("AssignmentViewType.USER_TYPE"),
                getTabVisibleBehaviour(SchemaConstants.OBJECT_COLLECTION_USER_ASSIGNMENTS_URI)) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new UserViewTabPanel(panelId, roleManagementConfigModel.getObject()) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void assignmentAddedToShoppingCartPerformed(AjaxRequestTarget target){
                        reloadShoppingCartIcon(target);
                    }

                    @Override
                    protected QName getNewAssignmentRelation(){
                        return getRelationParameterValue();
                    }
                };
            }
        });
        return tabs;
    }

    private VisibleEnableBehaviour getTabVisibleBehaviour(String viewType){
        return new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible(){
                if (SchemaConstants.OBJECT_COLLECTION_ROLE_CATALOG_URI.equals(viewType)){
                    if (StringUtils.isEmpty(getRoleCatalogOid())){
                        return false;
                    } else {
                        return true;
                    }
                }

                RoleManagementConfigurationType config = roleManagementConfigModel.getObject();
                if (config == null || config.getRoleCatalogCollections() == null){
                    return true;
                }

                List<ObjectCollectionUseType> viewsList = config.getRoleCatalogCollections().getCollection();
                if (viewsList.isEmpty()){
                    return true;
                }
                for (ObjectCollectionUseType view : viewsList){
                    if (viewType.equals(view.getCollectionUri())){
                        return true;
                    }
                }
                return false;
            }
        };
    }

    private void initTargetUserSelectionPanel(WebMarkupContainer parametersPanel){
        UserSelectionButton targetUserPanel = new UserSelectionButton(ID_TARGET_USER_PANEL,
                new IModel<List<UserType>>() {
                    @Override
                    public List<UserType> getObject() {
                        return getRoleCatalogStorage().getTargetUserList();
                    }
                },
                true, createStringResource("AssignmentCatalogPanel.selectTargetUser")){
            private static final long serialVersionUID = 1L;

            @Override
            protected String getUserButtonLabel(){
                return getTargetUserSelectionButtonLabel(getModelObject());
            }

            @Override
            protected String getTargetUserButtonClass(){
                return "btn-sm";
            }

            @Override
            protected void onDeleteSelectedUsersPerformed(AjaxRequestTarget target){
                super.onDeleteSelectedUsersPerformed(target);
                getRoleCatalogStorage().setTargetUserList(new ArrayList<>());

                target.add(PageAssignmentShoppingCart.this.getTabbedPanel());
                target.add(parametersPanel.get(ID_TARGET_USER_PANEL));
            }

            @Override
            protected void multipleUsersSelectionPerformed(AjaxRequestTarget target, List<UserType> usersList){
                getRoleCatalogStorage().setTargetUserList(usersList);
                target.add(PageAssignmentShoppingCart.this.getTabbedPanel());
                target.add(parametersPanel.get(ID_TARGET_USER_PANEL));
           }

        };
        targetUserPanel.setOutputMarkupId(true);
        parametersPanel.add(targetUserPanel);
    }

    private void initRelationPanel(WebMarkupContainer parametersPanel){
        WebMarkupContainer relationContainer = new WebMarkupContainer(ID_RELATION_CONTAINER);
        relationContainer.setOutputMarkupId(true);
        parametersPanel.add(relationContainer);

        List<QName> availableRelations = WebComponentUtil.getCategoryRelationChoices(AreaCategoryType.SELF_SERVICE, PageAssignmentShoppingCart.this);
        relationContainer.add(new RelationDropDownChoicePanel(ID_RELATION, getRoleCatalogStorage().getSelectedRelation(),
                availableRelations, false){
            private static final long serialVersionUID = 1L;

            @Override
            protected void onValueChanged(AjaxRequestTarget target){
                getRoleCatalogStorage().setSelectedRelation(getRelationValue());
            }
        });
    }

    private QName getRelationParameterValue(){
        return getRelationDropDown().getRelationValue();
    }

    private RelationDropDownChoicePanel getRelationDropDown(){
        return (RelationDropDownChoicePanel)get(createComponentPath(ID_MAIN_FORM, ID_PARAMETERS_PANEL, ID_RELATION_CONTAINER, ID_RELATION));
    }

    private TabbedPanel getTabbedPanel(){
        return (TabbedPanel) get(createComponentPath(ID_MAIN_FORM, ID_VIEWS_TAB_PANEL));
    }

    private RoleCatalogStorage getRoleCatalogStorage(){
        return getSessionStorage().getRoleCatalog();
    }

    private String getRoleCatalogOid(){
        RoleManagementConfigurationType config = roleManagementConfigModel.getObject();
        return config == null || config.getRoleCatalogRef() == null ? null : config.getRoleCatalogRef().getOid();
    }

    private String getTargetUserSelectionButtonLabel(List<UserType> usersList){
        if (usersList == null || usersList.size() == 0){
            return createStringResource("AssignmentCatalogPanel.requestForMe").getString();
        } else if (usersList.size() == 1){
            if (usersList.get(0).getOid().equals(loadUserSelf().getOid())){
                return createStringResource("AssignmentCatalogPanel.requestForMe").getString();
            } else {
                return usersList.get(0).getName().getOrig();
            }
        } else {
            return createStringResource("AssignmentCatalogPanel.requestForMultiple",
                    usersList.size()).getString();
        }
    }
}
