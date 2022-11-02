package com.evolveum.midpoint.gui.impl.component.search;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
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
    }

    private void initLayout() {
        ScopeSearchItemPanel scopeSearchItemPanel = new ScopeSearchItemPanel(ID_SCOPE, new PropertyModel<>(getModel(), AbstractRoleSearchItemWrapper.F_SCOPE));
        scopeSearchItemPanel.add(new VisibleBehaviour(() -> isScopeVisible()));
        add(scopeSearchItemPanel);

        RelationSearchItemPanel relationSearchItemPanel = new RelationSearchItemPanel(ID_RELATION, new PropertyModel<>(getModel(), AbstractRoleSearchItemWrapper.F_RELATION));
        relationSearchItemPanel.add(new VisibleBehaviour(() -> isRelationVisible()));
        add(relationSearchItemPanel);

        IndirectSearchItemPanel indirectSearchItemPanel = new IndirectSearchItemPanel(ID_INDIRECT, new PropertyModel<>(getModel(), AbstractRoleSearchItemWrapper.F_INDIRECT));
        indirectSearchItemPanel.add(new VisibleBehaviour(() -> isIndirectVisible()));
//        indirectSearchItemPanel.add(new EnableBehaviour(() -> getModelObject().isSearchScope(SearchBoxScopeType.SUBTREE)));
        add(indirectSearchItemPanel);

        TenantSearchItemPanel tenantSearchItemPanel = new TenantSearchItemPanel(ID_TENANT, new PropertyModel<>(getModel(), AbstractRoleSearchItemWrapper.F_TENANT));
        tenantSearchItemPanel.add(new VisibleBehaviour(() -> isParameterSearchVisible()));
        add(tenantSearchItemPanel);

        ProjectSearchItemPanel projectSearchItemWrapper = new ProjectSearchItemPanel(ID_PROJECT, new PropertyModel<>(getModel(), AbstractRoleSearchItemWrapper.F_PROJECT));
        projectSearchItemWrapper.add(new VisibleBehaviour(() -> isParameterSearchVisible()));
        add(projectSearchItemWrapper);


//        return !isSearchItemVisible(RelationSearchItemWrapper.class, search) && !isSearchItemVisible(IndirectSearchItemWrapper.class, search)
//                && (!isOrg() || !isSearchItemVisible(ScopeSearchItemWrapper.class, search))
//                && (isNotRole() || !isSearchItemVisible(TenantSearchItemWrapper.class, search))
//                && (isNotRole() || !isSearchItemVisible(ProjectSearchItemWrapper.class, search));
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
