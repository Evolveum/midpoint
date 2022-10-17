/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.visit.IVisit;
import org.apache.wicket.util.visit.IVisitor;

import java.util.List;

public class SelectableDataTable<T> extends DataTable<T, String> {

    public SelectableDataTable(String id, List<IColumn<T, String>> columns, IDataProvider<T> dataProvider, int rowsPerPage) {
        super(id, columns, dataProvider, rowsPerPage);
        visitChildren((component, objectIVisit) -> {
            if (component.getId() != null && component.getId().equals("body")) {
                component.setOutputMarkupId(true);
            }
        });
    }

    @Override
    protected Item<T> newRowItem(String id, int index, final IModel<T> model) {
        final Item<T> rowItem = new SelectableRowItem<>(id, index, model);

        rowItem.setOutputMarkupId(true);
        return rowItem;
    }

    public static class SelectableRowItem<T> extends Item<T> {

        public SelectableRowItem(String id, int index, IModel<T> model) {
            super(id, index, model);
        }
    }

    @Override
    protected Item<IColumn<T, String>> newCellItem(String id, int index, IModel<IColumn<T, String>> model) {
        Item item = super.newCellItem(id, index, model);
        item.add(AttributeAppender.append("class", "word-break-longer-text"));
        item.add(AttributeAppender.append("style", "word-wrap: break-word !important;"));

        return item;
    }

    @Override
    protected void onPageChanged() {
        super.onPageChanged();
     }
}
