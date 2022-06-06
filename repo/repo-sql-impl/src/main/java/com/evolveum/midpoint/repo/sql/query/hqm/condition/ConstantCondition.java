/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query.hqm.condition;

import com.evolveum.midpoint.repo.sql.query.hqm.HibernateQuery;

public class ConstantCondition extends Condition {

    private final boolean value;

    public ConstantCondition(HibernateQuery rootHibernateQuery, boolean value) {
        super(rootHibernateQuery);
        this.value = value;
    }

    @Override
    public void dumpToHql(StringBuilder sb, int indent) {
        HibernateQuery.indent(sb, indent);
        sb.append(value ? "1=1" : "1=0");
    }
}
