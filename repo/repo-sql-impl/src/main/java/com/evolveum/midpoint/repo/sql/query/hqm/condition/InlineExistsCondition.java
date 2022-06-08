/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query.hqm.condition;

import java.util.Objects;

import com.evolveum.midpoint.repo.sql.query.hqm.HibernateQuery;

/**
 * Specific for SQL Server.
 * EXPERIMENTAL
 */
public class InlineExistsCondition extends Condition {

    private final String innerQueryText;
    private final String linkingCondition;

    public InlineExistsCondition(HibernateQuery rootHibernateQuery, String innerQueryText, String linkingCondition) {
        super(rootHibernateQuery);
        Objects.requireNonNull(innerQueryText);
        Objects.requireNonNull(linkingCondition);
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
        if (this == o) { return true; }
        if (!(o instanceof InlineExistsCondition)) { return false; }

        InlineExistsCondition that = (InlineExistsCondition) o;
        return Objects.equals(innerQueryText, that.innerQueryText) &&
                Objects.equals(linkingCondition, that.linkingCondition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(innerQueryText, linkingCondition);
    }
}
