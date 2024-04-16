/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.role;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QPolicy extends QAbstractRole<MPolicy> {

    private static final long serialVersionUID = -6556210963622526756L;

    public static final String TABLE_NAME = "m_policy";

    // columns and relations

    public QPolicy(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QPolicy(String variable, String schema, String table) {
        super(MPolicy.class, variable, schema, table);
    }
}
