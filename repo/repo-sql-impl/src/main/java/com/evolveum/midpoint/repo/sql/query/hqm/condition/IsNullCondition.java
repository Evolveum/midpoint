/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query.hqm.condition;

import com.evolveum.midpoint.repo.sql.query.hqm.HibernateQuery;
import com.evolveum.midpoint.repo.sql.query.hqm.RootHibernateQuery;

/**
 * @author mederly
 */
public class IsNullCondition extends PropertyCondition {

    public IsNullCondition(RootHibernateQuery rootHibernateQuery, String propertyPath) {
        super(rootHibernateQuery, propertyPath);
    }

    @Override
    public void dumpToHql(StringBuilder sb, int indent) {
        HibernateQuery.indent(sb, indent);
        sb.append(propertyPath).append(" is null");
    }

    // inherited "equals" is OK
}
