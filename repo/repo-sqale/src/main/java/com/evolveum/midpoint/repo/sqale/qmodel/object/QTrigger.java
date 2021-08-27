/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.object;

import java.sql.Types;
import java.time.Instant;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.sql.ColumnMetadata;

import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainer;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QTrigger<OR extends MObject> extends QContainer<MTrigger, OR> {

    private static final long serialVersionUID = 2478404102829142213L;

    /**
     * If `QTrigger.class` is not enough because of generics, try `QTrigger.CLASS`.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static final Class<QTrigger<MObject>> CLASS = (Class) QTrigger.class;

    public static final String TABLE_NAME = "m_trigger";

    public static final ColumnMetadata HANDLER_URI_ID =
            ColumnMetadata.named("handlerUriId").ofType(Types.INTEGER);
    public static final ColumnMetadata TIMESTAMP =
            ColumnMetadata.named("timestamp").ofType(Types.TIMESTAMP_WITH_TIMEZONE);

    // attributes

    public final NumberPath<Integer> handlerUriId = createInteger("handlerUriId", HANDLER_URI_ID);
    public final DateTimePath<Instant> timestamp = createInstant("timestamp", TIMESTAMP);

    public QTrigger(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QTrigger(String variable, String schema, String table) {
        super(MTrigger.class, variable, schema, table);
    }

    @Override
    public BooleanExpression isOwnedBy(OR ownerRow) {
        return ownerOid.eq(ownerRow.oid);
    }
}
