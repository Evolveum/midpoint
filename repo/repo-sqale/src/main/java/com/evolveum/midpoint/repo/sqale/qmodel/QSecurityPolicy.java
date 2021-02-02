/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel;

import com.evolveum.midpoint.repo.sqale.qbean.MSecurityPolicy;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QSecurityPolicy extends QObject<MSecurityPolicy> {

    private static final long serialVersionUID = 289603300613404007L;

    public static final String TABLE_NAME = "m_security_policy";

    // no additional columns and relations

    public QSecurityPolicy(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QSecurityPolicy(String variable, String schema, String table) {
        super(MSecurityPolicy.class, variable, schema, table);
    }
}
