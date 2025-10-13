/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.query.hqm.condition;

import java.util.Objects;

import com.evolveum.midpoint.repo.sql.query.hqm.HibernateQuery;

public class NotCondition extends Condition {

    protected Condition child;

    public NotCondition(HibernateQuery rootHibernateQuery, Condition child) {
        super(rootHibernateQuery);
        Objects.requireNonNull(child, "child");
        this.child = child;
    }

    @Override
    public void dumpToHql(StringBuilder sb, int indent) {
        HibernateQuery.indent(sb, indent);
        sb.append("not ");
        child.dumpToHql(sb, -1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        NotCondition that = (NotCondition) o;

        return child.equals(that.child);

    }

    @Override
    public int hashCode() {
        return child.hashCode();
    }
}
