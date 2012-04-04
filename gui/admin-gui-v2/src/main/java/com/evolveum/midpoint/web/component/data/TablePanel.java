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

import org.apache.commons.lang.Validate;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.panel.Panel;

import java.util.List;

/**
 * @author lazyman
 */
public class TablePanel<T> extends Panel {

    private Class<T> type;

    public TablePanel(String id, Class<T> type, List<IColumn<T>> columns) {
        this(id, type, columns, 10);
    }

    public TablePanel(String id, Class<T> type, List<IColumn<T>> columns, int itemsPerPage) {
        super(id);
        Validate.notNull(type, "Object type must not be null.");
        Validate.notNull(columns, "Columns must not be null.");
        this.type = type;

        initLayout(columns, itemsPerPage);
    }

    private void initLayout(List<IColumn<T>> columns, int itemsPerPage) {
        ISortableDataProvider provider = new ObjectDataProvider(type);
        DataTable<T> table = new DataTable<T>("table", columns, provider, itemsPerPage);
        table.addTopToolbar(new TableHeadersToolbar(table, provider));

        add(table);
        add(new NavigatorPanel("navigatorTop", table));
        add(new NavigatorPanel("navigatorBottom", table));
    }
}
