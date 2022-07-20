/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.querydsl;

import com.querydsl.core.types.Operator;

/**
 * Set of operators supported by midPoint Native repository in addition to standard
 * PG template available in Querydsl.
 *
 * Operations and functions used here are documented on the following pages:
 *
 * * https://www.postgresql.org/docs/current/fuzzystrmatch.html
 *
 * TODO: If this is not used and string templates are generated in the filters directly, it can be removed
 *  including the test using this.
 */
public enum SqaleOps implements Operator {

    /** 2-parameter version of levenshtein() function. */
    LEVENSHTEIN2(Integer.class);

    private final Class<?> type;

    SqaleOps(Class<?> type) {
        this.type = type;
    }

    @Override
    public Class<?> getType() {
        return type;
    }
}
