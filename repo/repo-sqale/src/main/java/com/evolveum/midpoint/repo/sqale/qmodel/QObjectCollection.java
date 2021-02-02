/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel;

import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QObjectCollection extends QObject<MObjectCollection> {

    private static final long serialVersionUID = -62230817260259478L;

    public static final String TABLE_NAME = "m_object_collection";

    // no additional columns and relations

    public QObjectCollection(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QObjectCollection(String variable, String schema, String table) {
        super(MObjectCollection.class, variable, schema, table);
    }
}
