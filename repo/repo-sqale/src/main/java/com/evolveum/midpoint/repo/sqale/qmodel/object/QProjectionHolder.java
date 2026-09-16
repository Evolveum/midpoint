/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.object;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
public class QProjectionHolder<R extends MObject> extends QAssignmentHolder<R> {

    private static final long serialVersionUID = 4920460723180724361L;

    /**
     * If {@code QProjectionHolder.class} is not enough because of generics,
     * try {@code QProjectionHolder.CLASS}.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static final Class<QProjectionHolder<MObject>> CLASS =
            (Class) QProjectionHolder.class;

    public static final String TABLE_NAME = "m_projection_holder";

    public QProjectionHolder(Class<R> type, String variable) {
        this(type, variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QProjectionHolder(Class<R> type, String variable, String schema, String table) {
        super(type, variable, schema, table);
    }
}
