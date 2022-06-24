/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query.hqm.condition;

import java.util.Collection;

import com.evolveum.midpoint.repo.sql.query.hqm.HibernateQuery;

public class AndCondition extends JunctionCondition {

    public AndCondition(HibernateQuery hibernateQuery, Condition... conditions) {
        super(hibernateQuery, conditions);
    }

    public AndCondition(HibernateQuery hibernateQuery, Collection<Condition> conditions) {
        super(hibernateQuery, conditions);
    }

    @Override
    public void dumpToHql(StringBuilder sb, int indent) {
        super.dumpToHql(sb, indent, "and");
    }

    // inherited "equals" is OK
}
