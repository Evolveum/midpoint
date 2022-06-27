/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;

import com.evolveum.midpoint.web.component.menu.cog.MenuLinkPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class SearchButtonWithDropdownMenu extends BasePanel<List<InlineMenuItem>> {
    private static final long serialVersionUID = 1L;

    private static final String ID_SEARCH_BUTTON = "searchButton";
    private static final String ID_SEARCH_BUTTON_LABEL = "searchButtonLabel";
    private static final String ID_MENU_ITEMS = "menuItems";
    private static final String ID_MENU_ITEM = "menuItem";

    InlineMenuItem selectedItem;

    public SearchButtonWithDropdownMenu(String id, @NotNull IModel<List<InlineMenuItem>> menuItemsModel) {
        super(id, menuItemsModel);
        selectedItem = CollectionUtils.isNotEmpty(menuItemsModel.getObject()) ? menuItemsModel.getObject().get(0) : null;
    }

    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        final AjaxSubmitLink searchButton = new AjaxSubmitLink(ID_SEARCH_BUTTON) {

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                searchPerformed(target);
            }
        };
        searchButton.setOutputMarkupId(true);

        searchButton.add(new Label(ID_SEARCH_BUTTON_LABEL, (IModel<Object>) () -> selectedItem.getLabel()));
        add(searchButton);

        ListView<InlineMenuItem> menuItems = new ListView<InlineMenuItem>(ID_MENU_ITEMS, getModel()) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<InlineMenuItem> item) {
                WebMarkupContainer menuItemBody = new MenuLinkPanel(ID_MENU_ITEM, item.getModel());
                menuItemBody.setRenderBodyOnly(true);
                item.add(menuItemBody);
                menuItemBody.add(new VisibleEnableBehaviour() {
                    @Override
                    public boolean isVisible() {
                        return Boolean.TRUE.equals(item.getModelObject().getVisible().getObject());
                    }
                });
            }
        };
        menuItems.setOutputMarkupId(true);
        add(menuItems);

    }

    public Component getSearchButton() {
        return get(ID_SEARCH_BUTTON);
    }

    protected abstract void searchPerformed(AjaxRequestTarget target);
}
