/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.role;

import java.sql.Types;

import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.sql.ColumnMetadata;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QService extends QAbstractRole<MService> {

    private static final long serialVersionUID = 5112532519874031825L;

    public static final String TABLE_NAME = "m_service";

    public static final ColumnMetadata DISPLAY_ORDER =
            ColumnMetadata.named("displayOrder").ofType(Types.INTEGER);

    public final NumberPath<Integer> displayOrder = createInteger("displayOrder", DISPLAY_ORDER);

    public QService(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QService(String variable, String schema, String table) {
        super(MService.class, variable, schema, table);
    }
}
