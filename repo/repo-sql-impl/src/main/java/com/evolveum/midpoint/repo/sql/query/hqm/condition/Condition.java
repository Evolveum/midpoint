/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.query.hqm.condition;

import java.util.List;
import java.util.Objects;

import com.evolveum.midpoint.repo.sql.query.hqm.HibernateQuery;

/**
 * Condition in HQL.
 */
public abstract class Condition {

    protected final HibernateQuery hibernateQuery;

    public Condition(HibernateQuery hibernateQuery) {
        Objects.requireNonNull(hibernateQuery, "hibernateQuery");
        this.hibernateQuery = hibernateQuery;
    }

    public abstract void dumpToHql(StringBuilder sb, int indent);

    public static void dumpToHql(StringBuilder sb, List<Condition> conditions, int indent) {
        boolean first = true;
        for (Condition condition : conditions) {
            if (first) {
                first = false;
            } else {
                sb.append(" and\n");
            }
            condition.dumpToHql(sb, indent);
        }
    }
}
