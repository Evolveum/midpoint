/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.connector;

import java.sql.Types;

import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;

import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolder;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QConnectorHost extends QAssignmentHolder<MConnectorHost> {

    private static final long serialVersionUID = 8908570767190499506L;

    public static final String TABLE_NAME = "m_connector_host";

    public static final ColumnMetadata HOSTNAME =
            ColumnMetadata.named("hostname").ofType(Types.VARCHAR);
    public static final ColumnMetadata PORT =
            ColumnMetadata.named("port").ofType(Types.VARCHAR);

    public final StringPath hostname = createString("hostname", HOSTNAME);
    public final StringPath port = createString("port", PORT);

    public QConnectorHost(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QConnectorHost(String variable, String schema, String table) {
        super(MConnectorHost.class, variable, schema, table);
    }
}
