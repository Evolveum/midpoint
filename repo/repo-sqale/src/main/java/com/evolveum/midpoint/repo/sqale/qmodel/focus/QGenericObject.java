/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.focus;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QGenericObject extends QFocus<MFocus> {

    private static final long serialVersionUID = -8447131528511969285L;

    public static final String TABLE_NAME = "m_generic_object";

    public QGenericObject(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QGenericObject(String variable, String schema, String table) {
        super(MFocus.class, variable, schema, table);
    }
}
