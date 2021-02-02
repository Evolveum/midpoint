/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
 */
public class QQName extends FlexibleRelationalPathBase<MQName> {

    private static final long serialVersionUID = -1519824042438215508L;

    public static final String TABLE_NAME = "m_qname";

    public static final ColumnMetadata ID =
            ColumnMetadata.named("id").ofType(Types.INTEGER).notNull();
    public static final ColumnMetadata URI =
            ColumnMetadata.named("uri").ofType(Types.VARCHAR).withSize(255).notNull();

    public final NumberPath<Integer> id = createInteger("id", ID);
    public final StringPath uri = createString("uri", URI);

    public final PrimaryKey<MQName> pk = createPrimaryKey(id);

    public QQName(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QQName(String variable, String schema, String table) {
        super(MQName.class, variable, schema, table);
    }
}
