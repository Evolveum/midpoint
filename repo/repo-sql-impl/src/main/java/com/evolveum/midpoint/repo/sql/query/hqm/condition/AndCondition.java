/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.query.hqm.condition;

import com.evolveum.midpoint.repo.sql.query.hqm.HibernateQuery;

import java.util.Collection;

public class AndCondition extends JunctionCondition {

    public AndCondition(HibernateQuery rootHibernateQuery, Condition... conditions) {
        super(rootHibernateQuery, conditions);
    }

    public AndCondition(HibernateQuery rootHibernateQuery, Collection<Condition> conditions) {
        super(rootHibernateQuery, conditions);
    }

    @Override
    public void dumpToHql(StringBuilder sb, int indent) {
        super.dumpToHql(sb, indent, "and");
    }

    // inherited "equals" is OK
}
