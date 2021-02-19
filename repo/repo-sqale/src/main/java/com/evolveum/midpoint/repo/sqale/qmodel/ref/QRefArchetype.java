/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.ref;

/**
 * Querydsl query type for {@value #TABLE_NAME} table that contains refs to archetypes.
 */
public class QRefArchetype extends QReference {

    private static final long serialVersionUID = -953946255884007875L;

    public static final String TABLE_NAME = "m_ref_archetype";
    public static final String DEFAULT_ALIAS_NAME = "refa";

    public QRefArchetype(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QRefArchetype(String variable, String schema, String table) {
        super(variable, schema, table);
    }
}
