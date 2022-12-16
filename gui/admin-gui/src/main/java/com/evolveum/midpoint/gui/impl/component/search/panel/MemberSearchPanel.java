package com.evolveum.midpoint.gui.impl.component.search.panel;

import com.evolveum.midpoint.gui.impl.component.search.wrapper.AbstractRoleSearchItemWrapper;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

public class MemberSearchPanel extends AbstractSearchItemPanel<AbstractRoleSearchItemWrapper> {

    private static final String ID_MEMBER_SEARCH = "memberSearch";
    private static final String ID_SCOPE = "scope";
    private static final String ID_RELATION = "relation";
    private static final String ID_INDIRECT = "indirect";
    private static final String ID_TENANT = "tenant";
    private static final String ID_PROJECT = "project";

    public MemberSearchPanel(String id, IModel<AbstractRoleSearchItemWrapper> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
        setOutputMarkupId(true);
        add(AttributeAppender.append("style", "display: contents !important; background-color: white!important;"));
        add(AttributeAppender.append("class", "d-flex gap-1 pl-1 bg-light rounded-sm align-items-center"));
    }

    private void initLayout() {
        ScopeSearchItemPanel scopeSearchItemPanel = new ScopeSearchItemPanel(ID_SCOPE,
                new PropertyModel<>(getModel(), AbstractRoleSearchItemWrapper.F_SCOPE)){

            @Override
            protected void onScopeItemChanged(AjaxRequestTarget target) {
                refreshSearchItems(target);
//                target.add(MemberSearchPanel.this);
//                SearchPanel searchPanel = (SearchPanel) findParent(SearchPanel.class);
//                if (searchPanel != null) {
//                    target.add(searchPanel);
//                }
            }
        };
        scopeSearchItemPanel.add(new VisibleBehaviour(() -> isScopeVisible()));
        scopeSearchItemPanel.setOutputMarkupId(true);
        scopeSearchItemPanel.setOutputMarkupPlaceholderTag(true);
        add(scopeSearchItemPanel);

        RelationSearchItemPanel relationSearchItemPanel = new RelationSearchItemPanel(ID_RELATION,
                new PropertyModel<>(getModel(), AbstractRoleSearchItemWrapper.F_RELATION));
        relationSearchItemPanel.add(new VisibleBehaviour(() -> isRelationVisible()));
        relationSearchItemPanel.setOutputMarkupId(true);
        add(relationSearchItemPanel);

        IndirectSearchItemPanel indirectSearchItemPanel = new IndirectSearchItemPanel(ID_INDIRECT,
                new PropertyModel<>(getModel(), AbstractRoleSearchItemWrapper.F_INDIRECT)) {
            @Override
            protected void onIndirectItemChanged(AjaxRequestTarget target) {
                refreshSearchItems(target);
//                SearchPanel searchPanel = (SearchPanel) findParent(SearchPanel.class);
//                if (searchPanel != null) {
//                    target.add(searchPanel);
//                }
            }
        };
        indirectSearchItemPanel.add(new VisibleBehaviour(() -> isIndirectVisible()));
        indirectSearchItemPanel.setOutputMarkupId(true);
        add(indirectSearchItemPanel);

        TenantSearchItemPanel tenantSearchItemPanel = new TenantSearchItemPanel(ID_TENANT,
                new PropertyModel<>(getModel(), AbstractRoleSearchItemWrapper.F_TENANT));
        tenantSearchItemPanel.add(new VisibleBehaviour(() -> isParameterSearchVisible()));
        tenantSearchItemPanel.setOutputMarkupId(true);
        add(tenantSearchItemPanel);

        ProjectSearchItemPanel projectSearchItemWrapper = new ProjectSearchItemPanel(ID_PROJECT,
                new PropertyModel<>(getModel(), AbstractRoleSearchItemWrapper.F_PROJECT));
        projectSearchItemWrapper.add(new VisibleBehaviour(() -> isParameterSearchVisible()));
        projectSearchItemWrapper.setOutputMarkupId(true);
        add(projectSearchItemWrapper);


//        return !isSearchItemVisible(RelationSearchItemWrapper.class, search) && !isSearchItemVisible(IndirectSearchItemWrapper.class, search)
//                && (!isOrg() || !isSearchItemVisible(ScopeSearchItemWrapper.class, search))
//                && (isNotRole() || !isSearchItemVisible(TenantSearchItemWrapper.class, search))
//                && (isNotRole() || !isSearchItemVisible(ProjectSearchItemWrapper.class, search));
    }

    private void refreshSearchItems(AjaxRequestTarget target) {
//        target.add(get(ID_SCOPE));
//        target.add(get(ID_INDIRECT));
//        target.add(get(ID_TENANT));
//        target.add(get(ID_PROJECT));
//        target.add(get(ID_RELATION));
        target.add(MemberSearchPanel.this);
    }

    private boolean isScopeVisible() {
        return getModelObject().isSearchScopeVisible();
    }

    private boolean isRelationVisible() {
        return getModelObject().isRelationVisible();
//        CollectionUtils.isEmpty(relationSearchItemConfigurationType.getSupportedRelations())
//                || relationSearchItemConfigurationType.getDefaultScope() == null
//                || !SearchBoxScopeType.SUBTREE.equals(getSearchConfig().getDefaultScope());
    }

    private boolean isIndirectVisible() {
        return getModelObject().isIndirectVisible();
    }

    private boolean isParameterSearchVisible() {
        return getModelObject().isParameterSearchVisible();
    }

}
