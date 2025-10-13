/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.query.hqm.condition;

import com.evolveum.midpoint.repo.sql.query.hqm.HibernateQuery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class JunctionCondition extends Condition {

    protected List<Condition> components = new ArrayList<>();

    public JunctionCondition(HibernateQuery rootHibernateQuery, Condition... conditions) {
        super(rootHibernateQuery);
        Collections.addAll(components, conditions);
    }

    public JunctionCondition(HibernateQuery rootHibernateQuery, Collection<Condition> conditions) {
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
