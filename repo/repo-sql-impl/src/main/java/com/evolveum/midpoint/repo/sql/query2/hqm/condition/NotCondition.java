/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query2.hqm.condition;

import com.evolveum.midpoint.repo.sql.query2.hqm.HibernateQuery;
import com.evolveum.midpoint.repo.sql.query2.hqm.RootHibernateQuery;
import org.apache.commons.lang.Validate;

/**
 * @author mederly
 */
public class NotCondition extends Condition {

    protected Condition child;

    public NotCondition(RootHibernateQuery rootHibernateQuery, Condition child) {
        super(rootHibernateQuery);
        Validate.notNull(child, "child");
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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NotCondition that = (NotCondition) o;

        return child.equals(that.child);

    }

    @Override
    public int hashCode() {
        return child.hashCode();
    }
}
