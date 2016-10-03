package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.web.component.data.*;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.search.SearchFactory;
import com.evolveum.midpoint.web.component.search.SearchPanel;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.navigation.paging.IPageable;
import org.apache.wicket.markup.html.navigation.paging.IPageableItems;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.data.DataViewBase;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by honchar
 */
public class CatalogItemsPanel extends BasePanel<String> implements IPageableItems {
    private static String ID_MULTI_BUTTON_TABLE = "multiButtonTable";
    private static final String ID_SEARCH_FORM = "searchForm";
    private static final String ID_SEARCH = "search";
    private static final String ID_PAGING_FOOTER = "pagingFooter";
    private static final String ID_PAGING = "paging";
    private static final String ID_COUNT = "count";
    private static final String ID_MENU = "menu";
    private static final String ID_FOOTER_CONTAINER = "footerContainer";
    private static final String ID_BUTTON_TOOLBAR = "buttonToolbar";
    private static final String ID_FOOTER = "footer";

    private IModel<Search> searchModel;
    private ObjectDataProvider<AssignmentEditorDto, AbstractRoleType> provider;
    private IModel<List<AssignmentEditorDto>> itemsListModel;

    private static final long ITEMS_PER_ROW = 3;
    private static final long DEFAULT_ROWS_COUNT = 5;
    private PageBase pageBase;
    private boolean providerSizeSet = false;
    private long currentPage = 0;

    public CatalogItemsPanel(String id) {
        super(id);
    }

    public CatalogItemsPanel(String id, IModel<String> oidModel, String oid, PageBase pageBase) {
        super(id, oidModel);
        this.pageBase = pageBase;
        initProvider(oid);
        initSearchModel();
        initItemListModel();
        initLayout();
    }

    private void initLayout() {
        setOutputMarkupId(true);

        initSearchPanel();
        MultiButtonTable assignmentsTable = new MultiButtonTable(ID_MULTI_BUTTON_TABLE, ITEMS_PER_ROW, itemsListModel);
        assignmentsTable.setOutputMarkupId(true);
        add(assignmentsTable);

        add(createFooter(ID_FOOTER));

    }

    protected void initProvider(final String oid) {

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

                return createContentQuery(oid, null);
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

    private void initSearchPanel() {
        final Form searchForm = new Form(ID_SEARCH_FORM);
        add(searchForm);
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
//        MultipleAssignmentSelector.this.searchQuery = query;
//        if (filterModel != null && filterModel.getObject() != null) {
//            if (query == null){
//                query = new ObjectQuery();
//            }
//            query.addFilter(filterModel.getObject());
//            filterObjectIsAdded = true;
//        }
//        BoxedTablePanel panel = getTable();
//        panel.setCurrentPage(null);
//        provider.setQuery(query);
        setCurrentPage(0);
        provider.setQuery(createContentQuery(getModelObject(), query));
        refreshItemsPanel();
        target.add(CatalogItemsPanel.this);
    }

    protected ObjectQuery createContentQuery(String oid, ObjectQuery searchQuery) {
        ObjectQuery memberQuery = createMemberQuery(oid);
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
            long from  = currentPage * ITEMS_PER_ROW * DEFAULT_ROWS_COUNT;
            provider.internalIterator(from, ITEMS_PER_ROW * DEFAULT_ROWS_COUNT);
        }
        MultiButtonTable assignmentsTable = new MultiButtonTable(ID_MULTI_BUTTON_TABLE, ITEMS_PER_ROW, itemsListModel);
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
        long from  = page * ITEMS_PER_ROW * DEFAULT_ROWS_COUNT;
        if (provider.getAvailableData() != null){
            provider.getAvailableData().clear();
        }
        provider.internalIterator(from, ITEMS_PER_ROW * DEFAULT_ROWS_COUNT);
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
        return DEFAULT_ROWS_COUNT * ITEMS_PER_ROW;
    }

    @Override
    public long getItemCount() {
        return 0l;
    }

}
