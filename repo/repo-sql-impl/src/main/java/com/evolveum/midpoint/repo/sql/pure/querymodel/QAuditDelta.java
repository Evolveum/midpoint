/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.pure.querymodel;

import static com.querydsl.core.types.PathMetadataFactory.forVariable;

import java.sql.Types;

import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.SimplePath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.ForeignKey;
import com.querydsl.sql.PrimaryKey;

import com.evolveum.midpoint.repo.sql.pure.FlexibleRelationalPathBase;
import com.evolveum.midpoint.repo.sql.pure.querymodel.beans.MAuditDelta;

/**
 * Querydsl query type for M_AUDIT_DELTA table.
 */
@SuppressWarnings("unused")
public class QAuditDelta extends FlexibleRelationalPathBase<MAuditDelta> {

    private static final long serialVersionUID = -231012375;

    public static final String TABLE_NAME = "m_audit_delta";

    public static final ColumnMetadata CHECKSUM =
            ColumnMetadata.named("CHECKSUM").ofType(Types.VARCHAR).withSize(32).notNull();
    public static final ColumnMetadata DELTA =
            ColumnMetadata.named("DELTA").ofType(Types.BLOB).withSize(2147483647);
    public static final ColumnMetadata DELTA_OID =
            ColumnMetadata.named("DELTAOID").ofType(Types.VARCHAR).withSize(36);
    public static final ColumnMetadata DELTA_TYPE =
            ColumnMetadata.named("DELTATYPE").ofType(Types.INTEGER).withSize(10);
    public static final ColumnMetadata FULL_RESULT =
            ColumnMetadata.named("FULLRESULT").ofType(Types.BLOB).withSize(2147483647);
    public static final ColumnMetadata OBJECT_NAME_NORM =
            ColumnMetadata.named("OBJECTNAME_NORM").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata OBJECT_NAME_ORIG =
            ColumnMetadata.named("OBJECTNAME_ORIG").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata RECORD_ID =
            ColumnMetadata.named("RECORD_ID").ofType(Types.BIGINT).withSize(19).notNull();
    public static final ColumnMetadata RESOURCE_NAME_NORM =
            ColumnMetadata.named("RESOURCENAME_NORM").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata RESOURCE_NAME_ORIG =
            ColumnMetadata.named("RESOURCENAME_ORIG").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata RESOURCE_OID =
            ColumnMetadata.named("RESOURCEOID").ofType(Types.VARCHAR).withSize(36);
    public static final ColumnMetadata STATUS =
            ColumnMetadata.named("STATUS").ofType(Types.INTEGER).withSize(10);

    // columns and relations
    public final StringPath checksum = createString("checksum", CHECKSUM);
    public final SimplePath<java.sql.Blob> delta = createBlob("delta", DELTA);
    public final StringPath deltaOid = createString("deltaOid", DELTA_OID);
    public final NumberPath<Integer> deltaType = createInteger("deltaType", DELTA_TYPE);
    public final SimplePath<java.sql.Blob> fullResult = createBlob("fullResult", FULL_RESULT);
    public final StringPath objectNameNorm = createString("objectNameNorm", OBJECT_NAME_NORM);
    public final StringPath objectNameOrig = createString("objectNameOrig", OBJECT_NAME_ORIG);
    public final NumberPath<Long> recordId = createLong("recordId", RECORD_ID);
    public final StringPath resourceNameNorm = createString("resourceNameNorm", RESOURCE_NAME_NORM);
    public final StringPath resourceNameOrig = createString("resourceNameOrig", RESOURCE_NAME_ORIG);
    public final StringPath resourceOid = createString("resourceOid", RESOURCE_OID);
    public final NumberPath<Integer> status = createInteger("status", STATUS);

    public final PrimaryKey<MAuditDelta> constraint85 = createPrimaryKey(checksum, recordId);
    public final ForeignKey<QAuditEventRecord> auditDeltaFk = createForeignKey(recordId, "ID");

    public QAuditDelta(String variable) {
        this(variable, "PUBLIC", TABLE_NAME);
    }

    public QAuditDelta(String variable, String schema, String table) {
        super(MAuditDelta.class, forVariable(variable), schema, table);
    }
}
