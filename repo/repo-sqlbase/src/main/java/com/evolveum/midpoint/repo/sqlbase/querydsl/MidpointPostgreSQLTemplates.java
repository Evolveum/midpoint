/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
