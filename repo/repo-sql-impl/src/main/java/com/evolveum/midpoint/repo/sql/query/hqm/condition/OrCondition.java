/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.query.hqm.condition;

import com.evolveum.midpoint.repo.sql.query.hqm.HibernateQuery;

public class OrCondition extends JunctionCondition {

    public OrCondition(HibernateQuery hibernateQuery, Condition... conditions) {
        super(hibernateQuery, conditions);
    }

    @Override
    public void dumpToHql(StringBuilder sb, int indent) {
        super.dumpToHql(sb, indent, "or");
    }

    // inherited "equals" is OK
}
