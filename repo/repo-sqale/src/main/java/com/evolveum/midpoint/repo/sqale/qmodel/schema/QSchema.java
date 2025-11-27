/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.schema;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;

public class QSchema extends QObject<MObject> {

    private static final long serialVersionUID = -2157392986065893792L;

    public static final String TABLE_NAME = "m_schema";

    // no additional columns and relations

    public QSchema(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QSchema(String variable, String schema, String table) {
        super(MObject.class, variable, schema, table);
    }
}
