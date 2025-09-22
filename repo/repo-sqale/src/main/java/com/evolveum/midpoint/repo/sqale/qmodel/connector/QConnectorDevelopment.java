/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.connector;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolder;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;

import com.querydsl.core.types.dsl.*;
import com.querydsl.sql.ColumnMetadata;

import java.sql.Types;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QConnectorDevelopment extends QObject<MConnectorDevelopment> {

    private static final long serialVersionUID = 6073628996722018176L;

    public static final String TABLE_NAME = "m_connector_development";

    public QConnectorDevelopment(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QConnectorDevelopment(String variable, String schema, String table) {
        super(MConnectorDevelopment.class, variable, schema, table);
    }
}
