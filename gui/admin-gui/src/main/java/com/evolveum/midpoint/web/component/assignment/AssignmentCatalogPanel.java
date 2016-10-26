/*
 * Copyright (c) 2016 Evolveum
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
package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.RoleSelectionSpecification;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.search.SearchFactory;
import com.evolveum.midpoint.web.component.search.SearchPanel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.orgs.OrgTreePanel;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.web.page.self.PageAssignmentsList;
import com.evolveum.midpoint.web.page.self.dto.AssignmentViewType;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.web.session.SessionStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.util.ListModel;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by honchar.
 */
public class AssignmentCatalogPanel<F extends AbstractRoleType> extends BasePanel {
	private static final long serialVersionUID = 1L;

	private static String ID_TREE_PANEL_CONTAINER = "treePanelContainer";
    private static String ID_TREE_PANEL = "treePanel";
    private static String ID_CATALOG_ITEMS_PANEL_CONTAINER = "catalogItemsPanelContainer";
    private static String ID_CATALOG_ITEMS_PANEL = "catalogItemsPanel";
    private static final String ID_CART_BUTTON = "cartButton";
    private static final String ID_CART_ITEMS_COUNT = "itemsCount";
    private static final String ID_HEADER_PANEL = "headerPanel";
    private static final String ID_VIEW_TYPE = "viewTypeSelect";
    private static final String ID_SEARCH_FORM = "searchForm";
    private static final String ID_SEARCH = "search";
    private static final String ID_PANEL_CONTAINER = "catalogItemsPanelContainer";

    private static final String DOT_CLASS = AssignmentCatalogPanel.class.getName();
    private static final Trace LOGGER = TraceManager.getTrace(AssignmentCatalogPanel.class);
    private static final String OPERATION_LOAD_ASSIGNABLE_ROLES = DOT_CLASS + "loadAssignableRoles";
    private static final String OPERATION_LOAD_USER = DOT_CLASS + "loadUser";

    private PageBase pageBase;
    private IModel<String> selectedTreeItemOidModel;
    private String rootOid;
    private String selectedOid;
    private IModel<Search> searchModel;
    private IModel<AssignmentViewType> viewModel;
    private ObjectDataProvider<AssignmentEditorDto, AbstractRoleType> provider;
    private int itemsPerRow = 4;

    public AssignmentCatalogPanel(String id) {
        super(id);
    }

    public AssignmentCatalogPanel(String id, String rootOid, PageBase pageBase) {
        super(id);
        this.pageBase = pageBase;
        this.rootOid = rootOid;
        AssignmentViewType.saveViewTypeToSession(pageBase, AssignmentViewType.ROLE_CATALOG_VIEW);
        selectedOid = rootOid;
        initLayout();
    }

    public AssignmentCatalogPanel(String id, PageBase pageBase) {
        this(id, AssignmentViewType.getViewTypeFromSession(pageBase), pageBase);
    }

     public AssignmentCatalogPanel(String id, AssignmentViewType viewType, PageBase pageBase) {
        super(id);
        this.pageBase = pageBase;
        AssignmentViewType.saveViewTypeToSession(pageBase, viewType);
         initLayout();
    }

    protected void initProvider() {
        if (isCatalogOidEmpty()){
            provider = null;
        } else {
            provider = new ObjectDataProvider<AssignmentEditorDto, AbstractRoleType>(pageBase, AbstractRoleType.class) {
                private static final long serialVersionUID = 1L;

                @Override
                public AssignmentEditorDto createDataObjectWrapper(PrismObject<AbstractRoleType> obj) {
                    return AssignmentEditorDto.createDtoFromObject(obj.asObjectable(), UserDtoStatus.MODIFY, pageBase);
                }

                @Override
                public void setQuery(ObjectQuery query) {

                    super.setQuery(query);
                }

                @Override
                public ObjectQuery getQuery() {

                    return createContentQuery(null);
                }
            };
        }
    }

    private void initLayout() {
        initModels();
        initProvider();
        setOutputMarkupId(true);
        initHeaderPanel();

        WebMarkupContainer panelContainer = new WebMarkupContainer(ID_CATALOG_ITEMS_PANEL_CONTAINER);
        panelContainer.setOutputMarkupId(true);
        add(panelContainer);

        addOrReplaceLayout(panelContainer);
    }

