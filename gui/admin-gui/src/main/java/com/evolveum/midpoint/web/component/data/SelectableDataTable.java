/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.component.data;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
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
        visitChildren(new IVisitor<Component, Object>() {
            @Override
            public void component(Component component, IVisit<Object> objectIVisit) {
                if (component.getId() != null && component.getId().equals("body")) {
                    component.setOutputMarkupId(true);
                }
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
        item.add(new AttributeModifier("style", "max-width: 250px; word-wrap: break-word;"));
        return item;
    }

    @Override
    protected void onPageChanged() {
        super.onPageChanged();
     }
}
