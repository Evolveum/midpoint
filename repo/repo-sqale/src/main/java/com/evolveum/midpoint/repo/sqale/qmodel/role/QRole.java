/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.role;

import java.sql.Types;

import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QRole extends QAbstractRole<MRole> {

    private static final long serialVersionUID = -6556210963622526756L;

    public static final String TABLE_NAME = "m_role";

    public static final ColumnMetadata ROLE_TYPE =
            ColumnMetadata.named("roleType").ofType(Types.VARCHAR).withSize(255);

    // columns and relations

    public final StringPath roleType = createString("roleType", ROLE_TYPE);

    public QRole(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QRole(String variable, String schema, String table) {
        super(MRole.class, variable, schema, table);
    }
}
