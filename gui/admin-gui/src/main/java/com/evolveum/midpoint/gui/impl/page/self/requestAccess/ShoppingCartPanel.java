/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ShoppingCartPanel extends BasePanel implements WizardPanel {

    private static final String ID_TABLE = "table";
    private static final String ID_TABLE_FOOTER_FRAGMENT = "tableFooterFragment";
    private static final String ID_TABLE_BUTTON_COLUMN = "tableButtonColumn";
    private static final String ID_CLEAR_CART = "clearCart";
    private static final String ID_EDIT = "edit";
    private static final String ID_REMOVE = "remove";

    public ShoppingCartPanel(String id) {
        super(id);

        initLayout();
    }

    @Override
    public IModel<String> getTitle() {
        return () -> getString("ShoppingCartPanel.title");
    }

    @Override
    public String appendCssToWizard() {
        return "w-100";
    }

    private void initLayout() {
        List<IColumn> columns = createColumns();
        ISortableDataProvider provider = new ListDataProvider(this, () -> List.of(""));
        BoxedTablePanel table = new BoxedTablePanel(ID_TABLE, provider, columns) {

            @Override
            protected WebMarkupContainer createButtonToolbar(String id) {
                Fragment fragment = new Fragment(id, ID_TABLE_FOOTER_FRAGMENT, ShoppingCartPanel.this);
                fragment.add(new AjaxLink<>(ID_CLEAR_CART) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        clearCartPerformed(target);
                    }
                });

                return fragment;
            }
        };
        add(table);
    }

    private List<IColumn> createColumns() {
        List<IColumn> columns = new ArrayList<>();
//        columns.add(new IconColumn() {
//            @Override
//            protected DisplayType getIconDisplayType(IModel rowModel) {
//                return null;
//            }
//        });
        columns.add(new AbstractColumn(createStringResource("Access name")) {
            @Override
            public void populateItem(Item item, String id, IModel iModel) {
                item.add(new Label(id, "asdf"));
            }
        });
        columns.add(new AbstractColumn(createStringResource("Selected users")) {
            @Override
            public void populateItem(Item item, String id, IModel model) {
                item.add(new Label(id, "zxcv"));
            }
        });
        columns.add(new AbstractColumn(() -> "") {
            @Override
            public void populateItem(Item item, String id, IModel model) {
                Fragment fragment = new Fragment(id, ID_TABLE_BUTTON_COLUMN, ShoppingCartPanel.this);
                fragment.add(new AjaxLink<>(ID_EDIT) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {

                    }
                });
                fragment.add(new AjaxLink<>(ID_REMOVE) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {

                    }
                });

                item.add(fragment);
            }
        });

        return columns;
    }

    protected void clearCartPerformed(AjaxRequestTarget target) {

    }
}
