/*
 * Copyright (c) 2016-2017 Evolveum
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

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.RoleSelectionSpecification;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.assignment.*;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.search.SearchFactory;
import com.evolveum.midpoint.web.component.search.SearchPanel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.orgs.OrgTreePanel;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.web.page.self.dto.AssignmentViewType;
import com.evolveum.midpoint.web.page.self.dto.ShoppingCartConfigurationDto;
import com.evolveum.midpoint.web.session.OrgTreeStateStorage;
import com.evolveum.midpoint.web.session.RoleCatalogStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxChannel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.*;

/**
 * Created by honchar.
 */
@PageDescriptor(url = {"/self/assignmentShoppingCart"}, action = {
        @AuthorizationAction(actionUri = PageSelf.AUTH_SELF_ALL_URI,
                label = PageSelf.AUTH_SELF_ALL_LABEL,
                description = PageSelf.AUTH_SELF_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SELF_ASSIGNMENT_SHOP_KART_URL,
                label = "PageAssignmentShoppingKart.auth.requestAssignment.label",
                description = "PageAssignmentShoppingKart.auth.requestAssignment.description")})
public class PageAssignmentShoppingKart extends PageSelf {
    private static final long serialVersionUID = 1L;

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_TREE_PANEL_CONTAINER = "treePanelContainer";
    private static final String ID_TREE_PANEL = "treePanel";
    private static final String ID_NO_CATALOG_REF_DEFINED_LABEL = "noCatalogRefDefinedLabel";
    private static final String ID_CATALOG_ITEMS_GRID_PANEL = "catalogItemsGridPanel";
    private static final String ID_CONTENT_PANEL = "contentPanel";
    private static final String ID_CART_BUTTON = "cartButton";
    private static final String ID_CART_ITEMS_COUNT = "itemsCount";
    private static final String ID_HEADER_PANEL = "headerPanel";
    private static final String ID_VIEW_TYPE = "viewTypeSelect";
    private static final String ID_SEARCH_FORM = "searchForm";
    private static final String ID_SEARCH = "search";
    private static final String ID_TARGET_USER_PANEL = "targetUserPanel";
    private static final String ID_SOURCE_USER_PANEL = "sourceUserPanel";
    private static final String DOT_CLASS = PageAssignmentShoppingKart.class.getName() + ".";

    private static final String OPERATION_LOAD_ASSIGNABLE_ROLES = DOT_CLASS + "loadAssignableRoles";
    private static final String OPERATION_GET_ASSIGNMENT_VIEW_LIST = DOT_CLASS + "getRoleCatalogViewsList";
    private static final Trace LOGGER = TraceManager.getTrace(PageAssignmentShoppingKart.class);

    private ShoppingCartConfigurationDto shoppingCartConfigurationDto;
    private LoadableModel<Search> searchModel;
    private IModel<AssignmentViewType> viewTypeModel;
    private UserType selfUser;
    private BaseSortableDataProvider provider;

