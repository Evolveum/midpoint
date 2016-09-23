package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.component.data.MultiButtonTable;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.orgs.OrgTreePanel;
import com.evolveum.midpoint.web.page.admin.users.component.TreeTablePanel;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.IModel;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Kate on 20.09.2016.
 */
public class AssignmentShoppingCartPanel<F extends FocusType> extends BasePanel<String> {
    private static String ID_TREE_PANEL = "treePanel";
    private static String ID_ASSIGNMENTS_PANEL = "assignmentsPanel";

    public AssignmentShoppingCartPanel(String id) {
        super(id);
    }

    public AssignmentShoppingCartPanel(String id, IModel<String> rootOidModel) {
        super(id, rootOidModel);
        initLayout();
    }

    private void initLayout() {
        setOutputMarkupId(true);
        OrgTreePanel treePanel = new OrgTreePanel(ID_TREE_PANEL, getModel(), false) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void selectTreeItemPerformed(SelectableBean<OrgType> selected,
                                                   AjaxRequestTarget target) {
                AssignmentShoppingCartPanel.this.selectTreeItemPerformed(selected, target);
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
        add(treePanel);


        WebMarkupContainer initAssignmentsPanel = new WebMarkupContainer(ID_ASSIGNMENTS_PANEL);
        initAssignmentsPanel.setOutputMarkupId(true);
        add(initAssignmentsPanel);

    }

    private void selectTreeItemPerformed(SelectableBean<OrgType> selected, AjaxRequestTarget target){
        final OrgType selectedOgr = selected.getValue();
        final ObjectDataProvider<AssignmentEditorDto, FocusType> provider = initProvider(selectedOgr.getOid());
if (provider != null){
    long s = provider.size();
    provider.internalIterator(0, s);
}
        MultiButtonTable assignmentsTable = new MultiButtonTable(ID_ASSIGNMENTS_PANEL, 3, new IModel<List<AssignmentEditorDto>>() {
            @Override
            public List<AssignmentEditorDto> getObject() {
                return provider.getAvailableData();
            }

            @Override
            public void setObject(List<AssignmentEditorDto> assignmentTypeList) {

            }

            @Override
            public void detach() {

            }
        });
        assignmentsTable.setOutputMarkupId(true);
        replace(assignmentsTable);
        target.add(this);
    }

    protected ObjectDataProvider<AssignmentEditorDto, FocusType> initProvider(final String oid) {

        ObjectDataProvider<AssignmentEditorDto, FocusType> provider = new ObjectDataProvider<AssignmentEditorDto, FocusType>(getPageBase(), FocusType.class) {
            private static final long serialVersionUID = 1L;

            @Override
            public AssignmentEditorDto createDataObjectWrapper(PrismObject<FocusType> obj) {
                return AssignmentEditorDto.createDtoFromObject(obj.asObjectable(), UserDtoStatus.MODIFY, getPageBase());
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

        return provider;
    }

    protected ObjectQuery createContentQuery(String oid) {
//        Search search = searchModel.getObject();
//        ObjectQuery query = search.createObjectQuery(parentPage.getPrismContext());
//        query = addFilterToContentQuery(query);
//        return query;
        return createMemberQuery(oid);
    }

    protected ObjectQuery createMemberQuery(String oid) {
        ObjectQuery query = null;
        ObjectFilter filter = OrgFilter.createOrg(oid, OrgFilter.Scope.ONE_LEVEL);
        query = ObjectQuery.createObjectQuery(AndFilter.createAnd(filter));

//        TypeFilter roleTypeFilter = TypeFilter.createType(RoleType.COMPLEX_TYPE, filter);
//        TypeFilter orgTypeFilter = TypeFilter.createType(OrgType.COMPLEX_TYPE, filter);
//        TypeFilter serviceTypeFilter = TypeFilter.createType(ServiceType.COMPLEX_TYPE, filter);
//        query = ObjectQuery.createObjectQuery(OrFilter.createOr(roleTypeFilter, orgTypeFilter, serviceTypeFilter));
        return ObjectQuery.createObjectQuery(TypeFilter.createType(RoleType.COMPLEX_TYPE, query.getFilter()));

    }


}

