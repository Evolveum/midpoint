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
    private IModel<String> rootOidModel;
    private String rootOid;
    private IModel<QName> viewTypeClassModel;
    private QName viewTypeClass;

    public AssignmentCatalogPanel(String id) {
        super(id);
    }

    public AssignmentCatalogPanel(String id, String rootOid, PageBase pageBase) {
        super(id);
        this.pageBase = pageBase;
        this.rootOid = rootOid;
        this.viewTypeClass = null;
        initLayout();
    }

    public AssignmentCatalogPanel(String id, QName viewTypeClass, PageBase pageBase) {
        super(id);
        this.pageBase = pageBase;
        this.viewTypeClass = viewTypeClass;
        this.rootOid = null;
        initLayout();
    }

    private void initLayout() {
        initModels();
        setOutputMarkupId(true);
        addOrReplaceLayout();
    }
    public void addOrReplaceLayout(){
        WebMarkupContainer treePanelContainer = new WebMarkupContainer(ID_TREE_PANEL_CONTAINER);
        treePanelContainer.setOutputMarkupId(true);
        addOrReplace(treePanelContainer);
        if (viewTypeClass == null) {
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
            treePanelContainer.addOrReplace(treePanel);
        } else {
            WebMarkupContainer treePanel = new WebMarkupContainer(ID_TREE_PANEL);
            treePanel.setVisible(false);
            treePanel.setOutputMarkupId(true);
            treePanelContainer.addOrReplace(treePanel);
        }

        WebMarkupContainer catalogItemsPanelContainer = new WebMarkupContainer(ID_CATALOG_ITEMS_PANEL_CONTAINER);
        catalogItemsPanelContainer.setOutputMarkupId(true);
        addOrReplace(catalogItemsPanelContainer);

        CatalogItemsPanel catalogItemsPanel = new CatalogItemsPanel(ID_CATALOG_ITEMS_PANEL, rootOidModel, viewTypeClassModel, pageBase);
        if (viewTypeClass == null) {
            catalogItemsPanelContainer.add(new AttributeAppender("class", "col-md-9"));
        } else {
            catalogItemsPanelContainer.add(new AttributeAppender("class", "col-md-12"));
        }
        catalogItemsPanel.setOutputMarkupId(true);
        catalogItemsPanelContainer.addOrReplace(catalogItemsPanel);
    }

    private void selectTreeItemPerformed(SelectableBean<OrgType> selected, AjaxRequestTarget target) {
        final OrgType selectedOgr = selected.getValue();
        rootOidModel.setObject(selectedOgr.getOid());
        viewTypeClassModel.setObject(null);
        CatalogItemsPanel catalogItemsPanel = new CatalogItemsPanel(ID_CATALOG_ITEMS_PANEL, rootOidModel, viewTypeClassModel, pageBase);
        catalogItemsPanel.setOutputMarkupId(true);
        ((WebMarkupContainer) get(ID_CATALOG_ITEMS_PANEL_CONTAINER)).addOrReplace(catalogItemsPanel);
        target.add(catalogItemsPanel);
        target.add(get(ID_CATALOG_ITEMS_PANEL_CONTAINER));
    }

    private void initModels(){
        rootOidModel = new IModel<String>() {
            @Override
            public String getObject() {
                return rootOid;
            }

            @Override
            public void setObject(String s) {
                rootOid = s;
            }

            @Override
            public void detach() {

            }
        };
        viewTypeClassModel = new IModel<QName>() {
            @Override
            public QName getObject() {
                return viewTypeClass;
            }

            @Override
            public void setObject(QName qName) {
                viewTypeClass = qName;
            }

            @Override
            public void detach() {

            }
        };
    }
}

