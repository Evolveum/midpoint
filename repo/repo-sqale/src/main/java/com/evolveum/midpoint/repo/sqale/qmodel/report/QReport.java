/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.report;

import java.sql.Types;

import com.querydsl.core.types.dsl.BooleanPath;
import com.querydsl.core.types.dsl.EnumPath;
import com.querydsl.sql.ColumnMetadata;

import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrientationType;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QReport extends QObject<MReport> {

    private static final long serialVersionUID = -5738006878845987541L;

    public static final String TABLE_NAME = "m_report";

    public static final ColumnMetadata ORIENTATION =
            ColumnMetadata.named("orientation").ofType(Types.OTHER);
    public static final ColumnMetadata PARENT =
            ColumnMetadata.named("parent").ofType(Types.BOOLEAN);

    public final EnumPath<OrientationType> orientation =
            createEnum("orientation", OrientationType.class, ORIENTATION);
    public final BooleanPath parent = createBoolean("parent", PARENT);

    public QReport(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QReport(String variable, String schema, String table) {
        super(MReport.class, variable, schema, table);
    }
}
