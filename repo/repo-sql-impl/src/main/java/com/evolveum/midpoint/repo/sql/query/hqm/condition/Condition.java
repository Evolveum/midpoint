/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query.hqm.condition;

import com.evolveum.midpoint.repo.sql.query.hqm.RootHibernateQuery;
import org.apache.commons.lang.Validate;

import java.util.List;

/**
 * Condition in HQL.
 *
 * @author mederly
 */
public abstract class Condition {

    protected RootHibernateQuery rootHibernateQuery;

    public Condition(RootHibernateQuery rootHibernateQuery) {
        Validate.notNull(rootHibernateQuery, "rootHibernateQuery");
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
