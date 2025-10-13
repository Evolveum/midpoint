/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.audit.querymodel;

import java.sql.Types;

import com.querydsl.core.types.dsl.ArrayPath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.ForeignKey;
import com.querydsl.sql.PrimaryKey;

import com.evolveum.midpoint.repo.sql.audit.beans.MAuditDelta;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * Querydsl query type for M_AUDIT_DELTA table.
 */
@SuppressWarnings("unused")
public class QAuditDelta extends FlexibleRelationalPathBase<MAuditDelta> {

    private static final long serialVersionUID = -231012375;

    public static final String TABLE_NAME = "m_audit_delta";

    public static final ColumnMetadata RECORD_ID =
            ColumnMetadata.named("record_id").ofType(Types.BIGINT).withSize(19).notNull();
    public static final ColumnMetadata CHECKSUM =
            ColumnMetadata.named("checksum").ofType(Types.VARCHAR).withSize(32).notNull();
    public static final ColumnMetadata DELTA =
            ColumnMetadata.named("delta").ofType(Types.BINARY);
    public static final ColumnMetadata DELTA_OID =
            ColumnMetadata.named("deltaOid").ofType(Types.VARCHAR).withSize(36);
    public static final ColumnMetadata DELTA_TYPE =
            ColumnMetadata.named("deltaType").ofType(Types.INTEGER);
    public static final ColumnMetadata FULL_RESULT =
            ColumnMetadata.named("fullResult").ofType(Types.BINARY);
    public static final ColumnMetadata OBJECT_NAME_NORM =
            ColumnMetadata.named("objectName_norm").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata OBJECT_NAME_ORIG =
            ColumnMetadata.named("objectName_orig").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata RESOURCE_NAME_NORM =
            ColumnMetadata.named("resourceName_norm").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata RESOURCE_NAME_ORIG =
            ColumnMetadata.named("resourceName_orig").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata RESOURCE_OID =
            ColumnMetadata.named("resourceOid").ofType(Types.VARCHAR).withSize(36);
    public static final ColumnMetadata STATUS =
            ColumnMetadata.named("status").ofType(Types.INTEGER);

    // columns and relations
    public final NumberPath<Long> recordId = createLong("recordId", RECORD_ID);
    public final StringPath checksum = createString("checksum", CHECKSUM);
    public final ArrayPath<byte[], Byte> delta = createByteArray("delta", DELTA);
    public final StringPath deltaOid = createString("deltaOid", DELTA_OID);
    public final NumberPath<Integer> deltaType = createInteger("deltaType", DELTA_TYPE);
    public final ArrayPath<byte[], Byte> fullResult = createByteArray("fullResult", FULL_RESULT);
    public final StringPath objectNameNorm = createString("objectNameNorm", OBJECT_NAME_NORM);
    public final StringPath objectNameOrig = createString("objectNameOrig", OBJECT_NAME_ORIG);
    public final StringPath resourceNameNorm = createString("resourceNameNorm", RESOURCE_NAME_NORM);
    public final StringPath resourceNameOrig = createString("resourceNameOrig", RESOURCE_NAME_ORIG);
    public final StringPath resourceOid = createString("resourceOid", RESOURCE_OID);
    public final NumberPath<Integer> status = createInteger("status", STATUS);

    public final PrimaryKey<MAuditDelta> constraint85 = createPrimaryKey(recordId, checksum);
    public final ForeignKey<QAuditEventRecord> auditDeltaFk = createForeignKey(recordId, "ID");

    public QAuditDelta(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QAuditDelta(String variable, String schema, String table) {
        super(MAuditDelta.class, variable, schema, table);
    }
}
