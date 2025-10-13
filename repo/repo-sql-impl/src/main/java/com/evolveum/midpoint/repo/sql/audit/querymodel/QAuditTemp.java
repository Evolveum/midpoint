/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.audit.querymodel;

import java.sql.Types;

import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.PrimaryKey;

import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * Querydsl query type for audit temporary table used for batch deletions.
 */
public class QAuditTemp extends FlexibleRelationalPathBase<QAuditTemp> {

    private static final long serialVersionUID = 5917331012600618479L;

    public static final ColumnMetadata ID =
            ColumnMetadata.named("id").ofType(Types.BIGINT).withSize(19).notNull();

    // columns and relations
    public final NumberPath<Long> id = createLong("id", ID);

    public final PrimaryKey<QAuditTemp> pk = createPrimaryKey(id);

    public QAuditTemp(String variable, String table) {
        this(variable, DEFAULT_SCHEMA_NAME, table);
    }

    public QAuditTemp(String variable, String schema, String table) {
        super(QAuditTemp.class, variable, schema, table);
    }
}
