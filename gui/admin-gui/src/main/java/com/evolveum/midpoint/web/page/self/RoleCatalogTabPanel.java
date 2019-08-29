/*
 * Copyright (c) 2016-2018 Evolveum
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
package com.evolveum.midpoint.web.page.self;

import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.TreeSelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.orgs.OrgTreePanel;
import com.evolveum.midpoint.web.session.OrgTreeStateStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.Model;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by honchar
 */
public class RoleCatalogTabPanel extends AbstractShoppingCartTabPanel<AbstractRoleType> {
    private static final long serialVersionUID = 1L;

    private static final String ID_TREE_PANEL_CONTAINER = "treePanelContainer";
    private static final String ID_TREE_PANEL = "treePanel";

    private String roleCatalogOid;

    public RoleCatalogTabPanel(String id, RoleManagementConfigurationType roleManagementConfig, String roleCatalogOid){
        super(id, roleManagementConfig);
        this.roleCatalogOid = roleCatalogOid;
    }

    @Override
    protected void initLeftSidePanel(){
        getRoleCatalogStorage().setSelectedOid(roleCatalogOid);

        WebMarkupContainer treePanelContainer = new WebMarkupContainer(ID_TREE_PANEL_CONTAINER);
        treePanelContainer.setOutputMarkupId(true);
        add(treePanelContainer);

        OrgTreePanel treePanel = new OrgTreePanel(ID_TREE_PANEL, Model.of(roleCatalogOid), false,
                getPageBase(), "AssignmentShoppingCartPanel.treeTitle") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void selectTreeItemPerformed(TreeSelectableBean<OrgType> selected,
                                                   AjaxRequestTarget target) {
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
            public OrgTreeStateStorage getOrgTreeStateStorage(){
                return getRoleCatalogStorage();
            }
        };
        treePanel.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible(){
                return isRootOrgExists();
            }
        });
        treePanel.setOutputMarkupId(true);
        treePanelContainer.add(treePanel);
    }

    @Override
    protected boolean isShoppingCartItemsPanelVisible(){
        return isRootOrgExists();
    }

    @Override
    protected void appendItemsPanelStyle(WebMarkupContainer container){
        container.add(AttributeAppender.append("class", "col-md-9"));
    }

    private boolean isRootOrgExists(){
        OrgTreePanel treePanel = getOrgTreePanel();
        return treePanel.getSelected() != null && treePanel.getSelected().getValue() != null;
    }

    private OrgTreePanel getOrgTreePanel(){
        return (OrgTreePanel) get(ID_TREE_PANEL_CONTAINER).get(ID_TREE_PANEL);
    }

    private void selectTreeItemPerformed(SelectableBean<OrgType> selected, AjaxRequestTarget target) {
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
        ObjectFilter filter = queryFactory.createOrg(oid, OrgFilter.Scope.ONE_LEVEL);

        TypeFilter roleTypeFilter = queryFactory.createType(RoleType.COMPLEX_TYPE, filter);
        TypeFilter serviceTypeFilter = queryFactory.createType(ServiceType.COMPLEX_TYPE, filter);
        query.addFilter(queryFactory.createOr(roleTypeFilter, serviceTypeFilter));
        return query;
    }

    @Override
    protected QName getQueryType(){
        return AbstractRoleType.COMPLEX_TYPE;
    }

}
