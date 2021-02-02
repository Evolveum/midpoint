/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel;

import com.evolveum.midpoint.repo.sqale.qbean.MValuePolicy;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QValuePolicy extends QObject<MValuePolicy> {

    private static final long serialVersionUID = 5623917383769868004L;

    public static final String TABLE_NAME = "m_value_policy";

    // no additional columns and relations

    public QValuePolicy(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QValuePolicy(String variable, String schema, String table) {
        super(MValuePolicy.class, variable, schema, table);
    }
}