    private void initHeaderPanel(){
        WebMarkupContainer headerPanel = new WebMarkupContainer(ID_HEADER_PANEL);
        headerPanel.setOutputMarkupId(true);
        add(headerPanel);

        initViewSelector(headerPanel);
        initCartButton(headerPanel);
        initSearchPanel(headerPanel);
    }
    public void addOrReplaceLayout(WebMarkupContainer panelContainer){
        WebMarkupContainer treePanelContainer = new WebMarkupContainer(ID_TREE_PANEL_CONTAINER);
        treePanelContainer.setOutputMarkupId(true);
        panelContainer.addOrReplace(treePanelContainer);
        if (AssignmentViewType.ROLE_CATALOG_VIEW.equals(AssignmentViewType.getViewTypeFromSession(pageBase)) && StringUtils.isNotEmpty(rootOid)) {
            OrgTreePanel treePanel = new OrgTreePanel(ID_TREE_PANEL, new IModel<String>() {
                @Override
                public String getObject() {
                    return rootOid;
                }

                @Override
                public void setObject(String s) {

                }

                @Override
                public void detach() {

                }
            }, false, "AssignmentShoppingCartPanel.treeTitle") {
                private static final long serialVersionUID = 1L;

                @Override
                protected void selectTreeItemPerformed(SelectableBean<OrgType> selected,
                                                       AjaxRequestTarget target) {
                    AssignmentCatalogPanel.this.selectTreeItemPerformed(selected, target);
                }

                protected List<InlineMenuItem> createTreeMenu() {
                    return new ArrayList<>();
                }

                @Override
                protected List<InlineMenuItem> createTreeChildrenMenu() {
                    return new ArrayList<>();
                }

            };
            treePanel.setOutputMarkupId(true);
            treePanelContainer.add(new AttributeAppender("class", "col-md-3"));
            treePanelContainer.addOrReplace(treePanel);
        } else {
            WebMarkupContainer treePanel = new WebMarkupContainer(ID_TREE_PANEL);
            treePanel.setVisible(false);
            treePanel.setOutputMarkupId(true);
            treePanelContainer.addOrReplace(treePanel);
        }

        WebMarkupContainer catalogItemsPanelContainer = new WebMarkupContainer(ID_CATALOG_ITEMS_PANEL_CONTAINER);
        catalogItemsPanelContainer.setOutputMarkupId(true);
        panelContainer.addOrReplace(catalogItemsPanelContainer);

        CatalogItemsPanel catalogItemsPanel = new CatalogItemsPanel(ID_CATALOG_ITEMS_PANEL, selectedTreeItemOidModel,
                pageBase, itemsPerRow, provider);
        if (AssignmentViewType.ROLE_CATALOG_VIEW.equals(AssignmentViewType.getViewTypeFromSession(pageBase))) {
            catalogItemsPanelContainer.add(new AttributeAppender("class", "col-md-9"));
        } else {
            catalogItemsPanelContainer.add(new AttributeAppender("class", "col-md-12"));
        }
        catalogItemsPanel.setOutputMarkupId(true);
        catalogItemsPanelContainer.addOrReplace(catalogItemsPanel);
    }

    private void selectTreeItemPerformed(SelectableBean<OrgType> selected, AjaxRequestTarget target) {
        final OrgType selectedOgr = selected.getValue();
        selectedTreeItemOidModel.setObject(selectedOgr.getOid());
        AssignmentViewType.saveViewTypeToSession(pageBase, AssignmentViewType.ROLE_CATALOG_VIEW);
        AssignmentCatalogPanel.this.addOrReplaceLayout(getCatalogItemsPanelContainer());
        target.add(getCatalogItemsPanelContainer());

    }

    private void initModels(){
        selectedTreeItemOidModel = new IModel<String>() {
            @Override
            public String getObject() {
                return StringUtils.isEmpty(selectedOid) ? rootOid : selectedOid;
            }

            @Override
            public void setObject(String s) {
                selectedOid = s;
            }

            @Override
            public void detach() {

            }
        };
        viewModel = new IModel<AssignmentViewType>() {
            @Override
            public AssignmentViewType getObject() {
                return AssignmentViewType.getViewTypeFromSession(pageBase);
            }

            @Override
            public void setObject(AssignmentViewType assignmentViewType) {
                AssignmentViewType.saveViewTypeToSession(pageBase, assignmentViewType);
            }

            @Override
            public void detach() {

            }
        };

        searchModel = new LoadableModel<Search>(false) {
            @Override
            public Search load() {
                Search search = SearchFactory.createSearch(AbstractRoleType.class, pageBase.getPrismContext(),
                        pageBase.getModelInteractionService());
                return search;
            }
        };
    }

    public String getRootOid() {
        return rootOid;
    }

