/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel;

import com.evolveum.midpoint.repo.sqale.qbean.MDashboard;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QDashboard extends QObject<MDashboard> {

    private static final long serialVersionUID = -3546780348548754579L;

    public static final String TABLE_NAME = "m_dashboard";

    // no additional columns and relations

    public QDashboard(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QDashboard(String variable, String schema, String table) {
        super(MDashboard.class, variable, schema, table);
    }
}
