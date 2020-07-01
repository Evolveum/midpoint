/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query.hqm.condition;

import com.evolveum.midpoint.repo.sql.query.hqm.HibernateQuery;
import com.evolveum.midpoint.repo.sql.query.hqm.RootHibernateQuery;
import org.apache.commons.lang.Validate;

import java.util.Objects;

/**
 * Specific for SQL Server.
 * EXPERIMENTAL
 *
 * @author mederly
 */
public class ExistsCondition extends Condition {

    private String innerQueryText;
    private String linkingCondition;

    public ExistsCondition(RootHibernateQuery rootHibernateQuery, String innerQueryText, String linkingCondition) {
        super(rootHibernateQuery);
        Validate.notNull(innerQueryText);
        Validate.notNull(linkingCondition);
        this.innerQueryText = innerQueryText;
        this.linkingCondition = linkingCondition;
    }

    @Override
    public void dumpToHql(StringBuilder sb, int indent) {
        HibernateQuery.indent(sb, indent);
        sb.append(" exists (").append(innerQueryText).append(" ").append(linkingCondition).append(")");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ExistsCondition))
            return false;
        ExistsCondition that = (ExistsCondition) o;
        return Objects.equals(innerQueryText, that.innerQueryText) &&
                Objects.equals(linkingCondition, that.linkingCondition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(innerQueryText, linkingCondition);
    }
}
