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
import com.evolveum.midpoint.repo.sql.pure.querymodel.beans.MAuditPropertyValue;

/**
 * Querydsl query type for M_AUDIT_PROP_VALUE table.
 */
@SuppressWarnings("unused")
public class QAuditPropertyValue extends FlexibleRelationalPathBase<MAuditPropertyValue> {

    private static final long serialVersionUID = -1656131713;

    public static final String TABLE_NAME = "m_audit_prop_value";

    public static final ColumnMetadata ID =
            ColumnMetadata.named("ID").ofType(Types.BIGINT).withSize(19).notNull();
    public static final ColumnMetadata RECORD_ID =
            ColumnMetadata.named("RECORD_ID").ofType(Types.BIGINT).withSize(19);
    public static final ColumnMetadata NAME =
            ColumnMetadata.named("NAME").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata VALUE =
            ColumnMetadata.named("VALUE").ofType(Types.VARCHAR).withSize(1024);

    public final NumberPath<Long> id = createLong("id", ID);
    public final NumberPath<Long> recordId = createLong("recordId", RECORD_ID);
    public final StringPath name = createString("name", NAME);
    public final StringPath value = createString("value", VALUE);

    public final PrimaryKey<MAuditPropertyValue> constraint6 = createPrimaryKey(id);
    public final ForeignKey<QAuditEventRecord> auditPropValueFk = createForeignKey(recordId, "ID");

    public QAuditPropertyValue(String variable) {
        this(variable, "PUBLIC", TABLE_NAME);
    }

    public QAuditPropertyValue(String variable, String schema, String table) {
        super(MAuditPropertyValue.class, forVariable(variable), schema, table);
    }
}

