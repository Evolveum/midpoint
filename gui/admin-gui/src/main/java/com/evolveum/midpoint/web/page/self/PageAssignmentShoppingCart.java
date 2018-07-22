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
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.schema.constants.RelationTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.assignment.*;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.search.SearchFactory;
import com.evolveum.midpoint.web.component.search.SearchPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.self.dto.ShoppingCartConfigurationDto;
import com.evolveum.midpoint.web.session.RoleCatalogStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxChannel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.AbstractReadOnlyModel;
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
    private static final String ID_TREE_PANEL_CONTAINER = "treePanelContainer";
    private static final String ID_TREE_PANEL = "treePanel";
    private static final String ID_CATALOG_ITEMS_GRID_PANEL = "catalogItemsGridPanel";
    private static final String ID_CONTENT_PANEL = "contentPanel";
    private static final String ID_CART_BUTTON = "cartButton";
    private static final String ID_CART_ITEMS_COUNT = "itemsCount";
    private static final String ID_HEADER_PANEL = "headerPanel";
    private static final String ID_SEARCH_FORM = "searchForm";
    private static final String ID_SEARCH = "search";
    private static final String ID_TARGET_USER_PANEL = "targetUserPanel";
    private static final String ID_SOURCE_USER_PANEL = "sourceUserPanel";
    private static final String ID_VIEWS_TAB_PANEL = "viewsTabPanel";
    private static final String ID_PARAMETERS_PANEL = "parametersPanel";
    private static final String DOT_CLASS = PageAssignmentShoppingCart.class.getName() + ".";

    private static final String OPERATION_LOAD_ASSIGNABLE_ROLES = DOT_CLASS + "loadAssignableRoles";
    private static final String OPERATION_GET_ASSIGNMENT_VIEW_LIST = DOT_CLASS + "getRoleCatalogViewsList";
    private static final Trace LOGGER = TraceManager.getTrace(PageAssignmentShoppingCart.class);

    private ShoppingCartConfigurationDto shoppingCartConfigurationDto;
    private LoadableModel<Search> searchModel;
    private UserType selfUser;
    private IModel<RoleManagementConfigurationType> roleManagementConfigModel;

    public PageAssignmentShoppingCart() {
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
//        if (getRoleCatalogStorage().getShoppingCartConfigurationDto() == null){
//            initShoppingCartConfigurationDto();
//            getRoleCatalogStorage().setShoppingCartConfigurationDto(shoppingCartConfigurationDto);
//        }
//        if (StringUtils.isEmpty(getRoleCatalogStorage().getSelectedOid()) && shoppingCartConfigurationDto != null) {
//            getRoleCatalogStorage().setSelectedOid(shoppingCartConfigurationDto.getRoleCatalogOid());
//        }
        initModels();
//        initProvider();
        selfUser = loadUserSelf().asObjectable();


        setOutputMarkupId(true);

        Form mainForm = new com.evolveum.midpoint.web.component.form.Form(ID_MAIN_FORM);
        add(mainForm);

        TabbedPanel tabbedPanel = WebComponentUtil.createTabPanel(ID_VIEWS_TAB_PANEL, PageAssignmentShoppingCart.this, getTabsList(), null);
        tabbedPanel.setOutputMarkupId(true);
        mainForm.add(tabbedPanel);

        WebMarkupContainer parametersPanel = new WebMarkupContainer(ID_PARAMETERS_PANEL);
        parametersPanel.setOutputMarkupId(true);
        mainForm.add(parametersPanel);

        initTargetUserSelectionPanel(parametersPanel);
        initCartButton(parametersPanel);

//        initHeaderPanel(mainForm);

//        WebMarkupContainer contentPanel = new WebMarkupContainer(ID_CONTENT_PANEL);
//        contentPanel.setOutputMarkupId(true);
//        mainForm.add(contentPanel);

//        initContentPanel(contentPanel);

    }

