/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.pure.metamodel;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMAuditRefValue is a Querydsl query type for QMAuditRefValue
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMAuditRefValue extends com.querydsl.sql.RelationalPathBase<QMAuditRefValue> {

    private static final long serialVersionUID = 1173079757;

    public static final QMAuditRefValue mAuditRefValue = new QMAuditRefValue("M_AUDIT_REF_VALUE");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath name = createString("name");

    public final StringPath oid = createString("oid");

    public final NumberPath<Long> recordId = createNumber("recordId", Long.class);

    public final StringPath targetnameNorm = createString("targetnameNorm");

    public final StringPath targetnameOrig = createString("targetnameOrig");

    public final StringPath type = createString("type");

    public final com.querydsl.sql.PrimaryKey<QMAuditRefValue> constraint7 = createPrimaryKey(id);

    public final com.querydsl.sql.ForeignKey<QAuditEventRecord> auditRefValueFk = createForeignKey(recordId, "ID");

    public QMAuditRefValue(String variable) {
        super(QMAuditRefValue.class, forVariable(variable), "PUBLIC", "M_AUDIT_REF_VALUE");
        addMetadata();
    }

    public QMAuditRefValue(String variable, String schema, String table) {
        super(QMAuditRefValue.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMAuditRefValue(String variable, String schema) {
        super(QMAuditRefValue.class, forVariable(variable), schema, "M_AUDIT_REF_VALUE");
        addMetadata();
    }

    public QMAuditRefValue(Path<? extends QMAuditRefValue> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_AUDIT_REF_VALUE");
        addMetadata();
    }

    public QMAuditRefValue(PathMetadata metadata) {
        super(QMAuditRefValue.class, metadata, "PUBLIC", "M_AUDIT_REF_VALUE");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(id, ColumnMetadata.named("ID").withIndex(1).ofType(Types.BIGINT).withSize(19).notNull());
        addMetadata(name, ColumnMetadata.named("NAME").withIndex(2).ofType(Types.VARCHAR).withSize(255));
        addMetadata(oid, ColumnMetadata.named("OID").withIndex(3).ofType(Types.VARCHAR).withSize(36));
        addMetadata(recordId, ColumnMetadata.named("RECORD_ID").withIndex(4).ofType(Types.BIGINT).withSize(19));
        addMetadata(targetnameNorm, ColumnMetadata.named("TARGETNAME_NORM").withIndex(5).ofType(Types.VARCHAR).withSize(255));
        addMetadata(targetnameOrig, ColumnMetadata.named("TARGETNAME_ORIG").withIndex(6).ofType(Types.VARCHAR).withSize(255));
        addMetadata(type, ColumnMetadata.named("TYPE").withIndex(7).ofType(Types.VARCHAR).withSize(255));
    }

}

