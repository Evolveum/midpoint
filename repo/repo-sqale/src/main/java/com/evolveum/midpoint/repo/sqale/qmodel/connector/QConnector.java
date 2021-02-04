/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.connector;

import java.sql.Types;

import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;

import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QConnector extends QObject<MConnector> {

    private static final long serialVersionUID = 6073628996722018176L;

    public static final String TABLE_NAME = "m_connector";

    public static final ColumnMetadata CONNECTOR_BUNDLE =
            ColumnMetadata.named("connectorBundle").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata CONNECTOR_TYPE =
            ColumnMetadata.named("connectorType").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata CONNECTOR_VERSION =
            ColumnMetadata.named("connectorVersion").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata FRAMEWORK =
            ColumnMetadata.named("framework").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata CONNECTOR_HOST_REF_TARGET_OID =
            ColumnMetadata.named("connectorHostRef_targetOid").ofType(UuidPath.UUID_TYPE);
    public static final ColumnMetadata CONNECTOR_HOST_REF_TARGET_TYPE =
            ColumnMetadata.named("connectorHostRef_targetType").ofType(Types.INTEGER);
    public static final ColumnMetadata CONNECTOR_HOST_REF_RELATION_ID =
            ColumnMetadata.named("connectorHostRef_relation_id").ofType(Types.INTEGER);

    public final StringPath connectorBundle = createString("connectorBundle", CONNECTOR_BUNDLE);
    public final StringPath connectorType = createString("connectorType", CONNECTOR_TYPE);
    public final StringPath connectorVersion = createString("connectorVersion", CONNECTOR_VERSION);
    public final StringPath framework = createString("framework", FRAMEWORK);
    public final UuidPath connectorHostRefTargetOid =
            createUuid("connectorHostRefTargetOid", CONNECTOR_HOST_REF_TARGET_OID);
    public final NumberPath<Integer> connectorHostRefTargetType =
            createInteger("connectorHostRefTargetType", CONNECTOR_HOST_REF_TARGET_TYPE);
    public final NumberPath<Integer> connectorHostRefRelationId =
            createInteger("connectorHostRefRelationId", CONNECTOR_HOST_REF_RELATION_ID);

    public QConnector(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QConnector(String variable, String schema, String table) {
        super(MConnector.class, variable, schema, table);
    }
}
