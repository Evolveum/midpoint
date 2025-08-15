/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.role;

public class QApplication extends QAbstractRole<MApplication> {

    private static final long serialVersionUID = 3847501938475019384L;

    public static final String TABLE_NAME = "m_application";

    // columns and relations

    public QApplication(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QApplication(String variable, String schema, String table) {
        super(MApplication.class, variable, schema, table);
    }
}
