/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component.data;

import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortStateLocator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.HeadersToolbar;
import org.apache.wicket.markup.html.WebMarkupContainer;

/**
 * @author lazyman
 */
public class TableHeadersToolbar<T> extends HeadersToolbar {

    public TableHeadersToolbar(DataTable<T> table, ISortStateLocator stateLocator) {
        super(table, stateLocator);
    }

    @Override
    protected WebMarkupContainer newSortableHeader(String headerId, String property, ISortStateLocator locator) {
        return new BasicOrderByBorder(headerId, property, locator) {

            @Override
            protected void onSortChanged() {
                getTable().setCurrentPage(0);
            }
        };
    }
}
