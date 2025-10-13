/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.common;

import java.sql.Types;

import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.PrimaryKey;

import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * Querydsl query type for {@value #TABLE_NAME} table that contains repetitive URIs (e.g. channels).
 * This entity is not registered to any schema type so it doesn't have related mapping class.
 * Use {@link #DEFAULT} for default alias directly.
 */
public class QUri extends FlexibleRelationalPathBase<MUri> {

    private static final long serialVersionUID = -1519824042438215508L;

    public static final String TABLE_NAME = "m_uri";

    public static final QUri DEFAULT = new QUri("uri");

    public static final ColumnMetadata ID =
            ColumnMetadata.named("id").ofType(Types.INTEGER).notNull();
    public static final ColumnMetadata URI =
            ColumnMetadata.named("uri").ofType(Types.VARCHAR).notNull();

    public final NumberPath<Integer> id = createInteger("id", ID);
    public final StringPath uri = createString("uri", URI);

    public final PrimaryKey<MUri> pk = createPrimaryKey(id);

    public QUri(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QUri(String variable, String schema, String table) {
        super(MUri.class, variable, schema, table);
    }
}
