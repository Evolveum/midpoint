/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.org;

import java.sql.Types;

import com.querydsl.core.types.dsl.BooleanPath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.sql.ColumnMetadata;

import com.evolveum.midpoint.repo.sqale.qmodel.role.QAbstractRole;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QOrg extends QAbstractRole<MOrg> {

    private static final long serialVersionUID = -7711059436053747571L;

    public static final String TABLE_NAME = "m_org";

    public static final ColumnMetadata DISPLAY_ORDER =
            ColumnMetadata.named("displayOrder").ofType(Types.INTEGER);
    public static final ColumnMetadata TENANT =
            ColumnMetadata.named("tenant").ofType(Types.BOOLEAN);

    public final NumberPath<Integer> displayOrder = createInteger("displayOrder", DISPLAY_ORDER);
    public final BooleanPath tenant = createBoolean("tenant", TENANT);

    public QOrg(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QOrg(String variable, String schema, String table) {
        super(MOrg.class, variable, schema, table);
    }
}
