/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.role;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QRole extends QAbstractRole<MRole> {

    private static final long serialVersionUID = -6556210963622526756L;

    public static final String TABLE_NAME = "m_role";

    // columns and relations

    public QRole(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QRole(String variable, String schema, String table) {
        super(MRole.class, variable, schema, table);
    }
}