    public void setRootOid(String rootOid) {
        this.rootOid = rootOid;
    }


    private void initViewSelector(WebMarkupContainer headerPanel){
        DropDownChoice<AssignmentViewType> viewSelect = new DropDownChoice(ID_VIEW_TYPE, viewModel, new ListModel(createAssignableTypesList()),
                new EnumChoiceRenderer<AssignmentViewType>(this));
        viewSelect.add(new OnChangeAjaxBehavior() {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                AssignmentCatalogPanel.this.addOrReplaceLayout(getCatalogItemsPanelContainer());
                target.add(getCatalogItemsPanelContainer());
            }
        });
        viewSelect.setOutputMarkupId(true);
        headerPanel.add(viewSelect);

    }

    private WebMarkupContainer getCatalogItemsPanelContainer(){
        return (WebMarkupContainer)get(ID_CATALOG_ITEMS_PANEL_CONTAINER);
    }
    private void initSearchPanel(WebMarkupContainer headerPanel) {
        final Form searchForm = new Form(ID_SEARCH_FORM);
        headerPanel.add(searchForm);
        searchForm.add(new VisibleEnableBehaviour() {
            public boolean isVisible() {
                return !isCatalogOidEmpty();
            }
        });
        searchForm.setOutputMarkupId(true);

        SearchPanel search = new SearchPanel(ID_SEARCH, (IModel) searchModel, false) {

            @Override
            public void searchPerformed(ObjectQuery query, AjaxRequestTarget target) {
                AssignmentCatalogPanel.this.searchPerformed(query, target);
            }
        };
        searchForm.add(search);

    }

    private void searchPerformed(ObjectQuery query, AjaxRequestTarget target) {
//        setCurrentPage(0);
        provider.setQuery(createContentQuery(query));
        AssignmentCatalogPanel.this.addOrReplaceLayout(getCatalogItemsPanelContainer());
        target.add(getCatalogItemsPanelContainer());
    }

    protected ObjectQuery createContentQuery(ObjectQuery searchQuery) {
        ObjectQuery memberQuery;
        if (AssignmentViewType.ROLE_CATALOG_VIEW.equals(AssignmentViewType.getViewTypeFromSession(pageBase))){
            memberQuery = createMemberQuery(selectedTreeItemOidModel.getObject());
        } else {
            memberQuery = createMemberQuery(getViewTypeClass(AssignmentViewType.getViewTypeFromSession(pageBase)));
        }
        if (memberQuery == null) {
            memberQuery = new ObjectQuery();
        }
        if (searchQuery == null) {
            if (searchModel != null && searchModel.getObject() != null) {
                Search search = searchModel.getObject();
                searchQuery = search.createObjectQuery(pageBase.getPrismContext());
            }
        }
        if (searchQuery != null && searchQuery.getFilter() != null) {
            memberQuery.addFilter(searchQuery.getFilter());
        }
        return memberQuery;
    }

    private ObjectQuery createMemberQuery(QName focusTypeClass) {
        ObjectQuery query = new ObjectQuery();
        ObjectFilter filter = null;
        if (focusTypeClass.equals(RoleType.COMPLEX_TYPE)) {
            LOGGER.debug("Loading roles which the current user has right to assign");
            OperationResult result = new OperationResult(OPERATION_LOAD_ASSIGNABLE_ROLES);
            try {
                ModelInteractionService mis = pageBase.getModelInteractionService();
                RoleSelectionSpecification roleSpec =
                        mis.getAssignableRoleSpecification(SecurityUtils.getPrincipalUser().getUser().asPrismObject(), result);
                filter = roleSpec.getFilter();
            } catch (Exception ex) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load available roles", ex);
                result.recordFatalError("Couldn't load available roles", ex);
            } finally {
                result.recomputeStatus();
            }
            if (!result.isSuccess() && !result.isHandledError()) {
                pageBase.showResult(result);
            }
        }
        query.addFilter(TypeFilter.createType(focusTypeClass, filter));
        return query;

    }

    private ObjectQuery createMemberQuery(String oid) {
        ObjectFilter filter = OrgFilter.createOrg(oid, OrgFilter.Scope.ONE_LEVEL);

        TypeFilter roleTypeFilter = TypeFilter.createType(RoleType.COMPLEX_TYPE, filter);
        TypeFilter orgTypeFilter = TypeFilter.createType(OrgType.COMPLEX_TYPE, filter);
        TypeFilter serviceTypeFilter = TypeFilter.createType(ServiceType.COMPLEX_TYPE, filter);
        ObjectQuery query = ObjectQuery.createObjectQuery(OrFilter.createOr(roleTypeFilter, orgTypeFilter, serviceTypeFilter));
        return query;

    }

    private void initCartButton(WebMarkupContainer headerPanel){
        AjaxButton cartButton = new AjaxButton(ID_CART_BUTTON) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                setResponsePage(new PageAssignmentsList(loadUser()));
            }
        };
        cartButton.add(new VisibleEnableBehaviour(){
            public boolean isVisible(){
                return !isCatalogOidEmpty();
            }
        });
        cartButton.setOutputMarkupId(true);
        headerPanel.add(cartButton);

        Label cartItemsCount = new Label(ID_CART_ITEMS_COUNT, new IModel<String>() {
            @Override
            public String getObject() {
                SessionStorage storage = pageBase.getSessionStorage();
                return Integer.toString(storage.getRoleCatalog().getAssignmentShoppingCart().size());
            }

            @Override
            public void setObject(String s) {


            }

            @Override
            public void detach() {

            }
        });
        cartItemsCount.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                SessionStorage storage = pageBase.getSessionStorage();
                if (storage.getRoleCatalog().getAssignmentShoppingCart().size() == 0) {
                    return false;
                } else {
                    return true;
                }
            }
        });
        cartItemsCount.setOutputMarkupId(true);
        cartButton.add(cartItemsCount);
    }

    public void reloadCartButton(AjaxRequestTarget target) {
        target.add(get(ID_HEADER_PANEL).get(ID_CART_BUTTON));
    }

    private PrismObject<UserType> loadUser() {
        LOGGER.debug("Loading user and accounts.");
        OperationResult result = new OperationResult(OPERATION_LOAD_USER);
        PrismObject<UserType> user = null;
        try {
            String userOid = SecurityUtils.getPrincipalUser().getOid();
            Task task = pageBase.createSimpleTask(OPERATION_LOAD_USER);
            user = WebModelServiceUtils.loadObject(UserType.class, userOid, null, pageBase,
                    task, result);
            result.recordSuccessIfUnknown();

            result.recordSuccessIfUnknown();
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load accounts", ex);
            result.recordFatalError("Couldn't load accounts", ex);
        } finally {
            result.recomputeStatus();
        }
        if (!result.isSuccess() && !result.isHandledError()) {
            pageBase.showResult(result);
        }
        return user;
    }

    private void setCurrentViewType(QName viewTypeClass){
        if (OrgType.COMPLEX_TYPE.equals(viewTypeClass)) {
            AssignmentViewType.saveViewTypeToSession(pageBase, AssignmentViewType.ORG_TYPE) ;
        } else if (RoleType.COMPLEX_TYPE.equals(viewTypeClass)) {
            AssignmentViewType.saveViewTypeToSession(pageBase, AssignmentViewType.ROLE_TYPE) ;
        } else if (ServiceType.COMPLEX_TYPE.equals(viewTypeClass)) {
            AssignmentViewType.saveViewTypeToSession(pageBase, AssignmentViewType.SERVICE_TYPE) ;
        } else {
            AssignmentViewType.saveViewTypeToSession(pageBase, AssignmentViewType.ROLE_CATALOG_VIEW) ;
        }
    }

    private QName getViewTypeClass(AssignmentViewType viewType) {
        if (AssignmentViewType.ORG_TYPE.equals(viewType)) {
            return OrgType.COMPLEX_TYPE;
        } else if (AssignmentViewType.ROLE_TYPE.equals(viewType)) {
            return RoleType.COMPLEX_TYPE;
        } else if (AssignmentViewType.SERVICE_TYPE.equals(viewType)) {
            return ServiceType.COMPLEX_TYPE;
        }
        return null;
    }

    public static List<AssignmentViewType> createAssignableTypesList() {
        List<AssignmentViewType> focusTypeList = new ArrayList<>();

        focusTypeList.add(AssignmentViewType.ROLE_CATALOG_VIEW);
        focusTypeList.add(AssignmentViewType.ORG_TYPE);
        focusTypeList.add(AssignmentViewType.ROLE_TYPE);
        focusTypeList.add(AssignmentViewType.SERVICE_TYPE);

        return focusTypeList;
    }

    private boolean isCatalogOidEmpty(){
        return AssignmentViewType.ROLE_CATALOG_VIEW.equals(AssignmentViewType.getViewTypeFromSession(pageBase)) &&
                (selectedTreeItemOidModel == null || StringUtils.isEmpty(selectedTreeItemOidModel.getObject()));
    }

}

