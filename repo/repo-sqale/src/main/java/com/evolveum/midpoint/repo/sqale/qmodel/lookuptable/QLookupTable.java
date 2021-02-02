/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.lookuptable;

import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QLookupTable extends QObject<MLookupTable> {

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
