/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data;

import com.evolveum.midpoint.gui.impl.component.data.provider.BaseSortableDataProvider;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.sort.AjaxFallbackOrderByBorder;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackHeadersToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortState;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortStateLocator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.util.string.Strings;

public class RoleAnalysisTableHeadersToolbar<T> extends AjaxFallbackHeadersToolbar<String> {

    public RoleAnalysisTableHeadersToolbar(DataTable<T, String> table, ISortStateLocator stateLocator) {
        super(table, stateLocator);
    }

    @Override
    protected WebMarkupContainer newSortableHeader(String headerId, final String property, final ISortStateLocator locator) {
        IDataProvider provider = getTable().getDataProvider();
        if (provider instanceof BaseSortableDataProvider) {
            BaseSortableDataProvider sortableDataProvider = (BaseSortableDataProvider) provider;
            if (sortableDataProvider.isOrderingDisabled()) {
                return new WebMarkupContainer(headerId);
            }
        }

        return new AjaxFallbackOrderByBorder(headerId, property, locator) {

            @Override
            protected void onSortChanged() {
                getTable().setCurrentPage(0);
            }

            @Override
            protected void onAjaxClick(AjaxRequestTarget target) {
                refreshTable(target);
            }

            @Override
            public void onComponentTag(ComponentTag tag) {
                super.onComponentTag(tag);
                ISortState sortState = locator.getSortState();
                SortOrder dir = sortState.getPropertySortOrder(property);
                String cssClass;
                if (dir == SortOrder.ASCENDING) {
                    cssClass = "sortable asc role-mining-rotated-header";
                } else if (dir == SortOrder.DESCENDING) {
                    cssClass = "sortable desc role-mining-rotated-header";
                } else {
                    cssClass = "sortable";
                }

                if (!Strings.isEmpty(cssClass)) {
                    tag.remove("class");
                    tag.append("class", cssClass, " ");
                }
            }
        };
    }

    protected void refreshTable(AjaxRequestTarget target) {
        target.add(getTable());
    }
}
