/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data.mining;

import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.repeater.data.IDataProvider;

/**
 * <p>NOTE: This class is experimental and may be removed in the future.</p>
 * Part of RoleAnalysisCollapsableTablePanel class
 */
public class CustomDataTable<T, S> extends DataTable<T, S> {

    public CustomDataTable(String id, List<? extends IColumn<T, S>> columns, IDataProvider<T> dataProvider, long rowsPerPage) {
        super(id, columns, dataProvider, rowsPerPage);
    }

}
