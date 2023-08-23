/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.audit.qmodel;

import java.sql.Types;
import java.time.Instant;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

import com.querydsl.core.types.dsl.*;
import com.querydsl.sql.ColumnMetadata;

import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;

/**
 * Querydsl query type for `MA_AUDIT_DELTA` table.
 */
@SuppressWarnings("unused")
public class QAuditDelta extends FlexibleRelationalPathBase<MAuditDelta> {

    private static final long serialVersionUID = 1562903687207032933L;

    public static final String TABLE_NAME = "ma_audit_delta";

    public static final ColumnMetadata RECORD_ID =
            ColumnMetadata.named("recordId").ofType(Types.BIGINT).notNull();
    public static final ColumnMetadata TIMESTAMP =
            ColumnMetadata.named("timestamp").ofType(Types.TIMESTAMP_WITH_TIMEZONE).notNull();
    public static final ColumnMetadata CHECKSUM =
            ColumnMetadata.named("checksum").ofType(Types.VARCHAR).notNull();
    public static final ColumnMetadata DELTA =
            ColumnMetadata.named("delta").ofType(Types.BINARY);
    public static final ColumnMetadata DELTA_OID =
            ColumnMetadata.named("deltaOid").ofType(UuidPath.UUID_TYPE);
    public static final ColumnMetadata DELTA_TYPE =
            ColumnMetadata.named("deltaType").ofType(Types.OTHER);
    public static final ColumnMetadata FULL_RESULT =
            ColumnMetadata.named("fullResult").ofType(Types.BINARY);
    public static final ColumnMetadata OBJECT_NAME_NORM =
            ColumnMetadata.named("objectNameNorm").ofType(Types.VARCHAR);
    public static final ColumnMetadata OBJECT_NAME_ORIG =
            ColumnMetadata.named("objectNameOrig").ofType(Types.VARCHAR);
    public static final ColumnMetadata RESOURCE_NAME_NORM =
            ColumnMetadata.named("resourceNameNorm").ofType(Types.VARCHAR);
    public static final ColumnMetadata RESOURCE_NAME_ORIG =
            ColumnMetadata.named("resourceNameOrig").ofType(Types.VARCHAR);
    public static final ColumnMetadata RESOURCE_OID =
            ColumnMetadata.named("resourceOid").ofType(UuidPath.UUID_TYPE);
    public static final ColumnMetadata STATUS =
            ColumnMetadata.named("status").ofType(Types.OTHER);

    public static final ColumnMetadata SHADOW_INTENT =
            ColumnMetadata.named("shadowIntent").ofType(Types.VARCHAR);
    public static final ColumnMetadata SHADOW_KIND =
            ColumnMetadata.named("shadowKind").ofType(Types.OTHER);


    public final NumberPath<Long> recordId = createLong("recordId", RECORD_ID);
    public final DateTimePath<Instant> timestamp = createInstant("timestamp", TIMESTAMP);
    public final StringPath checksum = createString("checksum", CHECKSUM);
    public final ArrayPath<byte[], Byte> delta = createByteArray("delta", DELTA);
    public final UuidPath deltaOid = createUuid("deltaOid", DELTA_OID);
    public final EnumPath<ChangeTypeType> deltaType =
            createEnum("deltaType", ChangeTypeType.class, DELTA_TYPE);
    public final ArrayPath<byte[], Byte> fullResult = createByteArray("fullResult", FULL_RESULT);
    public final StringPath objectNameNorm = createString("objectNameNorm", OBJECT_NAME_NORM);
    public final StringPath objectNameOrig = createString("objectNameOrig", OBJECT_NAME_ORIG);
    public final UuidPath resourceOid = createUuid("resourceOid", RESOURCE_OID);
    public final StringPath resourceNameNorm = createString("resourceNameNorm", RESOURCE_NAME_NORM);
    public final StringPath resourceNameOrig = createString("resourceNameOrig", RESOURCE_NAME_ORIG);
    public final EnumPath<OperationResultStatusType> status =
            createEnum("status", OperationResultStatusType.class, STATUS);

    public final StringPath shadowIntent = createString("shadowIntent", SHADOW_INTENT);
    public final EnumPath<ShadowKindType> shadowKind = createEnum("shadowKind", ShadowKindType.class, SHADOW_KIND);


    public QAuditDelta(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QAuditDelta(String variable, String schema, String table) {
        super(MAuditDelta.class, variable, schema, table);
    }
}
