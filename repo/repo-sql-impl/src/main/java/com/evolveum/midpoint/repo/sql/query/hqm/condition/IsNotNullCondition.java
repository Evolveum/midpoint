/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.query.hqm.condition;

import com.evolveum.midpoint.repo.sql.query.hqm.HibernateQuery;

public class IsNotNullCondition extends PropertyCondition {

    public IsNotNullCondition(HibernateQuery rootHibernateQuery, String propertyPath) {
        super(rootHibernateQuery, propertyPath);
    }

    @Override
    public void dumpToHql(StringBuilder sb, int indent) {
        HibernateQuery.indent(sb, indent);
        sb.append(propertyPath).append(" is not null");
    }

    // inherited "equals" is OK
}
