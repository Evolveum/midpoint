/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data.mining;

import java.util.List;

import com.evolveum.midpoint.web.component.data.SelectableDataTable;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;

/**
 * <p>NOTE: This class is experimental and may be removed in the future.</p>
 * Part of RoleAnalysisCollapsableTablePanel class
 */
public class CustomSelectableDataTable<T> extends CustomDataTable<T, String> {

    public CustomSelectableDataTable(String id, List<IColumn<T, String>> columns, IDataProvider<T> dataProvider, int rowsPerPage) {
        super(id, columns, dataProvider, rowsPerPage);
        visitChildren((component, objectIVisit) -> {
            if (component.getId() != null && component.getId().equals("body")) {
                component.setOutputMarkupId(true);
            }
        });
    }

    @Override
    protected Item<T> newRowItem(String id, int index, final IModel<T> model) {
        final Item<T> rowItem = new SelectableDataTable.SelectableRowItem<>(id, index, model);

        rowItem.setOutputMarkupId(true);
        return rowItem;
    }

    @Override
    protected Item<IColumn<T, String>> newCellItem(String id, int index, IModel<IColumn<T, String>> model) {
        Item<IColumn<T, String>> item = super.newCellItem(id, index, model);
        if (isBreakTextBehaviourEnabled()) {
            item.add(new BreakTextBehaviour(item.getMarkupId()));
        }
        item.add(AttributeAppender.append("style", "word-wrap: break-word !important;"));

        return item;
    }

    protected boolean isBreakTextBehaviourEnabled() {
        return true;
    }

    @Override
    protected void onPageChanged() {
        super.onPageChanged();
    }

    private static class BreakTextBehaviour extends Behavior {

        private final String markId;

        private BreakTextBehaviour(String markId) {
            this.markId = markId;
        }

        @Override
        public void renderHead(Component component, IHeaderResponse response) {
            super.renderHead(component, response);

            String sb = "MidPointTheme.breakLongerTextInTableCell('" +
                    markId +
                    "');";

            response.render(OnDomReadyHeaderItem.forScript(sb));
        }
    }
}
