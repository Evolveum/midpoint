/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data;

import com.evolveum.midpoint.web.security.MidPointAuthWebSession;
import com.evolveum.midpoint.web.session.SessionStorage;

import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;

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
        item.add(new BreakTextBehaviour(item.getMarkupId()));
        item.add(AttributeAppender.append("style", "word-wrap: break-word !important;"));

        return item;
    }

    @Override
    protected void onPageChanged() {
        super.onPageChanged();
     }

     private class BreakTextBehaviour extends Behavior {

        private final String markId;

         private BreakTextBehaviour(String markId) {
             this.markId = markId;
         }

         @Override
         public void renderHead(Component component, IHeaderResponse response) {
             super.renderHead(component, response);

             StringBuilder sb = new StringBuilder();
             sb.append("MidPointTheme.breakLongerTextInTableCell('")
                     .append(markId)
                     .append("');");

             response.render(OnDomReadyHeaderItem.forScript(sb.toString()));
         }
     }
}
