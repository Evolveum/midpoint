/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.other;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolder;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QObjectTemplate extends QAssignmentHolder<MObject> {

    private static final long serialVersionUID = 5428353336949587877L;

    public static final String TABLE_NAME = "m_object_template";

    // no additional columns and relations

    public QObjectTemplate(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QObjectTemplate(String variable, String schema, String table) {
        super(MObject.class, variable, schema, table);
    }
}