    public PageAssignmentShoppingKart() {
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        if (getRoleCatalogStorage().getShoppingCartConfigurationDto() == null){
            initShoppingCartConfigurationDto();
            getRoleCatalogStorage().setShoppingCartConfigurationDto(shoppingCartConfigurationDto);
        }
        if (StringUtils.isEmpty(getRoleCatalogStorage().getSelectedOid()) && shoppingCartConfigurationDto != null) {
            getRoleCatalogStorage().setSelectedOid(shoppingCartConfigurationDto.getRoleCatalogOid());
        }
        initModels();
        initProvider();
        selfUser = loadUserSelf().asObjectable();


        setOutputMarkupId(true);

        Form mainForm = new Form(ID_MAIN_FORM);
        add(mainForm);

        initHeaderPanel(mainForm);

        WebMarkupContainer contentPanel = new WebMarkupContainer(ID_CONTENT_PANEL);
        contentPanel.setOutputMarkupId(true);
        mainForm.add(contentPanel);

        Label noCatalogRefDefinedLabel = new Label(ID_NO_CATALOG_REF_DEFINED_LABEL, createStringResource("PageAssignmentShoppingKart.roleCatalogIsNotConfigured"));
        noCatalogRefDefinedLabel.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;
            @Override
            public boolean isVisible(){
                return AssignmentViewType.ROLE_CATALOG_VIEW.equals(viewTypeModel.getObject()) &&
                        isCatalogOidEmpty();
            }
        });
        contentPanel.add(noCatalogRefDefinedLabel);

        initContentPanel(contentPanel);

    }

    private void initShoppingCartConfigurationDto(){
        this.shoppingCartConfigurationDto = new ShoppingCartConfigurationDto();
        this.shoppingCartConfigurationDto.initShoppingCartConfigurationDto(getRoleManagementConfigurationType());
    }

    private void initModels(){
        viewTypeModel = new IModel<AssignmentViewType>() {
            private static final long serialVersionUID = 1L;

            @Override
            public AssignmentViewType getObject() {
                return getRoleCatalogStorage().getShoppingCartConfigurationDto().getDefaultViewType();
            }

            @Override
            public void setObject(AssignmentViewType assignmentViewType) {
                getRoleCatalogStorage().getShoppingCartConfigurationDto().setDefaultViewType(assignmentViewType);
            }

            @Override
            public void detach() {

            }
        };

        searchModel = new LoadableModel<Search>(false) {
            private static final long serialVersionUID = 1L;
            @Override
            public Search load() {
                AssignmentViewType viewType =  viewTypeModel.getObject();
                return SearchFactory.createSearch(viewType.getType(), PageAssignmentShoppingKart.this);
            }
        };
    }

    private void initProvider() {
            provider = new ObjectDataProvider<AssignmentEditorDto, AbstractRoleType>(PageAssignmentShoppingKart.this, AbstractRoleType.class) {
                private static final long serialVersionUID = 1L;

                @Override
                public AssignmentEditorDto createDataObjectWrapper(PrismObject<AbstractRoleType> obj) {

                    AssignmentEditorDto dto = AssignmentEditorDto.createDtoFromObject(obj.asObjectable(), UserDtoStatus.ADD, PageAssignmentShoppingKart.this);
                    if (!getRoleCatalogStorage().isMultiUserRequest()) {
                        dto.setAlreadyAssigned(isAlreadyAssigned(obj, dto));
                        dto.setDefualtAssignmentConstraints(getRoleCatalogStorage().getShoppingCartConfigurationDto().getDefaultAssignmentConstraints());
                    }
                    return dto;
                }

                @Override
                public ObjectQuery getQuery() {
                    return createContentQuery(null);
                }
            };
    }

    private void initHeaderPanel(Form mainForm){
        WebMarkupContainer headerPanel = new WebMarkupContainer(ID_HEADER_PANEL);
        headerPanel.setOutputMarkupId(true);

        initViewSelector(headerPanel);
        initTargetUserSelectionPanel(headerPanel);
        initSourceUserSelectionPanel(headerPanel);
        initCartButton(headerPanel);
        initSearchPanel(headerPanel);
        mainForm.add(headerPanel);
    }

    public void initContentPanel(WebMarkupContainer contentPanel) {
        WebMarkupContainer treePanelContainer = new WebMarkupContainer(ID_TREE_PANEL_CONTAINER);
        treePanelContainer.setOutputMarkupId(true);
        contentPanel.add(treePanelContainer);

            OrgTreePanel treePanel = new OrgTreePanel(ID_TREE_PANEL,
                    Model.of(!isCatalogOidEmpty() ? getRoleCatalogStorage().getShoppingCartConfigurationDto().getRoleCatalogOid() : ""),
                    false, PageAssignmentShoppingKart.this, "AssignmentShoppingCartPanel.treeTitle") {
                private static final long serialVersionUID = 1L;

                @Override
                protected void selectTreeItemPerformed(SelectableBean<OrgType> selected,
                                                       AjaxRequestTarget target) {
                    PageAssignmentShoppingKart.this.selectTreeItemPerformed(selected, target);
                }

                protected List<InlineMenuItem> createTreeMenu() {
                    return new ArrayList<>();
                }

                @Override
                protected List<InlineMenuItem> createTreeChildrenMenu(OrgType org) {
                    return new ArrayList<>();
                }

                @Override
                protected OrgTreeStateStorage getOrgTreeStateStorage(){
                    return getRoleCatalogStorage();
                }
            };
            treePanel.setOutputMarkupId(true);
            treePanelContainer.add(new VisibleEnableBehaviour(){
                private static final long serialVersionUID = 1L;
                @Override
                public boolean isVisible(){
                    return AssignmentViewType.ROLE_CATALOG_VIEW.equals(viewTypeModel.getObject()) &&
                            !isCatalogOidEmpty();
                }
            });
        treePanelContainer.add(treePanel);

        initCatalogItemsPanel(contentPanel);
    }

    private void initCatalogItemsPanel(WebMarkupContainer panelContainer){
        GridViewComponent<AssignmentEditorDto> catalogItemsGrid = new GridViewComponent(ID_CATALOG_ITEMS_GRID_PANEL,
                new AbstractReadOnlyModel<IDataProvider>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public IDataProvider getObject() {
                        return provider;
                    }
                }) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onBeforeRender(){
                super.onBeforeRender();
                add(getCatalogItemsPanelClassAppender());
            }

            @Override
            protected void populateItem(Item item) {
                item.add(new RoleCatalogItemButton(getCellItemId(), item.getModel()){
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void assignmentAddedToShoppingCartPerformed(AjaxRequestTarget target){
                        PageAssignmentShoppingKart.this.reloadCartButton(target);

                    }
                });
            }
        };
        catalogItemsGrid.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;
            @Override
            public boolean isVisible(){
                return !(AssignmentViewType.ROLE_CATALOG_VIEW.equals(viewTypeModel.getObject()) &&
                        isCatalogOidEmpty());
            }
        });
        catalogItemsGrid.setOutputMarkupId(true);
        panelContainer.add(catalogItemsGrid);
    }

    private void selectTreeItemPerformed(SelectableBean<OrgType> selected, AjaxRequestTarget target) {
        final OrgType selectedOrg = selected.getValue();
        if (selectedOrg == null) {
            return;
        }
        getRoleCatalogStorage().setSelectedOid(selectedOrg.getOid());
        target.add(getCatalogItemsPanelContainer());

    }

    private void initTargetUserSelectionPanel(WebMarkupContainer headerPanel){
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

                target.add(getContentPanel());
                target.add(getHeaderPanel());
            }

            @Override
            protected void multipleUsersSelectionPerformed(AjaxRequestTarget target, List<UserType> usersList){
                getRoleCatalogStorage().setTargetUserList(usersList);
                target.add(getContentPanel());
                target.add(getHeaderPanel());
            }

        };
        targetUserPanel.setOutputMarkupId(true);
        headerPanel.add(targetUserPanel);
    }

    private void initSourceUserSelectionPanel(WebMarkupContainer headerPanel){

        UserSelectionButton sourceUserPanel = new UserSelectionButton(ID_SOURCE_USER_PANEL,
                new AbstractReadOnlyModel<List<UserType>>() {
                    @Override
                    public List<UserType> getObject() {
                        List<UserType> usersList = new ArrayList<>();
                        if (getRoleCatalogStorage().getAssignmentsUserOwner() != null){
                            usersList.add(getRoleCatalogStorage().getAssignmentsUserOwner());
                        }
                        return usersList;
                    }
                }, false, createStringResource("AssignmentCatalogPanel.selectSourceUser")){
            private static final long serialVersionUID = 1L;

            @Override
            protected void singleUserSelectionPerformed(AjaxRequestTarget target, UserType user){
                super.singleUserSelectionPerformed(target, user);
                viewTypeModel.setObject(AssignmentViewType.USER_TYPE);
                getRoleCatalogStorage().setAssignmentsUserOwner(user);

                initProvider();
//                TODO don't remove component
//                getContentPanel().remove(ID_CATALOG_ITEMS_PANEL_CONTAINER);
//                initCatalogItemsPanel(getContentPanel());
                searchModel.reset();

                target.add(getContentPanel());
                target.add(getHeaderPanel());
            }

            @Override
            protected String getUserButtonLabel(){
                return getSourceUserSelectionButtonLabel(getModelObject() != null && getModelObject().size() > 0 ? getModelObject().get(0) : null);
            }

            @Override
            protected void onDeleteSelectedUsersPerformed(AjaxRequestTarget target){
                super.onDeleteSelectedUsersPerformed(target);
                getRoleCatalogStorage().setAssignmentsUserOwner(null);
                if (getRoleCatalogStorage().getShoppingCartConfigurationDto().getViewTypeList().size() > 0) {
                    viewTypeModel.setObject(getRoleCatalogStorage().getShoppingCartConfigurationDto().getViewTypeList().get(0));
                }
                initProvider();
                searchModel.reset();

                target.add(getContentPanel());
                target.add(getHeaderPanel());
            }
        };
        sourceUserPanel.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

            public boolean isVisible(){
                return getRoleCatalogStorage().getShoppingCartConfigurationDto().isUserAssignmentsViewAllowed();
            }
        });
        sourceUserPanel.setOutputMarkupId(true);
        headerPanel.add(sourceUserPanel);
    }

    private void initViewSelector(WebMarkupContainer headerPanel){
        DropDownChoice<AssignmentViewType> viewSelect = new DropDownChoice(ID_VIEW_TYPE, viewTypeModel,
                Model.ofList(getRoleCatalogStorage().getShoppingCartConfigurationDto().getViewTypeList()),
                new EnumChoiceRenderer<AssignmentViewType>(this));
        viewSelect.add(new OnChangeAjaxBehavior() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                initProvider();
                searchModel.reset();

                target.add(getContentPanel());
                target.add(getHeaderPanel());
            }
        });
        viewSelect.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled(){
                return !AssignmentViewType.USER_TYPE.equals(viewTypeModel.getObject());
            }
        });
        viewSelect.setOutputMarkupId(true);
        headerPanel.add(viewSelect);

    }

    private void initSearchPanel(WebMarkupContainer headerPanel) {
        final Form searchForm = new Form(ID_SEARCH_FORM);
        searchForm.setOutputMarkupId(true);

        SearchPanel search = new SearchPanel(ID_SEARCH, (IModel) searchModel, false) {
            private static final long serialVersionUID = 1L;

            @Override
            public void searchPerformed(ObjectQuery query, AjaxRequestTarget target) {
                PageAssignmentShoppingKart.this.searchPerformed(query, target);
            }
        };
        searchForm.add(search);
        headerPanel.add(searchForm);
    }

    private void searchPerformed(ObjectQuery query, AjaxRequestTarget target) {
        provider.setQuery(createContentQuery(query));
        target.add(getCatalogItemsPanelContainer());
    }

    protected ObjectQuery createContentQuery(ObjectQuery searchQuery) {
        ObjectQuery memberQuery = new ObjectQuery();
        if (AssignmentViewType.ROLE_CATALOG_VIEW.equals(viewTypeModel.getObject())){
            String oid = getRoleCatalogStorage().getSelectedOid();
            if(StringUtils.isEmpty(oid)){
                return null;
            }
            addOrgMembersFilter(oid, memberQuery);
        }
        if (AssignmentViewType.USER_TYPE.equals(viewTypeModel.getObject())) {
            UserType assignmentsOwner =  getRoleCatalogStorage().getAssignmentsUserOwner();
            List<String> assignmentTargetObjectOidsList = collectTargetObjectOids(assignmentsOwner.getAssignment());
            ObjectFilter oidsFilter = InOidFilter.createInOid(assignmentTargetObjectOidsList);
            memberQuery.addFilter(oidsFilter);
        }
        memberQuery.addFilter(getAssignableRolesFilter());
        addViewTypeFilter(memberQuery);
        if (memberQuery == null) {
            memberQuery = new ObjectQuery();
        }
        if (searchQuery == null) {
            if (searchModel != null && searchModel.getObject() != null) {
                Search search = searchModel.getObject();
                searchQuery = search.createObjectQuery(getPrismContext());
            }
        }
        if (searchQuery != null && searchQuery.getFilter() != null) {
            memberQuery.addFilter(searchQuery.getFilter());
        }
        return memberQuery;
    }

    private void addViewTypeFilter(ObjectQuery query) {
        ObjectFilter prependedAndFilter = null;
        if (AssignmentViewType.ORG_TYPE.equals(viewTypeModel.getObject())){
            prependedAndFilter = ObjectQueryUtil.filterAnd(TypeFilter.createType(OrgType.COMPLEX_TYPE, null), query.getFilter());
            query.addFilter(prependedAndFilter);
        } else if (AssignmentViewType.ROLE_TYPE.equals(viewTypeModel.getObject())){
            prependedAndFilter = ObjectQueryUtil.filterAnd(TypeFilter.createType(RoleType.COMPLEX_TYPE, null), query.getFilter());
            query.addFilter(prependedAndFilter);
        } else if (AssignmentViewType.SERVICE_TYPE.equals(viewTypeModel.getObject())){
            prependedAndFilter = ObjectQueryUtil.filterAnd(TypeFilter.createType(ServiceType.COMPLEX_TYPE, null), query.getFilter());
            query.addFilter(prependedAndFilter);
        }
    }

    private ObjectFilter getAssignableRolesFilter() {
        ObjectFilter filter = null;
        LOGGER.debug("Loading roles which the current user has right to assign");
        Task task = createSimpleTask(OPERATION_LOAD_ASSIGNABLE_ROLES);
        OperationResult result = task.getResult();
        try {
            ModelInteractionService mis = getModelInteractionService();
            RoleSelectionSpecification roleSpec =
                    mis.getAssignableRoleSpecification(getTargetUser().asPrismObject(), task, result);
            filter = roleSpec.getFilter();
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load available roles", ex);
            result.recordFatalError("Couldn't load available roles", ex);
        } finally {
            result.recomputeStatus();
        }
        if (!result.isSuccess() && !result.isHandledError()) {
            showResult(result);
        }
        return filter;
    }

    private ObjectQuery addOrgMembersFilter(String oid, ObjectQuery query) {
        ObjectFilter filter = OrgFilter.createOrg(oid, OrgFilter.Scope.ONE_LEVEL);

        TypeFilter roleTypeFilter = TypeFilter.createType(RoleType.COMPLEX_TYPE, filter);
        TypeFilter serviceTypeFilter = TypeFilter.createType(ServiceType.COMPLEX_TYPE, filter);
        query.addFilter(OrFilter.createOr(roleTypeFilter, serviceTypeFilter));
        return query;

    }

    private void initCartButton(WebMarkupContainer headerPanel){
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
        headerPanel.add(cartButton);

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

    public void reloadCartButton(AjaxRequestTarget target) {
        target.add(getHeaderPanel().get(ID_CART_BUTTON));
    }

    private boolean isCatalogOidEmpty(){
        return StringUtils.isEmpty(getRoleCatalogStorage().getShoppingCartConfigurationDto().getRoleCatalogOid());
    }


    private WebMarkupContainer getCatalogItemsPanelContainer(){
        return (WebMarkupContainer)getContentPanel().get(ID_CATALOG_ITEMS_GRID_PANEL);
    }

    private WebMarkupContainer getHeaderPanel(){
        return (WebMarkupContainer)get(createComponentPath(ID_MAIN_FORM, ID_HEADER_PANEL));
    }

    private WebMarkupContainer getContentPanel(){
        return (WebMarkupContainer)get(createComponentPath(ID_MAIN_FORM, ID_CONTENT_PANEL));
    }

    private boolean isAlreadyAssigned(PrismObject<AbstractRoleType> obj, AssignmentEditorDto assignmentDto){
        UserType user = getTargetUser();
        if (user == null || user.getAssignment() == null){
            return false;
        }
        boolean isAssigned = false;
        List<RelationTypes> assignedRelationsList = new ArrayList<>();
        for (AssignmentType assignment : user.getAssignment()){
            if (assignment.getTargetRef() != null && assignment.getTargetRef().getOid().equals(obj.getOid())){
                isAssigned = true;
                assignedRelationsList.add(RelationTypes.getRelationType(assignment.getTargetRef().getRelation()));
            }
        }
        assignmentDto.setAssignedRelationsList(assignedRelationsList);
        return isAssigned;
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


    private RoleManagementConfigurationType getRoleManagementConfigurationType() {
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

    private AttributeModifier getCatalogItemsPanelClassAppender(){
        StringBuilder defaultPanleStyle = new StringBuilder("shopping-cart-item-table");
        if (AssignmentViewType.ROLE_CATALOG_VIEW.equals(viewTypeModel.getObject())) {
            return AttributeAppender.replace("class", defaultPanleStyle.append(" col-md-9"));
        } else {
            return AttributeAppender.replace("class", defaultPanleStyle.append(" col-md-12"));
        }

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

    private String getSourceUserSelectionButtonLabel(UserType user){
        if (user ==  null){
            return createStringResource("AssignmentCatalogPanel.selectSourceUser").getString();
        } else {
            return createStringResource("AssignmentCatalogPanel.sourceUserSelected", user.getName().getOrig()).getString();
        }
    }

    private List<String> collectTargetObjectOids(List<AssignmentType> assignments){
        List<String> oidsList = new ArrayList<>();
        if (assignments == null){
            return oidsList;
        }
        for (AssignmentType assignment : assignments){
            if (assignment.getTargetRef() == null || assignment.getTargetRef().getOid() == null){
                continue;
            }
            oidsList.add(assignment.getTargetRef().getOid());
        }
        return oidsList;
    }

}
