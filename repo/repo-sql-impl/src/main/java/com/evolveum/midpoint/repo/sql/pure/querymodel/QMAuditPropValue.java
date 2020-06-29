/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.pure.querymodel;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMAuditPropValue is a Querydsl query type for QMAuditPropValue
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMAuditPropValue extends com.querydsl.sql.RelationalPathBase<QMAuditPropValue> {

    private static final long serialVersionUID = -1656131713;

    public static final QMAuditPropValue mAuditPropValue = new QMAuditPropValue("M_AUDIT_PROP_VALUE");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath name = createString("name");

    public final NumberPath<Long> recordId = createNumber("recordId", Long.class);

    public final StringPath value = createString("value");

    public final com.querydsl.sql.PrimaryKey<QMAuditPropValue> constraint6 = createPrimaryKey(id);

    public final com.querydsl.sql.ForeignKey<QAuditEventRecord> auditPropValueFk = createForeignKey(recordId, "ID");

    public QMAuditPropValue(String variable) {
        super(QMAuditPropValue.class, forVariable(variable), "PUBLIC", "M_AUDIT_PROP_VALUE");
        addMetadata();
    }

    public QMAuditPropValue(String variable, String schema, String table) {
        super(QMAuditPropValue.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMAuditPropValue(String variable, String schema) {
        super(QMAuditPropValue.class, forVariable(variable), schema, "M_AUDIT_PROP_VALUE");
        addMetadata();
    }

    public QMAuditPropValue(Path<? extends QMAuditPropValue> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_AUDIT_PROP_VALUE");
        addMetadata();
    }

    public QMAuditPropValue(PathMetadata metadata) {
        super(QMAuditPropValue.class, metadata, "PUBLIC", "M_AUDIT_PROP_VALUE");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(id, ColumnMetadata.named("ID").withIndex(1).ofType(Types.BIGINT).withSize(19).notNull());
        addMetadata(name, ColumnMetadata.named("NAME").withIndex(2).ofType(Types.VARCHAR).withSize(255));
        addMetadata(recordId, ColumnMetadata.named("RECORD_ID").withIndex(3).ofType(Types.BIGINT).withSize(19));
        addMetadata(value, ColumnMetadata.named("VALUE").withIndex(4).ofType(Types.VARCHAR).withSize(1024));
    }

}

