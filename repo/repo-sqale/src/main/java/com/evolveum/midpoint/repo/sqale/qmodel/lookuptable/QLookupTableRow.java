/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.lookuptable;

import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QLookupTableRow extends FlexibleRelationalPathBase<MLookupTableRow> {

    private static final long serialVersionUID = -9105517154692783998L;

    public static final String TABLE_NAME = "m_lookup_table_row";

    // no additional columns and relations

    public QLookupTableRow(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QLookupTableRow(String variable, String schema, String table) {
        super(MLookupTableRow.class, variable, schema, table);
    }
}
