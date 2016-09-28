package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.MultiButtonTable;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.orgs.OrgTreePanel;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.web.session.SessionStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by honchar.
 */
public class AssignmentCatalogPanel<F extends FocusType> extends BasePanel<String> {
    private static String ID_TREE_PANEL = "treePanel";
    private static String ID_ASSIGNMENTS_PANEL = "assignmentsPanel";
    private static String ID_CART_BUTTON = "cartButton";
    private static String ID_CART_ITEMS_COUNT = "itemsCount";

    public AssignmentCatalogPanel(String id) {
        super(id);
    }

    public AssignmentCatalogPanel(String id, IModel<String> rootOidModel) {
        super(id, rootOidModel);
        initLayout();
    }

    private void initLayout() {
        setOutputMarkupId(true);

        AjaxButton cartButton = new AjaxButton(ID_CART_BUTTON) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {

            }
        };
        cartButton.setOutputMarkupId(true);
        add(cartButton);

        Label cartItemsCount = new Label(ID_CART_ITEMS_COUNT, new IModel<String>() {
            @Override
            public String getObject() {
                SessionStorage storage = getPageBase().getSessionStorage();
                return Integer.toString(storage.getUsers().getAssignmentShoppingCart().size());
            }

            @Override
            public void setObject(String s) {


            }

            @Override
            public void detach() {

            }
        });
        cartItemsCount.add(new VisibleEnableBehaviour(){
            @Override
        public boolean isVisible(){
                SessionStorage storage = getPageBase().getSessionStorage();
                if (storage.getUsers().getAssignmentShoppingCart().size() == 0){
                    return false;
                } else {
                    return true;
                }
            }
        });
        cartItemsCount.setOutputMarkupId(true);
        cartButton.add(cartItemsCount);

        OrgTreePanel treePanel = new OrgTreePanel(ID_TREE_PANEL, getModel(), false, "AssignmentShoppingCartPanel.treeTitle") {
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
        add(treePanel);


        WebMarkupContainer initAssignmentsPanel = new WebMarkupContainer(ID_ASSIGNMENTS_PANEL);
        initAssignmentsPanel.setOutputMarkupId(true);
        add(initAssignmentsPanel);

    }

    private void selectTreeItemPerformed(SelectableBean<OrgType> selected, AjaxRequestTarget target) {
        final OrgType selectedOgr = selected.getValue();
        final ObjectDataProvider<AssignmentEditorDto, FocusType> provider = initProvider(selectedOgr.getOid());
        if (provider != null) {
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

    private ObjectQuery createMemberQuery(String oid) {
        ObjectFilter filter = OrgFilter.createOrg(oid, OrgFilter.Scope.ONE_LEVEL);

        TypeFilter roleTypeFilter = TypeFilter.createType(RoleType.COMPLEX_TYPE, filter);
        TypeFilter orgTypeFilter = TypeFilter.createType(OrgType.COMPLEX_TYPE, filter);
        TypeFilter serviceTypeFilter = TypeFilter.createType(ServiceType.COMPLEX_TYPE, filter);
        ObjectQuery query = ObjectQuery.createObjectQuery(OrFilter.createOr(roleTypeFilter, orgTypeFilter, serviceTypeFilter));
        return query;

    }

    public void reloadCartButton(AjaxRequestTarget target){
        target.add(get(ID_CART_BUTTON));
    }
}

