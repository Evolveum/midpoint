/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.audit.qmodel;

import static com.evolveum.midpoint.repo.sqale.jsonb.JsonbPath.JSONB_TYPE;

import java.io.Serial;
import java.sql.Types;
import java.time.Instant;

import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;

import com.evolveum.midpoint.repo.sqale.jsonb.JsonbPath;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * Querydsl query type for `MA_AUDIT_PAYLOAD` table.
 */
@SuppressWarnings("unused")
public class QAuditPayload extends FlexibleRelationalPathBase<MAuditPayload> {

    @Serial private static final long serialVersionUID = 8505357412345938642L;

    public static final String TABLE_NAME = "ma_audit_payload";

    public static final ColumnMetadata RECORD_ID =
            ColumnMetadata.named("recordId").ofType(Types.BIGINT).notNull();
    public static final ColumnMetadata TIMESTAMP =
            ColumnMetadata.named("timestamp").ofType(Types.TIMESTAMP_WITH_TIMEZONE).notNull();
    public static final ColumnMetadata ORDINAL =
            ColumnMetadata.named("ordinal").ofType(Types.INTEGER).notNull();
    public static final ColumnMetadata NAME =
            ColumnMetadata.named("name").ofType(Types.VARCHAR).notNull();
    public static final ColumnMetadata CONTENT_TYPE =
            ColumnMetadata.named("contentType").ofType(Types.VARCHAR);
    public static final ColumnMetadata CONTENT =
            ColumnMetadata.named("content").ofType(JSONB_TYPE);
    public static final ColumnMetadata SEARCHABLE_TEXT =
            ColumnMetadata.named("searchableText").ofType(Types.VARCHAR);

    public final NumberPath<Long> recordId = createLong("recordId", RECORD_ID);
    public final DateTimePath<Instant> timestamp = createInstant("timestamp", TIMESTAMP);
    public final NumberPath<Integer> ordinal = createInteger("ordinal", ORDINAL);
    public final StringPath name = createString("name", NAME);
    public final StringPath contentType = createString("contentType", CONTENT_TYPE);
    public final JsonbPath content =
            addMetadata(add(new JsonbPath(forProperty("content"))), CONTENT);
    public final StringPath searchableText = createString("searchableText", SEARCHABLE_TEXT);

    public QAuditPayload(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QAuditPayload(String variable, String schema, String table) {
        super(MAuditPayload.class, variable, schema, table);
    }
}
