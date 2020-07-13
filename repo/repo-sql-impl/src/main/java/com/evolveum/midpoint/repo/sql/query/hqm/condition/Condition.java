/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query.hqm.condition;

import java.util.List;
import java.util.Objects;

import com.evolveum.midpoint.repo.sql.query.hqm.RootHibernateQuery;

/**
 * Condition in HQL.
 *
 * @author mederly
 */
public abstract class Condition {

    protected final RootHibernateQuery rootHibernateQuery;

    public Condition(RootHibernateQuery rootHibernateQuery) {
        Objects.requireNonNull(rootHibernateQuery, "rootHibernateQuery");
        this.rootHibernateQuery = rootHibernateQuery;
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
