/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.object;

import java.sql.Types;
import java.time.Instant;

import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.sql.ColumnMetadata;

import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainer;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QTrigger extends QContainer<MTrigger> {

    private static final long serialVersionUID = 2478404102829142213L;

    public static final String TABLE_NAME = "m_trigger";

    public static final ColumnMetadata HANDLER_URI_ID =
            ColumnMetadata.named("handlerUri_id").ofType(Types.INTEGER);
    public static final ColumnMetadata TIMESTAMP_VALUE =
            ColumnMetadata.named("timestampValue").ofType(Types.TIMESTAMP_WITH_TIMEZONE);

    // attributes

    public final NumberPath<Integer> handlerUriId = createInteger("handlerUriId", HANDLER_URI_ID);
    public final DateTimePath<Instant> timestampValue =
            createInstant("timestampValue", TIMESTAMP_VALUE);

    public QTrigger(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QTrigger(String variable, String schema, String table) {
        super(MTrigger.class, variable, schema, table);
    }
}
