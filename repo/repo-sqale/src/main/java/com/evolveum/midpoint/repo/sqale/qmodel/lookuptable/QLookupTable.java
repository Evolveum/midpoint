/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.lookuptable;

import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolder;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QLookupTable extends QAssignmentHolder<MLookupTable> {

    private static final long serialVersionUID = -2040531200445583676L;

    public static final String TABLE_NAME = "m_lookup_table";

    // no additional columns and relations

    public QLookupTable(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QLookupTable(String variable, String schema, String table) {
        super(MLookupTable.class, variable, schema, table);
    }
}
