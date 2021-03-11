/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.cases;

import java.sql.Types;
import java.time.Instant;

import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.EnumPath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;

import com.evolveum.midpoint.repo.sqale.MObjectType;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QCase extends QObject<MCase> {

    private static final long serialVersionUID = -5546874425855732858L;

    public static final String TABLE_NAME = "m_case";

    public static final ColumnMetadata STATE =
            ColumnMetadata.named("state").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata CLOSE_TIMESTAMP =
            ColumnMetadata.named("closeTimestamp").ofType(Types.TIMESTAMP_WITH_TIMEZONE);
    public static final ColumnMetadata OBJECT_REF_TARGET_OID =
            ColumnMetadata.named("objectRef_targetOid").ofType(UuidPath.UUID_TYPE);
    public static final ColumnMetadata OBJECT_REF_TARGET_TYPE =
            ColumnMetadata.named("objectRef_targetType").ofType(Types.OTHER);
    public static final ColumnMetadata OBJECT_REF_RELATION_ID =
            ColumnMetadata.named("objectRef_relation_id").ofType(Types.INTEGER);
    public static final ColumnMetadata PARENT_REF_TARGET_OID =
            ColumnMetadata.named("parentRef_targetOid").ofType(UuidPath.UUID_TYPE);
    public static final ColumnMetadata PARENT_REF_TARGET_TYPE =
            ColumnMetadata.named("parentRef_targetType").ofType(Types.OTHER);
    public static final ColumnMetadata PARENT_REF_RELATION_ID =
            ColumnMetadata.named("parentRef_relation_id").ofType(Types.INTEGER);
    public static final ColumnMetadata REQUESTOR_REF_TARGET_OID =
            ColumnMetadata.named("requestorRef_targetOid").ofType(UuidPath.UUID_TYPE);
    public static final ColumnMetadata REQUESTOR_REF_TARGET_TYPE =
            ColumnMetadata.named("requestorRef_targetType").ofType(Types.OTHER);
    public static final ColumnMetadata REQUESTOR_REF_RELATION_ID =
            ColumnMetadata.named("requestorRef_relation_id").ofType(Types.INTEGER);
    public static final ColumnMetadata TARGET_REF_TARGET_OID =
            ColumnMetadata.named("targetRef_targetOid").ofType(UuidPath.UUID_TYPE);
    public static final ColumnMetadata TARGET_REF_TARGET_TYPE =
            ColumnMetadata.named("targetRef_targetType").ofType(Types.OTHER);
    public static final ColumnMetadata TARGET_REF_RELATION_ID =
            ColumnMetadata.named("targetRef_relation_id").ofType(Types.INTEGER);

    // attributes

    public final StringPath state = createString("state", STATE);
    public final DateTimePath<Instant> closeTimestamp =
            createInstant("closeTimestamp", CLOSE_TIMESTAMP);
    public final UuidPath objectRefTargetOid =
            createUuid("objectRefTargetOid", OBJECT_REF_TARGET_OID);
    public final EnumPath<MObjectType> objectRefTargetType =
            createEnum("objectRefTargetType", MObjectType.class, OBJECT_REF_TARGET_TYPE);
    public final NumberPath<Integer> objectRefRelationId =
            createInteger("objectRefRelationId", OBJECT_REF_RELATION_ID);
    public final UuidPath parentRefTargetOid =
            createUuid("parentRefTargetOid", PARENT_REF_TARGET_OID);
    public final EnumPath<MObjectType> parentRefTargetType =
            createEnum("parentRefTargetType", MObjectType.class, PARENT_REF_TARGET_TYPE);
    public final NumberPath<Integer> parentRefRelationId =
            createInteger("parentRefRelationId", PARENT_REF_RELATION_ID);
    public final UuidPath requestorRefTargetOid =
            createUuid("requestorRefTargetOid", REQUESTOR_REF_TARGET_OID);
    public final EnumPath<MObjectType> requestorRefTargetType =
            createEnum("requestorRefTargetType", MObjectType.class, REQUESTOR_REF_TARGET_TYPE);
    public final NumberPath<Integer> requestorRefRelationId =
            createInteger("requestorRefRelationId", REQUESTOR_REF_RELATION_ID);
    public final UuidPath targetRefTargetOid =
            createUuid("targetRefTargetOid", TARGET_REF_TARGET_OID);
    public final EnumPath<MObjectType> targetRefTargetType =
            createEnum("targetRefTargetType", MObjectType.class, TARGET_REF_TARGET_TYPE);
    public final NumberPath<Integer> targetRefRelationId =
            createInteger("targetRefRelationId", TARGET_REF_RELATION_ID);

    public QCase(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QCase(String variable, String schema, String table) {
        super(MCase.class, variable, schema, table);
    }
}
