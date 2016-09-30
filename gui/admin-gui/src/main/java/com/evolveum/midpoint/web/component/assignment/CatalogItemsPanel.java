package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.web.component.data.MultiButtonTable;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.search.SearchFactory;
import com.evolveum.midpoint.web.component.search.SearchPanel;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ServiceType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by honchar
 */
public class CatalogItemsPanel extends BasePanel<String> {
    private static String ID_MULTI_BUTTON_TABLE = "multiButtonTable";
    private static final String ID_SEARCH_FORM = "searchForm";
    private static final String ID_SEARCH = "search";

    private IModel<Search> searchModel;
    private ObjectDataProvider<AssignmentEditorDto, FocusType> provider;

    private static final int ITEMS_PER_ROW = 3;
    private PageBase pageBase;

    public CatalogItemsPanel(String id){
        super(id);
    }

    public CatalogItemsPanel(String id, IModel<String> oidModel, String oid, PageBase pageBase){
        super(id, oidModel);
        this.pageBase = pageBase;
        initProvider(oid);
        initSearchModel();
        initLayout();
    }

    private void initLayout(){
        initSearchPanel();
        MultiButtonTable assignmentsTable = new MultiButtonTable(ID_MULTI_BUTTON_TABLE, ITEMS_PER_ROW, new IModel<List<AssignmentEditorDto>>() {
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
        });
        assignmentsTable.setOutputMarkupId(true);
        add(assignmentsTable);
    }

    protected void initProvider(final String oid) {

        provider = new ObjectDataProvider<AssignmentEditorDto, FocusType>(pageBase, FocusType.class) {
            private static final long serialVersionUID = 1L;

            @Override
            public AssignmentEditorDto createDataObjectWrapper(PrismObject<FocusType> obj) {
                return AssignmentEditorDto.createDtoFromObject(obj.asObjectable(), UserDtoStatus.MODIFY, pageBase);
            }

            @Override
            public void setQuery(ObjectQuery query) {
                super.setQuery(query);
            }

            @Override
            public ObjectQuery getQuery() {

                return createContentQuery(oid);
            }
        };
        provider.setQuery(createContentQuery(oid));
        if (provider != null) {
            long s = provider.size();
            provider.internalIterator(0, s);
        }
    }

    protected void refreshCatalogItemsPanel(){

    }

    private void initSearchModel(){
        searchModel = new LoadableModel<Search>(false) {
            @Override
            public Search load() {
                Search search =  SearchFactory.createSearch(FocusType.class, getPageBase().getPrismContext(),
                        getPageBase().getModelInteractionService());
                return search;
            }
        };
    }

    private void initSearchPanel(){
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
//        target.add(panel);
    }

    protected ObjectQuery createContentQuery(String oid) {
//        Search search = searchModel.getObject();
//        ObjectQuery query = search.createObjectQuery(parentPage.getPrismContext());
//        query = addFilterToContentQuery(query);
//        return query;
        return createMemberQuery(oid);
    }

    private ObjectQuery createMemberQuery(String oid) {
        ObjectFilter filter = OrgFilter.createOrg(oid, OrgFilter.Scope.ONE_LEVEL);

        TypeFilter roleTypeFilter = TypeFilter.createType(RoleType.COMPLEX_TYPE, filter);
        TypeFilter orgTypeFilter = TypeFilter.createType(OrgType.COMPLEX_TYPE, filter);
        TypeFilter serviceTypeFilter = TypeFilter.createType(ServiceType.COMPLEX_TYPE, filter);
        ObjectQuery query = ObjectQuery.createObjectQuery(OrFilter.createOr(roleTypeFilter, orgTypeFilter, serviceTypeFilter));
        return query;

    }


}
