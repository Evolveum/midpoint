/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.audit.qmodel;

import java.sql.Types;
import java.time.Instant;

import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.EnumPath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;

/**
 * Querydsl query type for `MA_AUDIT_REF` table.
 */
@SuppressWarnings("unused")
public class QAuditRefValue extends FlexibleRelationalPathBase<MAuditRefValue> {

    private static final long serialVersionUID = 2778508481708634421L;

    public static final String TABLE_NAME = "ma_audit_ref";

    public static final ColumnMetadata ID =
            ColumnMetadata.named("id").ofType(Types.BIGINT).notNull();
    public static final ColumnMetadata RECORD_ID =
            ColumnMetadata.named("recordId").ofType(Types.BIGINT).notNull();
    public static final ColumnMetadata TIMESTAMP =
            ColumnMetadata.named("timestamp").ofType(Types.TIMESTAMP_WITH_TIMEZONE).notNull();
    public static final ColumnMetadata NAME =
            ColumnMetadata.named("name").ofType(Types.VARCHAR);
    public static final ColumnMetadata TARGET_OID =
            ColumnMetadata.named("targetOid").ofType(UuidPath.UUID_TYPE);
    public static final ColumnMetadata TARGET_TYPE =
            ColumnMetadata.named("targetType").ofType(Types.OTHER);
    public static final ColumnMetadata TARGET_NAME_NORM =
            ColumnMetadata.named("targetNameNorm").ofType(Types.VARCHAR);
    public static final ColumnMetadata TARGET_NAME_ORIG =
            ColumnMetadata.named("targetNameOrig").ofType(Types.VARCHAR);

    public final NumberPath<Long> id = createLong("id", ID);
    public final NumberPath<Long> recordId = createLong("recordId", RECORD_ID);
    public final DateTimePath<Instant> timestamp = createInstant("timestamp", TIMESTAMP);
    public final StringPath name = createString("name", NAME);
    public final UuidPath targetOid = createUuid("targetOid", TARGET_OID);
    public final StringPath targetNameNorm = createString("targetNameNorm", TARGET_NAME_NORM);
    public final StringPath targetNameOrig = createString("targetNameOrig", TARGET_NAME_ORIG);
    public final EnumPath<MObjectType> targetType =
            createEnum("targetType", MObjectType.class, TARGET_TYPE);

    public QAuditRefValue(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QAuditRefValue(String variable, String schema, String table) {
        super(MAuditRefValue.class, variable, schema, table);
    }
}
