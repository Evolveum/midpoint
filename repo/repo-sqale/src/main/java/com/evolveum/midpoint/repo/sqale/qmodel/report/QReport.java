/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.report;

import java.sql.Types;

import com.querydsl.core.types.dsl.BooleanPath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.sql.ColumnMetadata;

import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QReport extends QObject<MReport> {

    private static final long serialVersionUID = -5738006878845987541L;

    public static final String TABLE_NAME = "m_report";

    public static final ColumnMetadata EXPORT =
            ColumnMetadata.named("export").ofType(Types.INTEGER);
    public static final ColumnMetadata ORIENTATION =
            ColumnMetadata.named("orientation").ofType(Types.INTEGER);
    public static final ColumnMetadata PARENT =
            ColumnMetadata.named("parent").ofType(Types.BOOLEAN);

    public final NumberPath<Integer> export = createInteger("export", EXPORT);
    public final NumberPath<Integer> orientation = createInteger("orientation", ORIENTATION);
    public final BooleanPath parent = createBoolean("parent", PARENT);
    // TODO what about that useHibernateSession

    public QReport(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QReport(String variable, String schema, String table) {
        super(MReport.class, variable, schema, table);
    }
}
