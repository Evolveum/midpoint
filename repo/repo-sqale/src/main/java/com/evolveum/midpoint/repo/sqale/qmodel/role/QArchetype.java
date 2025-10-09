/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.role;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QArchetype extends QAbstractRole<MArchetype> {

    private static final long serialVersionUID = -8367034620810300322L;

    public static final String TABLE_NAME = "m_archetype";

    // no additional columns and relations

    public QArchetype(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QArchetype(String variable, String schema, String table) {
        super(MArchetype.class, variable, schema, table);
    }
}
