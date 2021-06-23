/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.report;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolder;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QReport extends QAssignmentHolder<MObject> {

    private static final long serialVersionUID = -5738006878845987541L;

    public static final String TABLE_NAME = "m_report";

    // no additional columns and relations

    public QReport(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QReport(String variable, String schema, String table) {
        super(MObject.class, variable, schema, table);
    }
}
