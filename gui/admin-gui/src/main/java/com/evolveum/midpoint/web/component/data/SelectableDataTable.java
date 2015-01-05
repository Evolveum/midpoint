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

import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;

import java.util.List;

public class SelectableDataTable<T> extends DataTable<T, String> {

    public SelectableDataTable(String id, List<IColumn<T, String>> columns, IDataProvider<T> dataProvider, int rowsPerPage) {
        super(id, columns, dataProvider, rowsPerPage);
    }

    @Override
    protected Item<T> newRowItem(String id, int index, final IModel<T> model) {
        final Item<T> rowItem = new SelectableRowItem<T>(id, index, model);

        rowItem.setOutputMarkupId(true);

//        rowItem.add(new AttributeAppender("class", new AbstractReadOnlyModel<String>() {
//
//            @Override
//            public String getObject() {
//                T object = rowItem.getModel().getObject();
//                if (!(object instanceof Selectable)) {
//                    return "";
//                }
//
//                Selectable selectable = (Selectable) object;
//                return selectable.isSelected() ? "selectedRow" : "";
//            }
//        }));
//
//        rowItem.add(new AjaxEventBehavior("onclick") {
//
//            @Override
//            protected void onEvent(AjaxRequestTarget target) {
//                T object = rowItem.getModel().getObject();
//                if (!(object instanceof Selectable)) {
//                    return;
//                }
//
//                Selectable selectable = (Selectable) object;
//                selectable.setSelected(!selectable.isSelected());
//
//                //update table row
//                target.add(rowItem);
//                //update checkbox header column, if we found some
//                CheckBoxPanel headerCheck = CheckBoxHeaderColumn.findCheckBoxColumnHeader(SelectableDataTable.this);
//                if (headerCheck == null) {
//                    return;
//                }
//
//                headerCheck.getPanelComponent().setModelObject(
//                        CheckBoxHeaderColumn.shoulBeHeaderSelected(SelectableDataTable.this));
//                target.add(headerCheck);
//            }
//
//            @Override
//            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
//                super.updateAjaxAttributes(attributes);
//
//                attributes.getAjaxCallListeners().add(
//                        new AjaxCallListener().onPrecondition("return !dropClickEvent(attrs);"));
//            }
//        });

        return rowItem;
    }

    public static class SelectableRowItem<T> extends Item<T> {

        public SelectableRowItem(String id, int index, IModel<T> model) {
            super(id, index, model);
        }
    }
}
