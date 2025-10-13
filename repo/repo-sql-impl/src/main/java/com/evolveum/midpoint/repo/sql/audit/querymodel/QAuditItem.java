/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.audit.querymodel;

import java.sql.Types;

import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.ForeignKey;
import com.querydsl.sql.PrimaryKey;

import com.evolveum.midpoint.repo.sql.audit.beans.MAuditItem;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * Querydsl query type for M_AUDIT_ITEM table.
 */
@SuppressWarnings("unused")
public class QAuditItem extends FlexibleRelationalPathBase<MAuditItem> {

    private static final long serialVersionUID = -838572862;

    public static final String TABLE_NAME = "m_audit_item";

    public static final ColumnMetadata RECORD_ID =
            ColumnMetadata.named("record_id").ofType(Types.BIGINT).withSize(19).notNull();
    public static final ColumnMetadata CHANGED_ITEM_PATH =
            ColumnMetadata.named("changedItemPath").ofType(Types.VARCHAR).withSize(255).notNull();

    public final NumberPath<Long> recordId = createLong("recordId", RECORD_ID);
    public final StringPath changedItemPath = createString("changedItemPath", CHANGED_ITEM_PATH);

    public final PrimaryKey<MAuditItem> constraint1 = createPrimaryKey(recordId, changedItemPath);
    public final ForeignKey<QAuditEventRecord> auditItemFk = createForeignKey(recordId, "ID");

    public QAuditItem(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QAuditItem(String variable, String schema, String table) {
        super(MAuditItem.class, variable, schema, table);
    }
}
