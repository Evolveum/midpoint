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
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.ForeignKey;
import com.querydsl.sql.PrimaryKey;

import com.evolveum.midpoint.repo.sql.pure.FlexibleRelationalPathBase;
import com.evolveum.midpoint.repo.sql.pure.querymodel.beans.MAuditRefValue;

/**
 * Querydsl query type for M_AUDIT_REF_VALUE table.
 */
@SuppressWarnings("unused")
public class QAuditRefValue extends FlexibleRelationalPathBase<MAuditRefValue> {

    private static final long serialVersionUID = 1173079757;

    public static final String TABLE_NAME = "m_audit_ref_value";

    public static final ColumnMetadata ID =
            ColumnMetadata.named("id").ofType(Types.BIGINT).withSize(19).notNull();
    public static final ColumnMetadata RECORD_ID =
            ColumnMetadata.named("record_id").ofType(Types.BIGINT).withSize(19);
    public static final ColumnMetadata NAME =
            ColumnMetadata.named("name").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata OID =
            ColumnMetadata.named("oid").ofType(Types.VARCHAR).withSize(36);
    public static final ColumnMetadata TARGET_NAME_NORM =
            ColumnMetadata.named("targetName_norm").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata TARGET_NAME_ORIG =
            ColumnMetadata.named("targetName_orig").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata TYPE =
            ColumnMetadata.named("type").ofType(Types.VARCHAR).withSize(255);

    public final NumberPath<Long> id = createLong("id", ID);
    public final NumberPath<Long> recordId = createLong("recordId", RECORD_ID);
    public final StringPath name = createString("name", NAME);
    public final StringPath oid = createString("oid", OID);
    public final StringPath targetNameNorm = createString("targetNameNorm", TARGET_NAME_NORM);
    public final StringPath targetNameOrig = createString("targetNameOrig", TARGET_NAME_ORIG);
    public final StringPath type = createString("type", TYPE);

    public final PrimaryKey<MAuditRefValue> constraint7 = createPrimaryKey(id);
    public final ForeignKey<QAuditEventRecord> auditRefValueFk = createForeignKey(recordId, "ID");

    public QAuditRefValue(String variable) {
        this(variable, "PUBLIC", TABLE_NAME);
    }

    public QAuditRefValue(String variable, String schema, String table) {
        super(MAuditRefValue.class, forVariable(variable), schema, table);
    }
}
