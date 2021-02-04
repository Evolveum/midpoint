/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.connector;

import java.sql.Types;

import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;

import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QConnectorHost extends QObject<MConnectorHost> {

    private static final long serialVersionUID = 8908570767190499506L;

    public static final String TABLE_NAME = "m_connector_host";

    public static final ColumnMetadata HOSTNAME =
            ColumnMetadata.named("hostname").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata PORT =
            ColumnMetadata.named("port").ofType(Types.VARCHAR).withSize(32);

    public final StringPath hostname = createString("hostname", HOSTNAME);
    public final StringPath port = createString("port", PORT);

    public QConnectorHost(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QConnectorHost(String variable, String schema, String table) {
        super(MConnectorHost.class, variable, schema, table);
    }
}
