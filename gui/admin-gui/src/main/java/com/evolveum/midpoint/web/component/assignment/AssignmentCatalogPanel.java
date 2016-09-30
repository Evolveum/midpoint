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
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by honchar.
 */
public class AssignmentCatalogPanel<F extends FocusType> extends BasePanel<String> {
    private static String ID_TREE_PANEL = "treePanel";
    private static String ID_CART_BUTTON = "cartButton";
    private static String ID_CART_ITEMS_COUNT = "itemsCount";
    private static String ID_CATALOG_ITEMS_PANEL = "catalogItemsPanel";

    private PageBase pageBase;

    public AssignmentCatalogPanel(String id) {
        super(id);
    }

    public AssignmentCatalogPanel(String id, IModel<String> rootOidModel, PageBase pageBase) {
        super(id, rootOidModel);
        this.pageBase = pageBase;
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
        cartItemsCount.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                SessionStorage storage = getPageBase().getSessionStorage();
                if (storage.getUsers().getAssignmentShoppingCart().size() == 0) {
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

        CatalogItemsPanel catalogItemsPanel = new CatalogItemsPanel(ID_CATALOG_ITEMS_PANEL, getModel(), getModelObject(), pageBase);
        catalogItemsPanel.setOutputMarkupId(true);
        add(catalogItemsPanel);
    }

    private void selectTreeItemPerformed(SelectableBean<OrgType> selected, AjaxRequestTarget target) {
        final OrgType selectedOgr = selected.getValue();
        CatalogItemsPanel catalogItemsPanel = new CatalogItemsPanel(ID_CATALOG_ITEMS_PANEL, new IModel<String>() {
            @Override
            public String getObject() {
                return selectedOgr.getOid();
            }

            @Override
            public void setObject(String s) {

            }

            @Override
            public void detach() {

            }
        }, selectedOgr.getOid(), pageBase);
        catalogItemsPanel.setOutputMarkupId(true);
        addOrReplace(catalogItemsPanel);
        target.add(catalogItemsPanel);
        target.add(catalogItemsPanel.getParent());
    }


    public void reloadCartButton(AjaxRequestTarget target) {
        target.add(get(ID_CART_BUTTON));
    }
}

