/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.lookuptable;

import java.sql.Types;
import java.time.Instant;

import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.PrimaryKey;

import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QLookupTableRow extends FlexibleRelationalPathBase<MLookupTableRow> {

    private static final long serialVersionUID = -9105517154692783998L;

    public static final String TABLE_NAME = "m_lookup_table_row";

    public static final ColumnMetadata OWNER_OID =
            ColumnMetadata.named("owner_oid").ofType(UuidPath.UUID_TYPE);
    public static final ColumnMetadata ROW_ID =
            ColumnMetadata.named("cid").ofType(Types.INTEGER);
    public static final ColumnMetadata ROW_KEY =
            ColumnMetadata.named("row_key").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata LABEL_NORM =
            ColumnMetadata.named("label_norm").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata LABEL_ORIG =
            ColumnMetadata.named("label_orig").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata ROW_VALUE =
            ColumnMetadata.named("row_value").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata LAST_CHANGE_TIMESTAMP =
            ColumnMetadata.named("lastChangeTimestamp").ofType(Types.TIMESTAMP_WITH_TIMEZONE);

    public UuidPath ownerOid = createUuid("ownerOid", OWNER_OID);
    public NumberPath<Integer> cid = createInteger("cid", ROW_ID);
    public StringPath rowKey = createString("rowKey", ROW_KEY);
    public StringPath labelNorm = createString("labelNorm", LABEL_NORM);
    public StringPath labelOrig = createString("labelOrig", LABEL_ORIG);
    public StringPath rowValue = createString("rowValue", ROW_VALUE);
    public DateTimePath<Instant> lastChangeTimestamp =
            createInstant("lastChangeTimestamp", LAST_CHANGE_TIMESTAMP);

    public final PrimaryKey<MLookupTableRow> pk = createPrimaryKey(ownerOid, cid);

    public QLookupTableRow(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QLookupTableRow(String variable, String schema, String table) {
        super(MLookupTableRow.class, variable, schema, table);
    }
}
