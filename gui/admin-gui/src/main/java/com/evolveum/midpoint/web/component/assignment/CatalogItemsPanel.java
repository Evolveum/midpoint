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
import com.evolveum.midpoint.web.component.data.*;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.search.SearchFactory;
import com.evolveum.midpoint.web.component.search.SearchPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.web.page.self.PageAssignmentDetails;
import com.evolveum.midpoint.web.page.self.PageAssignmentsList;
import com.evolveum.midpoint.web.page.self.dto.AssignmentViewType;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.web.session.SessionStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.navigation.paging.IPageable;
import org.apache.wicket.markup.html.navigation.paging.IPageableItems;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.data.DataViewBase;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.util.ListModel;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by honchar
 */
public class CatalogItemsPanel extends BasePanel implements IPageableItems {
	private static final long serialVersionUID = 1L;

	private static final String ID_MULTI_BUTTON_TABLE = "multiButtonTable";
    private static final String ID_SEARCH_FORM = "searchForm";
    private static final String ID_SEARCH = "search";
    private static final String ID_PAGING_FOOTER = "pagingFooter";
    private static final String ID_PAGING = "paging";
    private static final String ID_COUNT = "count";
    private static final String ID_MENU = "menu";
    private static final String ID_FOOTER_CONTAINER = "footerContainer";
    private static final String ID_BUTTON_TOOLBAR = "buttonToolbar";
    private static final String ID_FOOTER = "footer";
    private static final String ID_CART_BUTTON = "cartButton";
    private static final String ID_CART_ITEMS_COUNT = "itemsCount";
    private static final String ID_HEADER_PANEL = "headerPanel";
    private static final String ID_VIEW_TYPE = "viewTypeSelect";

    private static final String DOT_CLASS = CatalogItemsPanel.class.getName();
    private static final Trace LOGGER = TraceManager.getTrace(CatalogItemsPanel.class);
    private static final String OPERATION_LOAD_ASSIGNABLE_ROLES = DOT_CLASS + "loadAssignableRoles";
    private static final String OPERATION_LOAD_USER = DOT_CLASS + "loadUser";

    private IModel<Search> searchModel;
    private ObjectDataProvider<AssignmentEditorDto, AbstractRoleType> provider;
    private IModel<List<AssignmentEditorDto>> itemsListModel;
    private IModel<AssignmentViewType> viewModel;
    private AssignmentViewType currentViewType = AssignmentViewType.ROLE_CATALOG_VIEW;

    private long itemsPerRow = 4;
    private static final long DEFAULT_ROWS_COUNT = 5;
    private PageBase pageBase;
    private IModel<QName> viewTypeClassModel;
    private IModel<String> catalogOidModel;
    private long currentPage = 0;

    public CatalogItemsPanel(String id) {
        super(id);
    }

    public CatalogItemsPanel(String id, IModel<String> catalogOidModel, IModel<QName> viewTypeClassModel, PageBase pageBase) {
        this(id, catalogOidModel, viewTypeClassModel, pageBase, 0);
    }

    public CatalogItemsPanel(String id, IModel<String> catalogOidModel, IModel<QName> viewTypeClassModel, PageBase pageBase, int itemsPerRow) {
        super(id);
        this.pageBase = pageBase;
        this.viewTypeClassModel = viewTypeClassModel;
        this.catalogOidModel = catalogOidModel;
        setCurrentViewType(viewTypeClassModel.getObject());

        if (itemsPerRow > 0){
            this.itemsPerRow = itemsPerRow;
        }
        viewModel = new IModel<AssignmentViewType>() {
            @Override
            public AssignmentViewType getObject() {
                return currentViewType;
            }

            @Override
            public void setObject(AssignmentViewType assignmentViewType) {
                currentViewType = assignmentViewType;
            }

            @Override
            public void detach() {

            }
        };
        initProvider();
        initSearchModel();
        initItemListModel();
        initLayout();
    }

    private void initLayout() {
        setOutputMarkupId(true);

        WebMarkupContainer headerPanel = new WebMarkupContainer(ID_HEADER_PANEL);
        headerPanel.setOutputMarkupId(true);
        add(headerPanel);

        initViewSelector(headerPanel);
        initCartButton(headerPanel);
        initSearchPanel(headerPanel);

        MultiButtonTable assignmentsTable = new MultiButtonTable(ID_MULTI_BUTTON_TABLE, itemsPerRow, itemsListModel, pageBase);
        assignmentsTable.setOutputMarkupId(true);
        add(assignmentsTable);

        add(createFooter(ID_FOOTER));

    }

    protected void initProvider() {

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
        setCurrentPage(0);
    }

    protected void refreshCatalogItemsPanel() {

    }

