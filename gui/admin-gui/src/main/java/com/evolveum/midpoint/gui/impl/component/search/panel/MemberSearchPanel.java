package com.evolveum.midpoint.gui.impl.component.search.panel;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.impl.component.search.wrapper.AbstractRoleSearchItemWrapper;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

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
        add(AttributeAppender.append("class", "d-flex gap-2 px-2 bg-light rounded-sm align-items-center"));
    }

    private void initLayout() {
        ScopeSearchItemPanel scopeSearchItemPanel = new ScopeSearchItemPanel(ID_SCOPE,
                new PropertyModel<>(getModel(), AbstractRoleSearchItemWrapper.F_SCOPE)){

            @Override
            protected void onScopeItemChanged(AjaxRequestTarget target) {
                refreshSearchItems(target);
            }
        };
        scopeSearchItemPanel.add(new VisibleBehaviour(this::isScopeVisible));
        scopeSearchItemPanel.setOutputMarkupId(true);
        scopeSearchItemPanel.setOutputMarkupPlaceholderTag(true);
        add(scopeSearchItemPanel);

        RelationSearchItemPanel relationSearchItemPanel = new RelationSearchItemPanel(ID_RELATION,
                new PropertyModel<>(getModel(), AbstractRoleSearchItemWrapper.F_RELATION));
        relationSearchItemPanel.add(new VisibleBehaviour(this::isRelationVisible));
        relationSearchItemPanel.setOutputMarkupId(true);
        add(relationSearchItemPanel);

        IndirectSearchItemPanel indirectSearchItemPanel = new IndirectSearchItemPanel(ID_INDIRECT,
                new PropertyModel<>(getModel(), AbstractRoleSearchItemWrapper.F_INDIRECT)) {
            @Override
            protected void onIndirectItemChanged(AjaxRequestTarget target) {
                refreshSearchItems(target);
            }
        };
        indirectSearchItemPanel.add(new VisibleBehaviour(this::isIndirectVisible));
        indirectSearchItemPanel.setOutputMarkupId(true);
        add(indirectSearchItemPanel);

        TenantSearchItemPanel tenantSearchItemPanel = new TenantSearchItemPanel(ID_TENANT,
                new PropertyModel<>(getModel(), AbstractRoleSearchItemWrapper.F_TENANT));
        tenantSearchItemPanel.add(new VisibleBehaviour(this::isTenantVisible));
        tenantSearchItemPanel.setOutputMarkupId(true);
        add(tenantSearchItemPanel);

        ProjectSearchItemPanel projectSearchItemWrapper = new ProjectSearchItemPanel(ID_PROJECT,
                new PropertyModel<>(getModel(), AbstractRoleSearchItemWrapper.F_PROJECT));
        projectSearchItemWrapper.add(new VisibleBehaviour(this::isProjectVisible));
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

    private boolean isTenantVisible() {
        return getModelObject().isTenantVisible();
    }

    private boolean isProjectVisible() {
        return getModelObject().isProjectVisible();
    }

}