//    private void initShoppingCartConfigurationDto(){
//        this.shoppingCartConfigurationDto = new ShoppingCartConfigurationDto();
//        this.shoppingCartConfigurationDto.initShoppingCartConfigurationDto(getRoleManagementConfigurationType());
//    }

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

        searchModel = new LoadableModel<Search>(false) {
            private static final long serialVersionUID = 1L;
            @Override
            public Search load() {
//                AssignmentViewType viewType =  viewTypeModel.getObject();
                return SearchFactory.createSearch(AbstractRoleType.class, PageAssignmentShoppingCart.this);
            }
        };
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
                            target.add(getCartButton());
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
                    @Override
                    protected QName getQueryType() {
                        return RoleType.COMPLEX_TYPE;
                    }

                    @Override
                    protected void assignmentAddedToShoppingCartPerformed(AjaxRequestTarget target){
                        target.add(getCartButton());
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
                    @Override
                    protected QName getQueryType() {
                        return OrgType.COMPLEX_TYPE;
                    }

                    @Override
                    protected void assignmentAddedToShoppingCartPerformed(AjaxRequestTarget target){
                        target.add(getCartButton());
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
                    @Override
                    protected QName getQueryType() {
                        return ServiceType.COMPLEX_TYPE;
                    }

                    @Override
                    protected void assignmentAddedToShoppingCartPerformed(AjaxRequestTarget target){
                        target.add(getCartButton());
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
                    @Override
                    protected void assignmentAddedToShoppingCartPerformed(AjaxRequestTarget target){
                        target.add(getCartButton());
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

    private void initHeaderPanel(Form mainForm){
        WebMarkupContainer headerPanel = new WebMarkupContainer(ID_HEADER_PANEL);
        headerPanel.setOutputMarkupId(true);

//        initViewSelector(headerPanel);
        initTargetUserSelectionPanel(headerPanel);
//        initSourceUserSelectionPanel(headerPanel);
        initCartButton(headerPanel);
        initSearchPanel(headerPanel);
        mainForm.add(headerPanel);
    }

     private void initTargetUserSelectionPanel(WebMarkupContainer parametersPanel){
        UserSelectionButton targetUserPanel = new UserSelectionButton(ID_TARGET_USER_PANEL,
                new AbstractReadOnlyModel<List<UserType>>() {
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


//    private void initViewSelector(WebMarkupContainer headerPanel){
//        DropDownChoice<AssignmentViewType> viewSelect = new DropDownChoice(ID_VIEW_TYPE, viewTypeModel,
//                Model.ofList(getRoleCatalogStorage().getShoppingCartConfigurationDto().getViewTypeList()),
//                new EnumChoiceRenderer<AssignmentViewType>(this));
//        viewSelect.add(new OnChangeAjaxBehavior() {
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            protected void onUpdate(AjaxRequestTarget target) {
//                initProvider();
//                searchModel.reset();
//
//                target.add(getTabbedPanel());
//                target.add(getHeaderPanel());
//            }
//        });
//        viewSelect.add(new VisibleEnableBehaviour(){
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            public boolean isEnabled(){
//                return getRoleCatalogStorage().getAssignmentsUserOwner() == null;
//            }
//        });
//        viewSelect.setOutputMarkupId(true);
//        headerPanel.add(viewSelect);
//
//    }

    private void initSearchPanel(WebMarkupContainer headerPanel) {
        final Form searchForm = new com.evolveum.midpoint.web.component.form.Form(ID_SEARCH_FORM);
        searchForm.setOutputMarkupId(true);

        SearchPanel search = new SearchPanel(ID_SEARCH, (IModel) searchModel, false) {
            private static final long serialVersionUID = 1L;

            @Override
            public void searchPerformed(ObjectQuery query, AjaxRequestTarget target) {
//                PageAssignmentShoppingCart.this.searchPerformed(query, target);
            }
        };
        searchForm.add(search);
        headerPanel.add(searchForm);
    }

//    private void searchPerformed(ObjectQuery query, AjaxRequestTarget target) {
//        provider.setQuery(createContentQuery(query));
//        target.add(getCatalogItemsPanelContainer());
//    }

    private void initCartButton(WebMarkupContainer parametersPanel){
        AjaxButton cartButton = new AjaxButton(ID_CART_BUTTON) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                attributes.setChannel(new AjaxChannel("blocking", AjaxChannel.Type.ACTIVE));
            }

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                navigateToNext(new PageAssignmentsList(true));
            }
        };
        cartButton.setOutputMarkupId(true);
        parametersPanel.add(cartButton);

        Label cartItemsCount = new Label(ID_CART_ITEMS_COUNT,  new LoadableModel<String>(true) {
            private static final long serialVersionUID = 1L;

            @Override
            public String load(){
                return Integer.toString(getRoleCatalogStorage().getAssignmentShoppingCart().size());
            }
        });
        cartItemsCount.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return !(getRoleCatalogStorage().getAssignmentShoppingCart().size() == 0);
            }
        });
        cartItemsCount.setOutputMarkupId(true);
        cartButton.add(cartItemsCount);
    }

    private TabbedPanel getTabbedPanel(){
        return (TabbedPanel) get(createComponentPath(ID_MAIN_FORM, ID_VIEWS_TAB_PANEL));
    }

    private UserType getTargetUser(){
        if (getRoleCatalogStorage().isSelfRequest()){
            return selfUser;
        }
        return getRoleCatalogStorage().getTargetUserList().get(0);
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
            String name = usersList.get(0).getName().getOrig();
            return createStringResource("AssignmentCatalogPanel.requestFor", name).getString();
        } else {
            return createStringResource("AssignmentCatalogPanel.requestForMultiple",
                    usersList.size()).getString();
        }
    }

    private AjaxButton getCartButton(){
        return (AjaxButton) get(ID_MAIN_FORM).get(ID_PARAMETERS_PANEL).get(ID_CART_BUTTON);
    }

}