    private void initSearchModel() {
        searchModel = new LoadableModel<Search>(false) {
            @Override
            public Search load() {
                Search search = SearchFactory.createSearch(AbstractRoleType.class, getPageBase().getPrismContext(),
                        getPageBase().getModelInteractionService());
                return search;
            }
        };
    }

    private void initItemListModel() {
        itemsListModel = new IModel<List<AssignmentEditorDto>>() {
            @Override
            public List<AssignmentEditorDto> getObject() {
                return provider != null ? provider.getAvailableData() : new ArrayList<AssignmentEditorDto>();
            }

            @Override
            public void setObject(List<AssignmentEditorDto> assignmentTypeList) {

            }

            @Override
            public void detach() {

            }
        };
    }

    private void initViewSelector(WebMarkupContainer headerPanel){
        DropDownChoice<AssignmentViewType> viewSelect = new DropDownChoice(ID_VIEW_TYPE, viewModel, new ListModel(createAssignableTypesList()),
                new EnumChoiceRenderer<AssignmentViewType>(this));
        viewSelect.add(new OnChangeAjaxBehavior() {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                viewTypeClassModel.setObject(getViewTypeClass());
                AssignmentCatalogPanel parentPanel = CatalogItemsPanel.this.findParent(AssignmentCatalogPanel.class);
                parentPanel.addOrReplaceLayout();
                target.add(parentPanel);
            }
        });
        viewSelect.setOutputMarkupId(true);
        headerPanel.add(viewSelect);

    }

    private void initSearchPanel(WebMarkupContainer headerPanel) {
        final Form searchForm = new Form(ID_SEARCH_FORM);
        headerPanel.add(searchForm);
        searchForm.setOutputMarkupId(true);

        SearchPanel search = new SearchPanel(ID_SEARCH, (IModel) searchModel, false) {

            @Override
            public void searchPerformed(ObjectQuery query, AjaxRequestTarget target) {
                CatalogItemsPanel.this.searchPerformed(query, target);
            }
        };
        searchForm.add(search);

    }

    private void searchPerformed(ObjectQuery query, AjaxRequestTarget target) {
        setCurrentPage(0);
        provider.setQuery(createContentQuery(query));
        refreshItemsPanel();
        target.add(CatalogItemsPanel.this);
    }

    protected ObjectQuery createContentQuery(ObjectQuery searchQuery) {
        ObjectQuery memberQuery;
        if (viewTypeClassModel.getObject() == null){
            memberQuery = createMemberQuery(catalogOidModel.getObject());
        } else {
            memberQuery = createMemberQuery(viewTypeClassModel.getObject());
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

    private void refreshItemsPanel() {
        if (provider != null) {
            if (provider.getAvailableData() != null){
                provider.getAvailableData().clear();
            }
            long from  = currentPage * itemsPerRow * DEFAULT_ROWS_COUNT;
            provider.internalIterator(from, itemsPerRow * DEFAULT_ROWS_COUNT);
        }
        MultiButtonTable assignmentsTable = new MultiButtonTable(ID_MULTI_BUTTON_TABLE, itemsPerRow, itemsListModel, pageBase);
        assignmentsTable.setOutputMarkupId(true);
        replace(assignmentsTable);
    }

    private MultiButtonTable getMultiButtonTable() {
        return (MultiButtonTable) get(ID_MULTI_BUTTON_TABLE);
    }

    protected WebMarkupContainer createFooter(String footerId) {
        return new PagingFooter(footerId, ID_PAGING_FOOTER, CatalogItemsPanel.this);
    }

//    @Override
//    public void setCurrentPage(ObjectPaging paging) {
////        WebComponentUtil.setCurrentPage(this, paging);
//    }
//
//    @Override
//    public void setCurrentPage(long page) {
//        getDataTable().setCurrentPage(page);
//    }

    private static class PagingFooter extends Fragment {

        public PagingFooter(String id, String markupId, CatalogItemsPanel markupProvider) {
            super(id, markupId, markupProvider);
            setOutputMarkupId(true);

            initLayout(markupProvider);
        }

        private void initLayout(final CatalogItemsPanel catalogItemsPanel) {
            WebMarkupContainer footerContainer = new WebMarkupContainer(ID_FOOTER_CONTAINER);
            footerContainer.setOutputMarkupId(true);

            final Label count = new Label(ID_COUNT, new AbstractReadOnlyModel<String>() {

                @Override
                public String getObject() {
                    return "";
                }
            });
            count.setOutputMarkupId(true);
            footerContainer.add(count);

            BoxedPagingPanel nb2 = new BoxedPagingPanel(ID_PAGING, catalogItemsPanel, true) {

                @Override
                protected void onPageChanged(AjaxRequestTarget target, long page) {
                    CatalogItemsPanel catalogPanel = PagingFooter.this.findParent(CatalogItemsPanel.class);
                    catalogPanel.refreshItemsPanel();
                    target.add(catalogPanel);
                    target.add(count);
                }
            };
            footerContainer.add(nb2);

            add(footerContainer);
        }

        public Component getFooterMenu() {
            return get(ID_FOOTER_CONTAINER).get(ID_MENU);
        }

        public Component getFooterCountLabel() {
            return get(ID_FOOTER_CONTAINER).get(ID_COUNT);
        }

        public Component getFooterPaging() {
            return get(ID_FOOTER_CONTAINER).get(ID_PAGING);
        }

        private String createCountString(IPageable pageable) {
            long from = 0;
            long to = 0;
            long count = 0;

            if (pageable instanceof DataViewBase) {
                DataViewBase view = (DataViewBase) pageable;

                from = view.getFirstItemOffset() + 1;
                to = from + view.getItemsPerPage() - 1;
                long itemCount = view.getItemCount();
                if (to > itemCount) {
                    to = itemCount;
                }
                count = itemCount;
            } else if (pageable instanceof DataTable) {
                DataTable table = (DataTable) pageable;

                from = table.getCurrentPage() * table.getItemsPerPage() + 1;
                to = from + table.getItemsPerPage() - 1;
                long itemCount = table.getItemCount();
                if (to > itemCount) {
                    to = itemCount;
                }
                count = itemCount;
            }

            if (count > 0) {
                if (count == Integer.MAX_VALUE) {
                    return PageBase.createStringResourceStatic(PagingFooter.this, "CountToolbar.label.unknownCount",
                            new Object[]{from, to}).getString();
                }

                return PageBase.createStringResourceStatic(PagingFooter.this, "CountToolbar.label",
                        new Object[]{from, to, count}).getString();
            }

            return PageBase
                    .createStringResourceStatic(PagingFooter.this, "CountToolbar.noFound", new Object[]{})
                    .getString();
        }
    }

    @Override
    public void setCurrentPage(long page) {
        currentPage = page;
        long from  = page * itemsPerRow * DEFAULT_ROWS_COUNT;
        if (provider.getAvailableData() != null){
            provider.getAvailableData().clear();
        }
        provider.internalIterator(from, itemsPerRow * DEFAULT_ROWS_COUNT);
    }

    @Override
    public void setItemsPerPage(long page) {
    }

    @Override
    public long getCurrentPage() {
        return currentPage;
    }

    @Override
    public long getPageCount() {
        if (provider != null){
            long itemsPerPage = getItemsPerPage();
            return itemsPerPage != 0 ? (provider.size() % itemsPerPage == 0 ? (provider.size() / itemsPerPage) :
                    (provider.size() / itemsPerPage + 1)) : 0;
        }
        return 0;
    }

    @Override
    public long getItemsPerPage() {
        return DEFAULT_ROWS_COUNT * itemsPerRow;
    }

    @Override
    public long getItemCount() {
        return 0l;
    }

    private void initCartButton(WebMarkupContainer headerPanel){
        AjaxButton cartButton = new AjaxButton(ID_CART_BUTTON) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                setResponsePage(new PageAssignmentsList(loadUser()));
            }
        };
        cartButton.setOutputMarkupId(true);
        headerPanel.add(cartButton);

        Label cartItemsCount = new Label(ID_CART_ITEMS_COUNT, new IModel<String>() {
            @Override
            public String getObject() {
                SessionStorage storage = pageBase.getSessionStorage();
                return Integer.toString(storage.getUsers().getAssignmentShoppingCart().size());
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
                if (storage.getUsers().getAssignmentShoppingCart().size() == 0) {
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
            user = WebModelServiceUtils.loadObject(UserType.class, userOid, null, (PageBase) getPage(),
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
            currentViewType = AssignmentViewType.ORG_TYPE ;
        } else if (RoleType.COMPLEX_TYPE.equals(viewTypeClass)) {
            currentViewType = AssignmentViewType.ROLE_TYPE ;
        } else if (ServiceType.COMPLEX_TYPE.equals(viewTypeClass)) {
            currentViewType = AssignmentViewType.SERVICE_TYPE ;
        } else {
            currentViewType = AssignmentViewType.ROLE_CATALOG_VIEW ;
        }
    }

    private QName getViewTypeClass() {
        if (AssignmentViewType.ORG_TYPE.equals(currentViewType)) {
            return OrgType.COMPLEX_TYPE;
        } else if (AssignmentViewType.ROLE_TYPE.equals(currentViewType)) {
            return RoleType.COMPLEX_TYPE;
        } else if (AssignmentViewType.SERVICE_TYPE.equals(currentViewType)) {
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

}
