/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.connector;

import java.sql.Types;

import com.querydsl.core.types.dsl.*;
import com.querydsl.sql.ColumnMetadata;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolder;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QConnector extends QAssignmentHolder<MConnector> {

    private static final long serialVersionUID = 6073628996722018176L;

    public static final String TABLE_NAME = "m_connector";

    public static final ColumnMetadata CONNECTOR_BUNDLE =
            ColumnMetadata.named("connectorBundle").ofType(Types.VARCHAR);
    public static final ColumnMetadata CONNECTOR_TYPE =
            ColumnMetadata.named("connectorType").ofType(Types.VARCHAR).notNull();
    public static final ColumnMetadata CONNECTOR_VERSION =
            ColumnMetadata.named("connectorVersion").ofType(Types.VARCHAR).notNull();
    public static final ColumnMetadata FRAMEWORK_ID =
            ColumnMetadata.named("frameworkId").ofType(Types.INTEGER);
    public static final ColumnMetadata CONNECTOR_HOST_REF_TARGET_OID =
            ColumnMetadata.named("connectorHostRefTargetOid").ofType(UuidPath.UUID_TYPE);
    public static final ColumnMetadata CONNECTOR_HOST_REF_TARGET_TYPE =
            ColumnMetadata.named("connectorHostRefTargetType").ofType(Types.OTHER);
    public static final ColumnMetadata CONNECTOR_HOST_REF_RELATION_ID =
            ColumnMetadata.named("connectorHostRefRelationId").ofType(Types.INTEGER);
    public static final ColumnMetadata TARGET_SYSTEM_TYPES =
            ColumnMetadata.named("targetSystemTypes").ofType(Types.ARRAY);
    public static final ColumnMetadata AVAILABLE =
            ColumnMetadata.named("available").ofType(Types.BOOLEAN);

    public final StringPath connectorBundle = createString("connectorBundle", CONNECTOR_BUNDLE);
    public final StringPath connectorType = createString("connectorType", CONNECTOR_TYPE);
    public final StringPath connectorVersion = createString("connectorVersion", CONNECTOR_VERSION);
    public final NumberPath<Integer> frameworkId = createInteger("frameworkId", FRAMEWORK_ID);
    public final UuidPath connectorHostRefTargetOid =
            createUuid("connectorHostRefTargetOid", CONNECTOR_HOST_REF_TARGET_OID);
    public final EnumPath<MObjectType> connectorHostRefTargetType =
            createEnum("connectorHostRefTargetType",
                    MObjectType.class, CONNECTOR_HOST_REF_TARGET_TYPE);
    public final NumberPath<Integer> connectorHostRefRelationId =
            createInteger("connectorHostRefRelationId", CONNECTOR_HOST_REF_RELATION_ID);
    public final ArrayPath<Integer[], Integer> targetSystemTypes =
            createArray("targetSystemTypes", Integer[].class, TARGET_SYSTEM_TYPES);
    public final BooleanPath available = createBoolean("available", AVAILABLE);

    public QConnector(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QConnector(String variable, String schema, String table) {
        super(MConnector.class, variable, schema, table);
    }
}
