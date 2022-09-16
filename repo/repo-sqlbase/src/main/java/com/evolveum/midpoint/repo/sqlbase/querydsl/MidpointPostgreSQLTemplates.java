/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.querydsl;

import com.querydsl.sql.PostgreSQLTemplates;

/**
 * Extension to the default Querydsl PostgreSQL template adding operations used by midPoint.
 */
public class MidpointPostgreSQLTemplates extends PostgreSQLTemplates {

    public static final MidpointPostgreSQLTemplates DEFAULT = new MidpointPostgreSQLTemplates();

    public MidpointPostgreSQLTemplates() {
        // Nothing special at this moment, we construct these functions dynamically in code.
//        add(SqaleOps.LEVENSHTEIN2, "levenshtein({0}, {1})");
    }
}
