/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query.hqm.condition;

import com.evolveum.midpoint.repo.sql.query.hqm.RootHibernateQuery;

/**
 * @author mederly
 */
public class OrCondition extends JunctionCondition {

    public OrCondition(RootHibernateQuery rootHibernateQuery, Condition... conditions) {
        super(rootHibernateQuery, conditions);
    }

    @Override
    public void dumpToHql(StringBuilder sb, int indent) {
        super.dumpToHql(sb, indent, "or");
    }

    // inherited "equals" is OK
}
