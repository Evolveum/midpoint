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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.sort.AjaxFallbackOrderByBorder;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackHeadersToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortState;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortStateLocator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.util.string.Strings;

/**
 * @author lazyman
 */
public class TableHeadersToolbar<T> extends AjaxFallbackHeadersToolbar<String> {

    public TableHeadersToolbar(DataTable<T, String> table, ISortStateLocator stateLocator) {
        super(table, stateLocator);
    }

    @Override
    protected WebMarkupContainer newSortableHeader(String headerId, final String property, final ISortStateLocator locator) {
        return new AjaxFallbackOrderByBorder(headerId, property, locator) {

            @Override
            protected void onSortChanged() {
                getTable().setCurrentPage(0);
            }

            @Override
            protected void onAjaxClick(AjaxRequestTarget target) {
            	target.add(getTable());
            }

            @Override
            public void onComponentTag(ComponentTag tag) {
                super.onComponentTag(tag);
                ISortState sortState = locator.getSortState();
                SortOrder dir = sortState.getPropertySortOrder(property);
                String cssClass;
                if (dir == SortOrder.ASCENDING) {
                    cssClass = "sortable asc";
                } else if (dir == SortOrder.DESCENDING) {
                    cssClass = "sortable desc";
                } else {
                    cssClass = "sortable";
                }

                if (!Strings.isEmpty(cssClass)) {
                    tag.append("class", cssClass, " ");
                }
            }
        };
    }
}
