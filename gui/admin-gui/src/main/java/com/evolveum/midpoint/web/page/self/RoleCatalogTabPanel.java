/*
 * Copyright (C) 2016-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.self;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.search.SearchItem;
import com.evolveum.midpoint.web.component.search.SearchSpecialItemPanel;
import com.evolveum.midpoint.web.component.search.SpecialSearchItem;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.web.component.util.TreeSelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.orgs.OrgTreePanel;
import com.evolveum.midpoint.web.session.OrgTreeStateStorage;
import com.evolveum.midpoint.web.session.RoleCatalogStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Created by honchar
 */
public class RoleCatalogTabPanel extends AbstractShoppingCartTabPanel<AbstractRoleType> {
    private static final long serialVersionUID = 1L;

    private static final String ID_TREE_PANEL_CONTAINER = "treePanelContainer";
    private static final String ID_TREE_PANEL = "treePanel";

    private final String roleCatalogOid;

    public RoleCatalogTabPanel(String id, RoleManagementConfigurationType roleManagementConfig, String roleCatalogOid) {
        super(id, roleManagementConfig);
        this.roleCatalogOid = roleCatalogOid;
    }

    @Override
    protected void initLeftSidePanel() {
        if (StringUtils.isEmpty(getRoleCatalogStorage().getSelectedOid())) {
            getRoleCatalogStorage().setSelectedOid(roleCatalogOid);
        }

        WebMarkupContainer treePanelContainer = new WebMarkupContainer(ID_TREE_PANEL_CONTAINER);
        treePanelContainer.setOutputMarkupId(true);
        add(treePanelContainer);

        OrgTreePanel treePanel = new OrgTreePanel(ID_TREE_PANEL, Model.of(roleCatalogOid), false,
                getPageBase(), "AssignmentShoppingCartPanel.treeTitle") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void selectTreeItemPerformed(TreeSelectableBean<OrgType> selected,
                    AjaxRequestTarget target) {
                setSelected(selected);
                refreshContentPannels();
                RoleCatalogTabPanel.this.selectTreeItemPerformed(selected, target);
            }

            protected List<InlineMenuItem> createTreeMenu() {
                return new ArrayList<>();
            }

            @Override
            protected List<InlineMenuItem> createTreeChildrenMenu(TreeSelectableBean<OrgType> org) {
                return new ArrayList<>();
            }

            @Override
            public OrgTreeStateStorage getOrgTreeStateStorage() {
                return getRoleCatalogStorage();
            }
        };
        treePanel.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return isRootOrgExists();
            }
        });
        treePanel.setOutputMarkupId(true);
        treePanelContainer.add(treePanel);
    }

    @Override
    protected boolean isShoppingCartItemsPanelVisible() {
        return isRootOrgExists();
    }

    @Override
    protected void appendItemsPanelStyle(WebMarkupContainer container) {
        container.add(AttributeAppender.append("class", "col-md-9"));
    }

    private boolean isRootOrgExists() {
        OrgTreePanel treePanel = getOrgTreePanel();
        return treePanel.getSelected() != null && treePanel.getSelected().getValue() != null;
    }

    private OrgTreePanel getOrgTreePanel() {
        return (OrgTreePanel) get(ID_TREE_PANEL_CONTAINER).get(ID_TREE_PANEL);
    }

    private void selectTreeItemPerformed(SelectableBeanImpl<OrgType> selected, AjaxRequestTarget target) {
        final OrgType selectedOrg = selected.getValue();
        if (selectedOrg == null) {
            return;
        }
        getRoleCatalogStorage().setSelectedOid(selectedOrg.getOid());
        target.add(getGridViewComponent());

    }

    @Override
    protected ObjectQuery createContentQuery() {
        ObjectQuery query = super.createContentQuery();
        String oid = getRoleCatalogStorage().getSelectedOid();
        if (StringUtils.isEmpty(oid)) {
            return query;
        }
        QueryFactory queryFactory = getPrismContext().queryFactory();

        ObjectFilter filter;
        if (SearchBoxScopeType.SUBTREE.equals(getRoleCatalogStorage().getOrgSearchScope())) {
            filter = queryFactory.createOrg(oid, OrgFilter.Scope.SUBTREE);
        } else {
            filter = queryFactory.createOrg(oid, OrgFilter.Scope.ONE_LEVEL);
        }

        TypeFilter roleTypeFilter = queryFactory.createType(RoleType.COMPLEX_TYPE, filter);
        TypeFilter serviceTypeFilter = queryFactory.createType(ServiceType.COMPLEX_TYPE, filter);
        query.addFilter(queryFactory.createOr(roleTypeFilter, serviceTypeFilter));
        return query;
    }

    @Override
    protected QName getQueryType() {
        return AbstractRoleType.COMPLEX_TYPE;
    }

    @Override
    protected Search createSearch() {
        Search search = super.createSearch();
        search.addSpecialItem(createScopeItem(search));
        return search;
    }

    private SearchItem createScopeItem(Search search) {
        return new SpecialSearchItem(search) {
            @Override
            public ObjectFilter createFilter(PageBase pageBase, VariablesMap variables) {
                return null;
            }

            @Override
            public SearchSpecialItemPanel createSpecialSearchPanel(String id, Consumer<AjaxRequestTarget> searchPerformedConsumer) {
                return new SearchSpecialItemPanel(id, new PropertyModel(getRoleCatalogStorage(), RoleCatalogStorage.F_ORG_SEARCH_SCOPE)) {
                    @Override
                    protected WebMarkupContainer initSearchItemField(String id) {
                        DropDownChoicePanel inputPanel = new DropDownChoicePanel(id, getModelValue(), Model.of(Arrays.asList(SearchBoxScopeType.values())), new EnumChoiceRenderer(), false);
                        inputPanel.getBaseFormComponent().add(WebComponentUtil.getSubmitOnEnterKeyDownBehavior("searchSimple"));
                        inputPanel.getBaseFormComponent().add(AttributeAppender.append("style", "width: 88px; max-width: 400px !important;"));
                        inputPanel.getBaseFormComponent().add(new OnChangeAjaxBehavior() {
                            @Override
                            protected void onUpdate(AjaxRequestTarget target) {
                                searchPerformedConsumer.accept(target);
                            }
                        });
                        inputPanel.setOutputMarkupId(true);
                        return inputPanel;
                    }

                    @Override
                    protected IModel<String> createLabelModel() {
                        return getPageBase().createStringResource("abstractRoleMemberPanel.searchScope");
                    }

                    @Override
                    protected IModel<String> createHelpModel() {
                        return getPageBase().createStringResource("abstractRoleMemberPanel.searchScope.tooltip");
                    }
                };
            }
        };
    }
}
