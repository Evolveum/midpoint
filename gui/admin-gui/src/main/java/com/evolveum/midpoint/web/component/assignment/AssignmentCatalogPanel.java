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
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.orgs.OrgTreePanel;
import com.evolveum.midpoint.web.session.SessionStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by honchar.
 */
public class AssignmentCatalogPanel<F extends AbstractRoleType> extends BasePanel {
	private static final long serialVersionUID = 1L;

	private static String ID_TREE_PANEL_CONTAINER = "treePanelContainer";
    private static String ID_TREE_PANEL = "treePanel";
    private static String ID_CATALOG_ITEMS_PANEL_CONTAINER = "catalogItemsPanelContainer";
    private static String ID_CATALOG_ITEMS_PANEL = "catalogItemsPanel";

    private PageBase pageBase;

    public AssignmentCatalogPanel(String id) {
        super(id);
    }

    public AssignmentCatalogPanel(String id, String rootOid, PageBase pageBase) {
        super(id);
        this.pageBase = pageBase;
        initLayout(null, rootOid);
    }

    public AssignmentCatalogPanel(String id, QName viewTypeClass, PageBase pageBase) {
        super(id);
        this.pageBase = pageBase;
        initLayout(viewTypeClass, null);
    }

    private void initLayout(QName viewTypeClass, final String rootOid) {
        setOutputMarkupId(true);
        WebMarkupContainer treePanelContainer = new WebMarkupContainer(ID_TREE_PANEL_CONTAINER);
        treePanelContainer.setOutputMarkupId(true);
        add(treePanelContainer);
        if (rootOid != null) {
            OrgTreePanel treePanel = new OrgTreePanel(ID_TREE_PANEL, new IModel<String>() {
                @Override
                public String getObject() {
                    return rootOid;
                }

                @Override
                public void setObject(String s) {

                }

                @Override
                public void detach() {

                }
            }, false, "AssignmentShoppingCartPanel.treeTitle") {
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
            treePanelContainer.add(new AttributeAppender("class", "col-md-3"));
            treePanelContainer.add(treePanel);
        } else {
            WebMarkupContainer treePanel = new WebMarkupContainer(ID_TREE_PANEL);
            treePanel.setVisible(false);
            treePanel.setOutputMarkupId(true);
            treePanelContainer.add(treePanel);
        }

        WebMarkupContainer catalogItemsPanelContainer = new WebMarkupContainer(ID_CATALOG_ITEMS_PANEL_CONTAINER);
        catalogItemsPanelContainer.setOutputMarkupId(true);
        add(catalogItemsPanelContainer);

        CatalogItemsPanel catalogItemsPanel;
        if (rootOid != null) {
            catalogItemsPanel = new CatalogItemsPanel(ID_CATALOG_ITEMS_PANEL, rootOid, pageBase);
            catalogItemsPanelContainer.add(new AttributeAppender("class", "col-md-9"));
        } else {
            catalogItemsPanel = new CatalogItemsPanel(ID_CATALOG_ITEMS_PANEL, viewTypeClass, pageBase);
            catalogItemsPanelContainer.add(new AttributeAppender("class", "col-md-12"));
        }
        catalogItemsPanel.setOutputMarkupId(true);
        catalogItemsPanelContainer.add(catalogItemsPanel);
    }

    private void selectTreeItemPerformed(SelectableBean<OrgType> selected, AjaxRequestTarget target) {
        final OrgType selectedOgr = selected.getValue();
        CatalogItemsPanel catalogItemsPanel = new CatalogItemsPanel(ID_CATALOG_ITEMS_PANEL, selectedOgr.getOid(), pageBase);
        catalogItemsPanel.setOutputMarkupId(true);
        ((WebMarkupContainer) get(ID_CATALOG_ITEMS_PANEL_CONTAINER)).addOrReplace(catalogItemsPanel);
        target.add(catalogItemsPanel);
        target.add(get(ID_CATALOG_ITEMS_PANEL_CONTAINER));
    }

}

