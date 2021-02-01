/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel;

import com.querydsl.sql.PrimaryKey;

import com.evolveum.midpoint.repo.sqale.qbean.MArchetype;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QArchetype extends QObject<MArchetype> {

    private static final long serialVersionUID = -8367034620810300322L;

    public static final String TABLE_NAME = "m_archetype";

    // no additional columns and relations

    public final PrimaryKey<MArchetype> pk = createPrimaryKey(oid);

    public QArchetype(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QArchetype(String variable, String schema, String table) {
        super(MArchetype.class, variable, schema, table);
    }
}
