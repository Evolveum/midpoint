/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query.hqm.condition;

import com.evolveum.midpoint.repo.sql.query.hqm.HibernateQuery;
import com.evolveum.midpoint.repo.sql.query.hqm.RootHibernateQuery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author mederly
 */
public abstract class JunctionCondition extends Condition {

    protected List<Condition> components = new ArrayList<>();

    public JunctionCondition(RootHibernateQuery rootHibernateQuery, Condition... conditions) {
        super(rootHibernateQuery);
        Collections.addAll(components, conditions);
    }

    public JunctionCondition(RootHibernateQuery rootHibernateQuery, Collection<Condition> conditions) {
        super(rootHibernateQuery);
        components.addAll(conditions);
    }

    public void add(Condition condition) {
        components.add(condition);
    }

    public void dumpToHql(StringBuilder sb, int indent, String logicalOperation) {
        if (components.isEmpty()) {
            // probably some programming bug
            throw new IllegalStateException("JunctionCondition with no components: " + this.getClass());
        } else if (components.size() == 1) {
            components.get(0).dumpToHql(sb, indent);
        } else {
            HibernateQuery.indent(sb, indent);
            sb.append("(\n");
            boolean first = true;
            for (Condition component : components) {
                if (first) {
                    first = false;
                } else {
                    sb.append(" ").append(logicalOperation).append("\n");
                }
                component.dumpToHql(sb, indent + 1);
            }
            sb.append("\n");
            HibernateQuery.indent(sb, indent);
            sb.append(")");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JunctionCondition that = (JunctionCondition) o;

        return components.equals(that.components);
    }

    @Override
    public int hashCode() {
        return components.hashCode();
    }
}
