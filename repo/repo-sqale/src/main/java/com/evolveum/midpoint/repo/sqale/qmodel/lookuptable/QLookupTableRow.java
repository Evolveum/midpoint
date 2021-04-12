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
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.PrimaryKey;

import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainer;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QLookupTableRow extends QContainer<MLookupTableRow> {

    private static final long serialVersionUID = -9105517154692783998L;

    public static final String TABLE_NAME = "m_lookup_table_row";

    public static final ColumnMetadata KEY =
            ColumnMetadata.named("key").ofType(Types.VARCHAR);
    public static final ColumnMetadata VALUE =
            ColumnMetadata.named("value").ofType(Types.VARCHAR);
    public static final ColumnMetadata LABEL_NORM =
            ColumnMetadata.named("label_norm").ofType(Types.VARCHAR);
    public static final ColumnMetadata LABEL_ORIG =
            ColumnMetadata.named("label_orig").ofType(Types.VARCHAR);
    public static final ColumnMetadata LAST_CHANGE_TIMESTAMP =
            ColumnMetadata.named("lastChangeTimestamp").ofType(Types.TIMESTAMP_WITH_TIMEZONE);

    public StringPath key = createString("key", KEY);
    public StringPath value = createString("value", VALUE);
    public StringPath labelNorm = createString("labelNorm", LABEL_NORM);
    public StringPath labelOrig = createString("labelOrig", LABEL_ORIG);
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
