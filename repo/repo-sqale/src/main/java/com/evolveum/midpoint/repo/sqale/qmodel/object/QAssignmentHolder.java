/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.object;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
public class QAssignmentHolder<R extends MObject> extends QObject<R> {

    private static final long serialVersionUID = -8772807624205702543L;

    /**
     * If {@code QAssignmentHolder.class} is not enough because of generics,
     * try {@code QAssignmentHolder.CLASS}.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static final Class<QAssignmentHolder<MObject>> CLASS =
            (Class) QAssignmentHolder.class;

    public static final String TABLE_NAME = "m_assignment_holder";

    public QAssignmentHolder(Class<R> type, String variable) {
        this(type, variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QAssignmentHolder(Class<R> type, String variable, String schema, String table) {
        super(type, variable, schema, table);
    }
}
