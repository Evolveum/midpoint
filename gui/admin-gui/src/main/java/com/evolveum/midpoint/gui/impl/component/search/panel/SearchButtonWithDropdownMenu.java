/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search.panel;

import java.io.Serial;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

public abstract class SearchButtonWithDropdownMenu<E extends Enum> extends BasePanel<List<E>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_SEARCH_BUTTON = "searchButton";
    private static final String ID_SEARCH_BUTTON_LABEL = "searchButtonLabel";
    private static final String ID_MENU_ITEMS = "menuItems";
    private static final String ID_MENU_ITEM = "menuItem";
    private static final String ID_DROPDOWN_BUTTON = "dropdownButton";

    private final IModel<E> mode;

    public SearchButtonWithDropdownMenu(String id, @NotNull IModel<List<E>> menuItemsModel, IModel<E> defaultValue) {
        super(id, menuItemsModel);
        this.mode = defaultValue;

    }

    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        final AjaxSubmitLink searchButton = new AjaxSubmitLink(ID_SEARCH_BUTTON) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onError(AjaxRequestTarget target) {
                Form<?> form = SearchButtonWithDropdownMenu.this.findParent(Form.class);
                if (form != null) {
                    target.add(form);
                } else {
                    target.add(SearchButtonWithDropdownMenu.this.getPageBase());
                }
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                searchPerformed(target);
            }
        };
        searchButton.setOutputMarkupId(true);
        searchButton.add(getSearchButtonVisibleEnableBehavior());

        Label buttonLabel = new Label(ID_SEARCH_BUTTON_LABEL, new LoadableDetachableModel<String>() {
            @Override
            protected String load() {
                return createStringResource(mode.getObject()).getString();
            }
        });
        searchButton.add(buttonLabel);
        add(searchButton);

        AjaxButton dropdownButton = new AjaxButton(ID_DROPDOWN_BUTTON) {
            @Override
            public void onClick(AjaxRequestTarget target) {

            }
        };
        dropdownButton.setOutputMarkupId(true);
        dropdownButton.add(new VisibleEnableBehaviour(() -> true, () -> getModelObject().size() > 1));
        add(dropdownButton);
        ListView<E> menuItems = new ListView<>(ID_MENU_ITEMS, getModel()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<E> item) {
                AjaxButton ajaxLinkPanel = new AjaxButton(ID_MENU_ITEM, createStringResource(item.getModelObject())) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        mode.setObject(item.getModelObject());
                        target.add(getSearchButton());
                        menuItemSelected(target, item.getModelObject());

                    }
                };

                item.add(ajaxLinkPanel);
            }
        };
        menuItems.setOutputMarkupId(true);
        add(menuItems);

    }

    protected VisibleEnableBehaviour getSearchButtonVisibleEnableBehavior() {
        return new VisibleEnableBehaviour();
    }

    public AjaxSubmitLink getSearchButton() {
        return (AjaxSubmitLink) get(ID_SEARCH_BUTTON);
    }

    public IModel<Boolean> isMenuItemVisible(E item) {
        return Model.of(true);
    }

    protected abstract void searchPerformed(AjaxRequestTarget target);

    protected abstract void menuItemSelected(AjaxRequestTarget target, E item);
}
