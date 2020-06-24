/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.metamodel;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMAuditResource is a Querydsl query type for QMAuditResource
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMAuditResource extends com.querydsl.sql.RelationalPathBase<QMAuditResource> {

    private static final long serialVersionUID = 1568947773;

    public static final QMAuditResource mAuditResource = new QMAuditResource("M_AUDIT_RESOURCE");

    public final NumberPath<Long> recordId = createNumber("recordId", Long.class);

    public final StringPath resourceoid = createString("resourceoid");

    public final com.querydsl.sql.PrimaryKey<QMAuditResource> constraint84 = createPrimaryKey(recordId, resourceoid);

    public final com.querydsl.sql.ForeignKey<QAuditEventRecord> auditResourceFk = createForeignKey(recordId, "ID");

    public QMAuditResource(String variable) {
        super(QMAuditResource.class, forVariable(variable), "PUBLIC", "M_AUDIT_RESOURCE");
        addMetadata();
    }

    public QMAuditResource(String variable, String schema, String table) {
        super(QMAuditResource.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMAuditResource(String variable, String schema) {
        super(QMAuditResource.class, forVariable(variable), schema, "M_AUDIT_RESOURCE");
        addMetadata();
    }

    public QMAuditResource(Path<? extends QMAuditResource> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_AUDIT_RESOURCE");
        addMetadata();
    }

    public QMAuditResource(PathMetadata metadata) {
        super(QMAuditResource.class, metadata, "PUBLIC", "M_AUDIT_RESOURCE");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(recordId, ColumnMetadata.named("RECORD_ID").withIndex(1).ofType(Types.BIGINT).withSize(19).notNull());
        addMetadata(resourceoid, ColumnMetadata.named("RESOURCEOID").withIndex(2).ofType(Types.VARCHAR).withSize(255).notNull());
    }

}

