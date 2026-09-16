/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.mining.session;

import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolder;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QSessionData extends QAssignmentHolder<MSessionObject> {

    public static final String TABLE_NAME = "m_role_analysis_session";

    public QSessionData(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QSessionData(String variable, String schema, String table) {
        super(MSessionObject.class, variable, schema, table);
    }

}
